/*
 * Copyright (c) 2021, FPS BOSA DG DT
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
package be.gov.data.populationperzip.reader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;

import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Helper class to read shapefiles
 * 
 * @author Bart Hanssens
 */
public abstract class ShapefileReader {
	private final static Logger LOG = Logger.getLogger(ShapefileReader.class.getName());

	/**
	 * Get a collection of "features", e.g. shapes
	 * 
	 * @param file shapefile
	 * @return collection of features found in shapefile
	 * @throws IOException 
	 */
	protected SimpleFeatureCollection getFeatures(File file) throws IOException {
		// parameters  for geotools
		LOG.log(Level.INFO, "Getting features from {0}", file);
		String name = file.getName();
		if (name.endsWith(".shp")) {
			name = name.substring(0, name.length() - 4);
		}

		Map params = new HashMap<>();
        params.put("url", file.toURI().toURL());
		DataStore store = DataStoreFinder.getDataStore(params);
		SimpleFeatureSource src = store.getFeatureSource(name);
		SimpleFeatureCollection coll = src.getFeatures();
		store.dispose();

		return coll;
	}

	/**
	 * Get an attribute / field from a "feature"
	 * 
	 * @param feature
	 * @param property name of the property
	 * @return string value or empty
	 */
	protected String getProperty(SimpleFeature feature, String property) {
		Optional<Property> prop = feature.getProperties(property).stream().findFirst();
		if (!prop.isPresent()) {
			return "";
		}
		String str = String.valueOf(prop.get().getValue());
		return new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
	}
}
