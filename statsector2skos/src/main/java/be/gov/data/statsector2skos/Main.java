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
package be.gov.data.statsector2skos;

import be.gov.data.statsector2skos.convert.Converter;
import be.gov.data.statsector2skos.convert.ConverterSKOS;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 * 
 * Converts Statbel statistical sectors shapefile to SKOS.
 * 
 * @see https://statbel.fgov.be/en/open-data/statistical-sectors-2023
 * 
 * @author Bart Hanssens
 */
public class Main {
	private final static Logger LOG = LoggerFactory.getLogger(Main.class);
		
	private final static Options OPTS = new Options()
		.addRequiredOption("i", "inputFile", true, "zipped shapefile")
		.addRequiredOption("b", "base", true, "base URL")
		.addRequiredOption("o", "outputFile", true, "SKOS output file (TTL, NT or JSONLD)");

	/**
	 * Print help info
	 */
	private static void printHelp() {
		HelpFormatter fmt = new HelpFormatter();
		fmt.printHelp("Convert Statistical Sectors to SKOS", OPTS);
	}
	
	/**
	 * Parse command line arguments
	 * 
	 * @param args
	 * @return 
	 */
	private static CommandLine parse(String[] args) {
		CommandLineParser cli = new DefaultParser();
		try {
			return cli.parse(OPTS, args);
		} catch (ParseException ex) {
			printHelp();
		}
		return null;
	}
	
	/**
	 *
	 * @param pin
	 * @return
	 * @throws IOException
	 */
	private static Path unzipTmpDir(Path pin) throws IOException {
		Path tmpdir = Files.createTempDirectory("stat2skos");
		tmpdir.toFile().deleteOnExit();
	
		LOG.info("Unzipping {} to {}", pin, tmpdir);

		try (FileSystem zipfs = FileSystems.newFileSystem(pin, ClassLoader.getSystemClassLoader())) {
			Path root = zipfs.getPath(Converter.SUBDIR_L08);
			if (root == null) {
				throw new IOException("No stat sector sub directory found");
			}
			for (Path zipEntry: Files.walk(root).toArray(Path[]::new)) {
				String ze = zipEntry.toString();
				if (! ze.isEmpty()) {
					Path p = Paths.get(tmpdir.toString(), ze);
					p.toFile().deleteOnExit();
					Files.copy(zipEntry, p);
				}
			}
		} 
		return tmpdir;
	}
		
	/**
	 * Main
	 * 
	 * @param args 
	 */
	public static void main(String args[]) {
		CommandLine cli  = parse(args);
		if (cli == null) {
			System.exit(-1);
		}

		String infile = cli.getOptionValue("i");
		String base = cli.getOptionValue("b");
		String outfile = cli.getOptionValue("o");

		Path pin = Paths.get(infile);
		if (! (Files.exists(pin) && Files.isReadable(pin))) {
			LOG.error("Cannot read input {}", pin);
			System.exit(-2);
		}
		if (Files.isRegularFile(pin) &&  pin.toString().endsWith("zip")) {
			try {
				pin = unzipTmpDir(pin);
			} catch (IOException ioe) {
				LOG.error("Cannot unzip", ioe);
				System.exit(-3);
			}
		}
		
		try {
			Converter conv = new ConverterSKOS();
			Path pout = Paths.get(outfile);
			LOG.info("Converting shapefile {} to {}", pin, pout);
			conv.convert(pin, base, pout);
			LOG.info("Done");
		} catch (IOException ioe) {
			LOG.error("Could not convert data", ioe);
			System.exit(-4);
		}
	}
}

