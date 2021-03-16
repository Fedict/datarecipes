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
package be.gov.data.populationperzip;

import be.gov.data.populationperzip.reader.PopulationReader;
import be.gov.data.populationperzip.reader.PostalReader;
import be.gov.data.populationperzip.reader.SectorReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import picocli.CommandLine;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main class.
 * 
 * 
 * @author Bart Hanssens
 */
@Command(name = "population-per-zip", mixinStandardHelpOptions = true, version = "1.0",
         description = "Create a list of the number of people per zip code, based on several files.")
public class Main implements Callable<Integer> {
	private final static Logger LOG = Logger.getLogger(Main.class.getName());

	@Option(names = {"-s", "--sectors"}, description = "Statistical sectors file (GeoJSON)")
    private Path sectorFile;

	@Option(names = {"-p", "--population"}, required = true, arity = "1", description = "Population file (TXT)")
    private Path populationFile;

	@Option(names = {"-z", "--zipcode"}, required = true, arity = "1", description = "ZIPcode file (SHP)")
    private Path zipcodeFile;

	@Option(names = {"-o", "--output"}, required = true, arity = "1", description = "Output file (CSV)")
    private Path outFile;

	
	/**
	 * Check if input file exist and in the expected file format
	 * 
	 * @param file file name
	 * @param format file extension
	 * @return true when checks are passed
	 */
	private boolean checkFile(Path file, String format) {
		if (!file.toFile().exists()) {
			LOG.log(Level.SEVERE, "File {0} does not exist", file);
			return false;
		}
		if (!file.toString().endsWith(format)) {
			LOG.log(Level.SEVERE, "File {0} not in {1} format", new Object[] { file, format });
			return false;
		}
		return true;
	}

	/**
	 * Find zip code for a given statistical sector
	 * 
	 * @param zipcodes map of all zip codes
	 * @param sector center of the sector
	 * @return zipcode as a map entry
	 */
	private Map.Entry<String,String> findZipCode(Map<String, MultiPolygon> zipcodes, Map.Entry<String, Point> sector) {
		Optional<Map.Entry<String, MultiPolygon>> zip = zipcodes.entrySet().stream()
				.filter(e -> sector.getValue().within(e.getValue()))
				.findFirst();
		if (!zip.isPresent()) {
			LOG.log(Level.WARNING, "No zipcode for sector {0} {1}", 
									new Object[] { sector.getKey(), sector.getValue().toText() });
		}
		return new HashMap.SimpleEntry<>(sector.getKey(), zip.isPresent() ? zip.get().getKey() : "");
	}
	

	/**
	 * Find population for a given statistical sector
	 * 
	 * @param population population per sector
	 * @param sector statistical sector
	 * @return population as a map entry
	 */

	private Map.Entry<String,Integer> findPopulation(Map<String, Integer> population, Map.Entry<String, String> sector) {
		Optional<Map.Entry<String, Integer>> pop = population.entrySet().stream()
				.filter(e -> e.getKey().equals(sector.getKey()))
				.findFirst();
		if (!pop.isPresent()) {
			LOG.log(Level.WARNING, "No population sector for {0}", sector.getKey());
		}
		return new HashMap.SimpleEntry<>(sector.getValue(), pop.isPresent() ? pop.get().getValue() : 0);
	}


	/**
	 * Write the results to a file
	 * 
	 * @param results 
	 */
	private void writeResults(Map<String, Integer> results, Path file) {
		List<String[]> rows = results.entrySet().stream()
			.map(e -> new String[] { e.getKey(), e.getValue().toString() })
			.sorted((a,b) -> a[0].compareTo(b[0]))
			.collect(Collectors.toList());
		
		try(BufferedWriter w = Files.newBufferedWriter(file)) {
			LOG.log(Level.INFO, "Writing results to {0}", file);

			w.write("Postal;Population");
			w.newLine();

			for (String[] row: rows) {
				w.write(row[0] + ";" + row[1]);
				w.newLine();
			}
		} catch (IOException ioe) {
			LOG.log(Level.SEVERE, "Could not write results to {0}: {1}", new Object[] { file, ioe.getMessage() });
		}
	}
	
		
	@Override
    public Integer call() throws Exception {
		if (! (checkFile(sectorFile, "geojson") && checkFile(populationFile, "txt") && checkFile(zipcodeFile, "shp"))) {
			return -1;
		}
		// read all files
		PostalReader postalReader = new PostalReader();
		Map<String, MultiPolygon> zipcodes = postalReader.read(zipcodeFile);

		PopulationReader populationReader = new PopulationReader();
		Map<String, Integer> population = populationReader.read(populationFile);
		
		SectorReader sectorReader = new SectorReader();
		Map<String, Point> sectors = sectorReader.read(sectorFile);

		// map the info based on sector ID
		Map<String, Integer> result = sectors.entrySet().stream()
										.map(e -> findZipCode(zipcodes, e))
										.map(e -> findPopulation(population, e))
										.filter(e -> e.getValue() != 0) // remove uninhabitated sectors
										.collect(Collectors.groupingBy(e -> e.getKey(),
												Collectors.summingInt(e -> e.getValue())));

		writeResults(result, outFile);

        return 0;
    }

	/**
	 * Main
	 * 
	 * @param args 
	 */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
