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
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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

	/**
	 * Reduce the KML file size by simplifying the shape and number of decimal digits.
	 * 
	 * @param geom shapefile geometry
	 * @return list of KML coordinates 
	 */
	private List<de.micromata.opengis.kml.v_2_2_0.Coordinate> smoothedCoords(Geometry geom) {
		Coordinate[] coords = geom.getCoordinates();
		List<de.micromata.opengis.kml.v_2_2_0.Coordinate> smoothed = new ArrayList<>();
		
		double prevX = 0.0;
		double prevY = 0.0;
				
		for(int p = 0; p < coords.length; p++) {
			// naive algorithm, remove points that are very close to the previous one
			if (Math.abs(coords[p].x - prevX) > 0.0004 || Math.abs(coords[p].y - prevY) > 0.0004) {
				double smoothX = Math.round(coords[p].x * 10000) / 10000.0d;
				double smoothY = Math.round(coords[p].y * 10000) / 10000.0d;
				
				smoothed.add(new de.micromata.opengis.kml.v_2_2_0.Coordinate(smoothX, smoothY));
				
				prevX = smoothX;
				prevY = smoothY;
			}
		}
		return smoothed;
	}

	/**
	 * Create simple shaped KML places, which is the default.
	 * 
	 * @param kmlPlace KML place
	 * @param geom shapefile geometry
	 */
	private void createKmlShapeSimple(Placemark kmlPlace, Geometry geom) {
		// simple case, which is the default
		List smoothed = smoothedCoords(geom);
		
		if (smoothed.size() > 2) {
			kmlPlace.createAndSetPolygon()
				.createAndSetOuterBoundaryIs()
				.createAndSetLinearRing()
				.withCoordinates(smoothed);
		} else {
			LOG.warn("Simple polygon is too small, ignoring");
		}
	}

	/**
	 * Create complex shaped KML places, like Ixelles or Baarle-Hertog.
	 * 
	 * @see https://en.wikipedia.org/wiki/Baarle-Hertog#/media/File:Baarle-Nassau_-_Baarle-Hertog-en.svg
	 * 
	 * @param kmlPlace KML place
	 * @param geom shapefile geometry
	 */
	private void createKmlShapeComplex(Placemark kmlPlace, MultiPolygon geom) {
		// complex shaped municipalities, like Ixelles
		MultiGeometry kmlMulti = kmlPlace.createAndSetMultiGeometry();
		int numPoly = geom.getNumGeometries();

		for (int i = 0; i < numPoly; i++) {
			Geometry geometryN = geom.getGeometryN(i);
					
			if (geometryN instanceof Polygon) {
				Polygon poly = (Polygon) geometryN;					
				LineString extRing = poly.getExteriorRing();
				List smoothed = smoothedCoords(extRing);

				if (smoothed.size() > 2) {
					de.micromata.opengis.kml.v_2_2_0.Polygon kmlPolygon = kmlMulti.createAndAddPolygon();
				
					kmlPolygon.createAndSetOuterBoundaryIs()
								.createAndSetLinearRing()
								.withCoordinates(smoothed);

					// Baarle-Hertog is _really_ complex
					int numInt = poly.getNumInteriorRing();
					for (int j = 0; j < numInt; j++) {
						LineString inRing = poly.getInteriorRingN(j);

						smoothed = smoothedCoords(inRing);
						if (smoothed.size() > 2) {
							kmlPolygon.createAndAddInnerBoundaryIs()
										.createAndSetLinearRing()
										.withCoordinates(smoothed);
						} else {
							LOG.warn("Interior polygon ring is too small, ignoring");
						}
					}
				} else {
					LOG.warn("Exterior polygon ring is too small, ignoring");
				}
			} else {
				LOG.warn("Expected polygon in shapefile");
			}
		}
	}

	/**
	 * Add geo coordinates and labels to KML place.
	 * The AdminVector shapefile uses multipolygons even for simple shapes,
	 * and of course also for very complex shapes.
	 * 
	 * @param place KML placemark
	 * @param feature shapefile feature
	 * @param geom shapefile geometry
	 */
	private void createKmlShape(Placemark place, SimpleFeature feature, Geometry geom) {			
		Object obj = feature.getDefaultGeometry();
		
		if (obj instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon) geom;
			// check if the region is one "simple" shape
			int numPoly = mp.getNumGeometries();
			
			if (numPoly == 1) {
				createKmlShapeSimple(place, mp);	
			} else if (numPoly > 1) {
				createKmlShapeComplex(place, mp);
			}
		} else {
			LOG.warn("Expected multipolygon in shapefile");
		}
	}				

	/**
	 * Join the names of the location into one string
	 * 
	 * @param feature
	 * @return 
	 */
	private String joinLocationNames(SimpleFeature feature) {
		StringBuilder builder = new StringBuilder();
		
		for(String lang: new String[] { Converter.NL, Converter.FR, Converter.DE }) {
			Optional<Property> prop = feature.getProperties(lang).stream().findFirst();
			if (prop.isPresent()) {
				String str = (String) prop.get().getValue();
				if (! str.isEmpty()) {
					if (builder.length() > 0) {
						builder.append(" / ");
					}
					builder.append(str);
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Add zipcodes from shapefile
	 * 
	 * @param indir
	 * @param kmldoc KML document
	 * @throws IOException 
	 */
	protected void addZipcodes(Path indir, Document kmlDoc) throws IOException {
		SimpleFeatureCollection collection = getFeatures(indir.toFile(), Converter.AD_1);
		
		Folder post = kmlDoc.createAndAddFolder().withName("POST");
		
		try (SimpleFeatureIterator features = collection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				Placemark place = post.createAndAddPlacemark();
				place.withStyleUrl("#style");
				
				// Get the ZIP code, if any
				Optional<Property> propZip = feature.getProperties(Converter.ZIP).stream().findFirst();
				if (propZip.isPresent()) {
					String zipcode = (String) propZip.get().getValue();
					String name = joinLocationNames(feature);

					place.setName(zipcode + " " + name);
				}
					
				try {
					// convert to GPS coordinates
					Geometry lambert08 = (Geometry) feature.getDefaultGeometry();			
					Geometry wgs84 = JTS.toGeographic(lambert08, LAM08);

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
		
		// start KML
		Kml kml = KmlFactory.createKml();
		Document doc = kml.createAndSetDocument();

		// Create simple style info
		Style style = doc.createAndAddStyle().withId("style");
		style.createAndSetLineStyle().withColor("ff0000ff");
		style.createAndSetPolyStyle().withFill(Boolean.FALSE);

		Path outfile = Paths.get(outdir.toString(), "adminvector.kml");
		LOG.info("Opening {}", outfile);

		try (Writer w = Files.newBufferedWriter(outfile, StandardCharsets.UTF_8,
														StandardOpenOption.TRUNCATE_EXISTING, 
														StandardOpenOption.CREATE)) {
			addZipcodes(indir, doc);
			LOG.info("Writing");
			kml.marshal(w);
		}
	}
}
