/*
 * Copyright (c) 2022, Bart.Hanssens
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

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;

/**
 *
 * @author Bart.Hanssens
 */
public class IcalWriter {
	private void writeStart(Writer w) throws IOException {
		w.write("BEGIN:VCALENDAR\r\n");
		w.write("VERSION:2.0\r\n");
		w.write("PRODID:BE Official Holidays Calculator\r\n");
		w.write("CALSCALE:GREGORIAN\r\n");
		w.write("METHOD:PUBLISH\r\n");
		w.flush();
	}
	
	private void writeDay(Writer w, Holiday h, String lang) throws IOException {
		w.write("BEGIN:VEVENT\r\n");
		
		switch(lang) {
			case "nl" -> w.write("SUMMARY;LANGUAGE=nl-be:" + h.nl() + "\r\n");
			case "fr" -> w.write("SUMMARY;LANGUAGE=nl-fr:" + h.fr() + "\r\n");
			case "de" -> w.write("SUMMARY;LANGUAGE=nl-de:" + h.de() + "\r\n");
		}

		w.write("DTSTART;VALUE=DATE:" + h.date().format(DateTimeFormatter.BASIC_ISO_DATE) + "\r\n");
		w.write("END:VEVENT\r\n");
		w.flush();
	}

	private void writeEnd(Writer w) throws IOException {
		w.write("END:VCALENDAR\r\n");
		w.flush();
	}

	/**
	 * Write days to vcal file
	 * 
	 * @param w
	 * @param days
	 * @param language code (nl, fr or de)
	 * @throws IOException 
	 */
	public void write(Writer w, SortedSet<Holiday> days, String lang) throws IOException {
		writeStart(w);
		for (Holiday day: days) {
			writeDay(w, day, lang);
		}
		writeEnd(w);
	}
}