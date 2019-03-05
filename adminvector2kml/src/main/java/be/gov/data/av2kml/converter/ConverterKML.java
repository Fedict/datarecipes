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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter to KML
 * 
 * @author Bart Hanssens
 */
public class ConverterKML implements Converter {
	private final static Logger LOG = LoggerFactory.getLogger(ConverterKML.class);

	@Override
	public void convert(Path indir, Path outdir) throws IOException {
		Map params = new HashMap<>();
		File file = new File(indir.toFile(), Converter.AD_1 + ".shp");
        params.put("url", file.toURI().toURL());
		
		LOG.info("Using shape {}", file);
        DataStore store = DataStoreFinder.getDataStore(params);

		SimpleFeatureSource src = store.getFeatureSource(Converter.AD_1);
		try (FeatureIterator features = src.getFeatures().features()) {
			while (features.hasNext()) {
				Feature feature = features.next();
				GeometryAttribute geom = feature.getDefaultGeometryProperty();
				Collection<Property> properties = feature.getProperties();

				properties.forEach(p -> System.err.println(p.getName() + ":" + p.getValue()));
			}
		}
	}
}
