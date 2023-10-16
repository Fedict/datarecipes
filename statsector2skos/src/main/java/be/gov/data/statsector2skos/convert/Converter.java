/*
 * Copyright (c) 2023, FPS BOSA DG SD
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
package be.gov.data.statsector2skos.convert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter interface
 * 
 * @author Bart Hanssens
 */
public abstract class Converter {
	private final static Logger LOG = LoggerFactory.getLogger(Converter.class);

	// subdirectory in zip file containing Lambert 2008
	public static final String SUBDIR_L08 = "/sh_statbel_statistical_sectors_3812_20230101.shp";

	// shape files
	public static final String SHP = "sh_statbel_statistical_sectors_3812_20230101";

	public static final String BASE = "http://vocab.belgif.be/auth/statsect2023";
	public static final String REFNIS = "http://vocab.belgif.be/auth/refnis2019";
	
	// properties inside the shape files
	public static final String ID = "CS01012023";
	public static final String NL = "T_SEC_NL";
	public static final String FR = "T_SEC_FR";
	public static final String DE = "T_SEC_DE";
	
	public static final String NIS5 = "CNIS5_2023";

	public static final String NIS6 = "C_NIS6";
	public static final String NIS6_NL = "T_NIS6_NL";
	public static final String NIS6_FR = "T_NIS6_FR";

	public static final String AREA = "Shape_Area";
	public static final String PERIM = "Shape_Leng";


	/**
	 * Convert from directory with shapefiles to another file format
	 * 
	 * @param infile shapefile input directory
	 * @param base base URI
	 * @param outfile output directory
	 * @throws IOException
	 */
	public abstract void convert(Path infile, String base, Path outfile) throws IOException;
	
	/**
	 * Get a collection of "features", e.g. shapes of municipalities
	 * 
	 * @param indir shapefile input directory
	 * @param name feature / shape file name
	 * @return collection of features found in shapefile
	 * @throws IOException 
	 */
	protected SimpleFeatureCollection getFeatures(File indir, String name) throws IOException {
		// parameters  for geotools
		File file = new File(indir, name + ".shp");
		LOG.info("Getting features from {}", file);

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
		String str = (String) prop.get().getValue();
		return new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
	}

	/**
	 * Get an attribute / field from a "feature"
	 * 
	 * @param feature
	 * @param property name of the property
	 * @return double value or empty
	 */
	protected double getPropertyDouble(SimpleFeature feature, String property) {
		Optional<Property> prop = feature.getProperties(property).stream().findFirst();
		if (!prop.isPresent()) {
			return 0;
		}
		return (double) prop.get().getValue();
	}
}
