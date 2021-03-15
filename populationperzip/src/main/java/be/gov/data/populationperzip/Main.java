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
import java.nio.file.Path;
import java.util.HashMap;
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
         description = "Calculates the number of people per zip code.")
public class Main implements Callable<Integer> {
	private final static Logger LOG = Logger.getLogger(Main.class.getName());

	@Option(names = {"-s", "--sectors"}, description = "Statistical sectors file")
    private Path sectorFile;

	@Option(names = {"-p", "--population"}, required = true, arity = "1", description = "Population file")
    private Path populationFile;

	@Option(names = {"-z", "--zipcode"}, required = true, arity = "1", description = "ZIPcode (bPost) file")
    private Path zipcodeFile;

	@Option(names = {"-o", "--output"}, required = true, arity = "1", description = "Output file")
    private Path outFile;

	
	private boolean checkFiles() {
		if (!sectorFile.toFile().exists()) {
			LOG.log(Level.SEVERE, "File {0} does not exist", sectorFile);
			return false;
		}
		if (!populationFile.toFile().exists()) {
			LOG.log(Level.SEVERE, "File {0} does not exist", populationFile);
			return false;
		}
		if (!zipcodeFile.toFile().exists()) {
			LOG.log(Level.SEVERE, "File {0} does not exist", zipcodeFile);
			return false;
		}
		return true;
	}

	private Map.Entry<String,String> findZipCode(Map<String, MultiPolygon> zipcodes, Map.Entry<String, Point> center) {
		Optional<Map.Entry<String, MultiPolygon>> zip = zipcodes.entrySet().stream()
				.filter(e -> center.getValue().within(e.getValue()))
				.findFirst();
		if (!zip.isPresent()) {
			LOG.log(Level.WARNING, "No zipcode for sector {0} {1}", 
									new Object[] { center.getKey(), center.getValue().toText() });
			//zipcodes.entrySet().forEach(e -> System.err.println(e.getValue().toText()));
		}
		return new HashMap.SimpleEntry<>(center.getKey(), zip.isPresent() ? zip.get().getKey() : "");
	}
	

	private Map.Entry<String,Integer> findPopulation(Map<String, Integer> population, Map.Entry<String, String> sector) {
		Optional<Map.Entry<String, Integer>> pop = population.entrySet().stream()
				.filter(e -> e.getKey().equals(sector.getKey()))
				.findFirst();
		if (!pop.isPresent()) {
			LOG.log(Level.WARNING, "No population sector for {0}", sector.getKey());
		}
		return new HashMap.SimpleEntry<>(sector.getValue(), pop.isPresent() ? pop.get().getValue() : 0);
	}

	@Override
    public Integer call() throws Exception {
		if (!checkFiles()) {
			return -1;
		}
		
		PostalReader postalReader = new PostalReader();
		Map<String, MultiPolygon> zipcodes = postalReader.read(zipcodeFile);

		PopulationReader populationReader = new PopulationReader();
		Map<String, Integer> population = populationReader.read(populationFile);
		
		SectorReader sectorReader = new SectorReader();
		Map<String, Point> sectors = sectorReader.read(sectorFile);

		Map<String, Integer> result = sectors.entrySet().stream()
										.map(e -> findZipCode(zipcodes, e))
										.map(e -> findPopulation(population, e))
										.collect(Collectors.groupingBy(e -> e.getKey(),
												Collectors.summingInt(e -> e.getValue())));
		
		result.entrySet().stream().sorted((a,b) -> a.getKey().compareTo(b.getKey()))
									.forEach(e -> System.err.println(e.getKey() + "," + e.getValue()));
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
