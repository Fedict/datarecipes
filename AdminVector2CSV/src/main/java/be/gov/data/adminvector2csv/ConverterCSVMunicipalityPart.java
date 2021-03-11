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
package be.gov.data.adminvector2csv;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.Collator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;

import org.opengis.feature.simple.SimpleFeature;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert munipality section to CSV
 * 
 * @author Bart Hanssens
 */
public class ConverterCSVMunicipalityPart extends ConverterCSV {
	private final static Logger LOG = LoggerFactory.getLogger(ConverterCSVMunicipalityPart.class);
	
	@Override
	public void convert(Path indir, Path outfile) throws IOException {
		LOG.info("Opening {}", outfile);
		
		try (PrintWriter w = new PrintWriter(outfile.toFile())) {
			SimpleFeatureCollection collection = getFeatures(indir.toFile(), Converter.AD_1);
		
			try (SimpleFeatureIterator features = collection.features()) {
				LOG.info("Writing to {}", outfile);
							
				String headers = Stream.of(new String[] { "Name NL", "Name FR", "Name DE", "Pseudo NIS", "Postal" })
										.collect(Collectors.joining(";"));
				w.println(headers);

				// make the list ordered, ignore accents
				Collator collator = Collator.getInstance(Locale.FRENCH);
				Set<String> list = new TreeSet<>(collator);

				while (features.hasNext()) {
					SimpleFeature feature = features.next();

					// Get the NIS code, which should alway be present, this is NOT the postal code
					String nis = getProperty(feature, Converter.PSEUDO);

					// Get the postal code
					String zip = getProperty(feature, Converter.ZIP);
					
					// Get the names in 1 or more languages
					String nl = getProperty(feature, Converter.NL);
					String fr = getProperty(feature, Converter.FR);
					String de = getProperty(feature, Converter.DE);

					// Also add non-translated names
					if (nl.isEmpty()) {
						nl = fr;
					}
					if (fr.isEmpty()) {
						fr = nl;
					}
					if (de.isEmpty()) {
						de = fr;
					}
					
					String row = Stream.of(new String[] { nl, fr, de, nis, zip })
										.collect(Collectors.joining(";"));
					list.add(row);
				}
				list.forEach(w::println);	
			}
		}
	}
}
