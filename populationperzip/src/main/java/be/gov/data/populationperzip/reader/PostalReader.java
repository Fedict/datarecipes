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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Reads postal (zipcode) sectors
 * 
 * @see https://www.geo.be/catalog/details/9738c7c0-5255-11ea-8895-34e12d0f0423
 * @author Bart Hanssens
 */
public class PostalReader extends ShapefileReader {
	public final String ZIPCODE = "nouveau_PO";
	
	/**
	 * Read postal sectors shapefile into map
	 * 
	 * @param indir
	 * @return map with postal code as key and shape as value
	 * @throws IOException 
	 */
	public Map<String, MultiPolygon> read(Path indir) throws IOException {
		HashMap<String, MultiPolygon>  map = new HashMap<>();

		SimpleFeatureCollection collection = getFeatures(indir.toFile());
		
		try (SimpleFeatureIterator features = collection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				
				String zipcode = getProperty(feature, ZIPCODE);
				MultiPolygon geom =  (MultiPolygon) feature.getDefaultGeometry();

				map.put(zipcode, geom);
			}
		}
		return map;
	}
}