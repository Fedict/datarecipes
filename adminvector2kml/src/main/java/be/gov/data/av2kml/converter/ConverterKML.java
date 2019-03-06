/*
 * Copyright (c) 2019, FPS BOSA DG DT
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.gov.data.av2kml.converter;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter to KML
 * 
 * @author Bart Hanssens
 */
public class ConverterKML implements Converter {
	private final static Logger LOG = LoggerFactory.getLogger(ConverterKML.class);

	private static CoordinateReferenceSystem LAM08;
	private static CoordinateReferenceSystem WGS84;
	private static MathTransform LAM08_TO_WGS84;
	
	/**
	 * Initialize coordinate system
	 * 
	 * @throws IOException 
	 */
	private void initCRS() throws IOException {
		try {
			if (LAM08 == null) {
				LAM08 = CRS.decode("EPSG:3812");
			}
			if (WGS84 == null) {
				WGS84 = CRS.decode("EPSG:4326");
			}
			LAM08_TO_WGS84 = CRS.findMathTransform(LAM08, WGS84);
		} catch (FactoryException ex) {
			throw new IOException(ex);
		}
	}

	/**
	 * Get a collection of "features", e.g. shapes of municipalities
	 * 
	 * @param indir shapefile input directory
	 * @param name feature / shape file name
	 * @return collection of features found in shapefile
	 * @throws IOException 
	 */
	private SimpleFeatureCollection getFeatures(File indir, String name) throws IOException {
		// parameters  for geotools
		Map params = new HashMap<>();
		File file = new File(indir, name + ".shp");
        params.put("url", file.toURI().toURL());
		DataStore store = DataStoreFinder.getDataStore(params);
		
		SimpleFeatureSource src = store.getFeatureSource(name);
		return src.getFeatures();
	}

	private void addCoordsSmooth(LinearRing ring, Geometry geom) {
		double prevX = 0.0;
		double prevY = 0.0;
		
		Coordinate[] coords = geom.getCoordinates();
		
		for(int p = 0; p < coords.length; p++) {
			if ((coords[p].x - prevX > 0.0005) || (coords[p].y - prevY > 0.0005)) {
			
				double smoothX = Math.round(coords[p].x * 1000) / 1000.0d;
				double smoothY = Math.round(coords[p].y * 1000) / 1000.0d;
			
				ring.addToCoordinates(smoothX, smoothY);
				
				prevX = smoothX;
				prevY = smoothY;
			}
		}
	}
	
	private void createKmlShape(Placemark place, SimpleFeature feature, Geometry geom) {			
		Object obj = feature.getDefaultGeometry();
		// by default, the shapefile uses multipolygons, 
		// which allows for very complex shapes

		if (obj instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon) geom;
			// check if the region is one "simple" shape
			int num = mp.getNumGeometries();
			
			if (num == 1) {
				Polygon poly = place.createAndSetPolygon();
				LinearRing ring = poly.createAndSetOuterBoundaryIs().createAndSetLinearRing();
				addCoordsSmooth(ring, geom);
			}
		
			if (num > 1) {
				MultiGeometry multigeom = place.createAndSetMultiGeometry();
				for (int i = 0; i < num; i++) {
					Geometry geometryN = mp.getGeometryN(i);
					Polygon poly = multigeom.createAndAddPolygon();
					LinearRing ring = poly.createAndSetOuterBoundaryIs().createAndSetLinearRing();

					addCoordsSmooth(ring, geometryN);
				}
			}
		}
	}				

			
	/**
	 * Add zipcodes from shapefile
	 * 
	 * @param indir
	 * @throws IOException 
	 */
	protected void addZipcodes(Path indir, Kml kml) throws IOException {
		SimpleFeatureCollection collection = getFeatures(indir.toFile(), Converter.AD_1);
		Document doc = kml.createAndSetDocument();
		Folder post = doc.createAndAddFolder().withName("POST");
		
		try (SimpleFeatureIterator features = collection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				Placemark place = post.createAndAddPlacemark();
				
				Collection<Property> properties = feature.getProperties(ZIP);
				if (! properties.isEmpty()) {
					Property property = properties.iterator().next();
					String zipcode = (String) property.getValue();
					place.setName(zipcode);
				}		
				
				try {
					// convert to GPS coordinates
					Geometry lambert08 = (Geometry) feature.getDefaultGeometry();			
					Geometry wgs84 = JTS.transform(lambert08, LAM08_TO_WGS84);
		
					createKmlShape(place, feature, wgs84);
				} catch (MismatchedDimensionException|TransformException ex) {
					throw new IOException(ex);
				}
			}
		}
	}

	
	@Override
	public void convert(Path indir, Path outdir) throws IOException {
		initCRS();
		
		Kml kml = KmlFactory.createKml();

		Path outfile = Paths.get(outdir.toString(), "adminvector.kml");
		LOG.info("Opening {}", outfile);

		try (Writer w = Files.newBufferedWriter(outfile, StandardCharsets.UTF_8,
														StandardOpenOption.CREATE)) {
			addZipcodes(indir, kml);
			LOG.info("Writing");
			kml.marshal(w);
		}
	}
}
