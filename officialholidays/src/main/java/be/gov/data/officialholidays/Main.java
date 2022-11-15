/*
 * Copyright (c) 2022, FPS BOSA DG DT
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
package be.gov.data.officialholidays;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.SortedSet;

/**
 * Generate a list of the official holidays in Belgium.
 * 
 * @author Bart Hanssens
 */
public class Main {

	/**
	 * Display usage: enter the year and (optionally) an output file
	 */
	private static void printUsage() {
		System.out.println("Usage: officialholidays <year> [output.ics]");
	}

	/**
	 * Main
	 * 
	 * @param args 
	 */
    public static void main(String[] args) {
		if (args.length < 1) {
			printUsage();
			System.exit(-1);
		}

		try {
			int year = Integer.parseInt(args[0]);
			SortedSet<Holiday> days = Calculator.allDatesOrdered(year);
			
			if (args.length == 3) {
				Path p = Path.of(args[1]);
				String lang = args[2];
				
				try (FileWriter w = new FileWriter(p.toFile(), StandardCharsets.UTF_8)) {
					IcalWriter ical = new IcalWriter();
					ical.write(w, days, lang);
				} catch (IOException e) {
					System.err.println(e.getMessage());
					System.exit(-3);
				}
			} else {
				printUsage();
				System.exit(-3);
			}
		} catch (NumberFormatException e) {
			printUsage();
			System.exit(-4);
		}	
    }
}
