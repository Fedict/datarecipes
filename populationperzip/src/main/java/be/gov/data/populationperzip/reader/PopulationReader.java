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

import com.opencsv.CSVIterator;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Reads CSV with population per statistical sector into map
 * 
 * @see https://statbel.fgov.be/en/open-data/population-statistical-sector-8
 * @author Bart Hanssens
 */
public class PopulationReader {	
	private final static Logger LOG = Logger.getLogger(PopulationReader.class.getName());

	/**
	 * Read CSV file into map
	 * 
	 * @param file
	 * @return map with NIS-code as key and population as value
	 * @throws IOException 
	 */
	public Map<String, Integer> read(Path file) throws IOException {
		HashMap<String, Integer> map = new HashMap<>();

		CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
		CSVReader reader = new CSVReaderBuilder(new FileReader(file.toFile()))
										.withSkipLines(1)
										.withCSVParser(parser).build();
		try {
			CSVIterator iter = new CSVIterator(reader);
			while(iter.hasNext()) {
				String[] row = iter.next();
				// get NIS-code and population
				map.put(row[3], Integer.valueOf(row[9]));
			}	
		} catch (CsvValidationException ex) {
			throw new IOException(ex);
		}
		return map;
	}
}
