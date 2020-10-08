/*
 * Copyright (c) 2020, FPS BOSA DG DT
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
package be.gov.data.adminvector2csv;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter to CSV
 * 
 * @author Bart Hanssens
 */
public class ConverterCSV implements Converter {
	private final static Logger LOG = LoggerFactory.getLogger(ConverterCSV.class);
	
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
	 * Get an attribute / field from a "feature"
	 * 
	 * @param feature
	 * @param property name of the property
	 * @return string value or empty
	 */
	private String getProperty(SimpleFeature feature, String property) {
		Optional<Property> prop = feature.getProperties(property).stream().findFirst();
		return prop.isPresent() ? (String) prop.get().getValue() : "";
	}
	
	@Override
	public void convert(Path indir, Path outfile) throws IOException {
		LOG.info("Opening {}", outfile);

		try (PrintWriter w = new PrintWriter(
								Files.newBufferedWriter(outfile, StandardCharsets.UTF_8,
															StandardOpenOption.TRUNCATE_EXISTING, 
															StandardOpenOption.CREATE))) {

			SimpleFeatureCollection collection = getFeatures(indir.toFile(), Converter.AD_2_CENTER);
		
			try (SimpleFeatureIterator features = collection.features()) {
				LOG.info("Writing to {}", outfile);
							
				String headers = Stream.of(new String[] { "NIS", "Name NL", "Name FR", "Name DE" })
										.collect(Collectors.joining(","));
				w.println(headers);

				while (features.hasNext()) {
					SimpleFeature feature = features.next();

					// Get the NIS code, which should alway be present
					String nis = getProperty(feature, Converter.NIS);

					// Get the names in 1 or more languages
					String nl = getProperty(feature, Converter.NL);
					String fr = getProperty(feature, Converter.FR);
					String de = getProperty(feature, Converter.DE);

					Object geom = feature.getDefaultGeometry();
					if (geom instanceof Point) {
						Coordinate[] coords = ((Point) geom).getCoordinates();
						if (coords.length == 0) {
							String x = String.valueOf(coords[0].x);
							String y = String.valueOf(coords[0].y);
	
							String row = Stream.of(new String[] { nis, nl, fr, de, x, y })
											.collect(Collectors.joining(","));
							w.println(row);
						} else {
							LOG.error("Empty coordinates found for {}", nis);
						}
					} else {
						LOG.error("No coordinates found for {}", nis);
					}
				}
			}
		}
	}
}
