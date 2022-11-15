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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Generate a list of the official holidays in Belgium.
 * 
 * @author Bart Hanssens
 */
public class Calculator {
	
	/**
	 * Calculate the date of Easter, using the algorithm published in New Scientist in 1961 
	 * See also https://en.wikipedia.org/wiki/Date_of_Easter
	 * 
	 * @param year
	 * @return 
	 */
	public static LocalDate calculateEaster(int year) {
		int a = year % 19;
		int b = year / 100;
		int c = year % 100;
		int d = b / 4;
		int e = b % 4;
		int g = (8 * b + 13) / 25;
		int h = (19 * a + b - d - g + 15) % 30;
		int i = c / 4;
		int k = c % 4;
		int l = (32 + 2 * e + 2 * i - h - k) % 7;
		int m = (a + 11 * h + 19 * l) / 433;
		int n = (h + l - 7 * m + 90) / 25;
		int p = (h + l - 7 * m + 33 * n + 19) % 32;

		return LocalDate.of(year, n, p);
	}

	/**
	 * Holidays based on the data of Easter
	 * 
	 * @param year
	 * @return set of holidays
	 */
	public static Set<Holiday> easterBasedDates(int year) {
		LocalDate easter = calculateEaster(year);
		return Set.of(
			new Holiday(easter, "Pasen", "Pâques", ""),
			new Holiday(easter.plusDays(1), "Paasmaandag", "lundi de Pâques", ""),
			new Holiday(easter.plusDays(40), "OLH Hemelvaart", "Ascension", ""),
			new Holiday(easter.plusDays(50), "Pinksteren", "Ascension", ""),			
			new Holiday(easter.plusDays(51), "Pinkstermaandag", "Ascension", ""));
	}

	/**
	 * Holidays with a fixed date in Gregorian calendar
	 * 
	 * @param year year
	 * @return set of holidays
	 */
	public static Set<Holiday> fixedDates(int year) {
		return Set.of(
			new Holiday(LocalDate.of(year, 1, 1), "Nieuwjaar", "jour de l'An", "Neujahr"),
			new Holiday(LocalDate.of(year, 5, 1), "Feest van de Arbeid", "fête du Travail", ""),
			new Holiday(LocalDate.of(year, 7, 21), "National feestdag", "fête nationale", ""),
			new Holiday(LocalDate.of(year, 8, 15), "OLV Hemelvaart", "Assomption", ""),
			new Holiday(LocalDate.of(year, 11, 1), "Allerheiligen", "Toussaint", ""),
			new Holiday(LocalDate.of(year, 11, 11), "Wapenstilstand", "Armistice", ""),
			new Holiday(LocalDate.of(year, 12, 25), "Kerstmis", "Noël", "")		
		);
	}

	/**
	 * Get an ordered set of all holidays
	 * 
	 * @param year year
	 * @return 
	 */
	public static SortedSet<Holiday> allDatesOrdered(int year) {
		SortedSet<Holiday> dates = new TreeSet<>(Comparator.comparing(Holiday::date));
		dates.addAll(fixedDates(year));
		dates.addAll(easterBasedDates(year));
		
		return dates;
	}
}
