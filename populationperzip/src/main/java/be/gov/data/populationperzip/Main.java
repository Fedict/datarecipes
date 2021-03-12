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
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

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


	@Override
    public Integer call() throws Exception {
		if (!sectorFile.toFile().exists()) {
			LOG.log(Level.SEVERE, "File {0} does not exist", sectorFile);
			return -1;
		}
		if (!populationFile.toFile().exists()) {
			LOG.log(Level.SEVERE, "File {0} does not exist", populationFile);
			return -1;
		}
		if (!zipcodeFile.toFile().exists()) {
			LOG.log(Level.SEVERE, "File {0} does not exist", zipcodeFile);
			return -1;
		}

		PostalReader postalReader = new PostalReader();
		postalReader.read(zipcodeFile);

		PopulationReader populationReader = new PopulationReader();
		populationReader.read(populationFile);
		
		SectorReader sectorReader = new SectorReader();
		sectorReader.read(sectorFile);

				
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
