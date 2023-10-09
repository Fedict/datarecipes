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
package be.gov.data.statsector2skos.convert;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.MultiPolygon;


import org.opengis.feature.simple.SimpleFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert stat sectors to SKOS
 * 
 * @author Bart Hanssens
 */
public class ConverterSKOS extends Converter {
	private final static Logger LOG = LoggerFactory.getLogger(ConverterSKOS.class);

	private final static IRI PERIM = Values.iri("http://www.opengis.net/ont/geosparql#hasMetricPerimeterLength");
	private final static IRI AREA = Values.iri("http://www.opengis.net/ont/geosparql#hasMetricArea");
	private final static IRI GEO = Values.iri("http://www.opengis.net/ont/geosparql#hasGeometry");
	private final static IRI WKT = Values.iri("http://www.opengis.net/ont/geosparql#wktLiteral");

	/**
	 * Add headers
	 * 
	 * @param m
	 * @param iri 
	 */
	private static void addHeader(Model m, IRI iri) {
		m.add(iri, RDF.TYPE, SKOS.CONCEPT_SCHEME);
		m.add(iri, DCTERMS.TITLE, Values.literal("StatSector2023", "nl"));
		m.add(iri, DCTERMS.TITLE, Values.literal("StatSector2023", "fr"));
		m.add(iri, DCTERMS.TITLE, Values.literal("StatSector2023", "de"));
		m.add(iri, DCTERMS.TITLE, Values.literal("StatSector2023", "en"));
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("StatSector2023", "nl"));
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("StatSector2023", "fr"));
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("StatSector2023", "de"));
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("StatSector2023", "en"));
		m.add(iri, OWL.VERSIONINFO, Values.literal("Draft 2023-10-09"));
		m.add(iri, DCTERMS.MODIFIED, Values.literal(LocalDateTime.now()));
		m.add(iri, DCTERMS.SOURCE, Values.iri("https://statbel.fgov.be/en/open-data/statistical-sectors-2023"));
	}
		
	@Override
	public void convert(Path indir, String base, Path outfile) throws IOException {
		Model m = new LinkedHashModel();
		IRI iri = Values.iri(base);
		addHeader(m, iri);

		SimpleFeatureCollection collection = getFeatures(indir.toFile(), Converter.SHP);
		try (SimpleFeatureIterator features = collection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();

				// Get the NIS9 code
				String nis = getProperty(feature, Converter.ID);
				IRI sector = Values.iri(BASE + "/" + nis);

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

				String city = getProperty(feature, Converter.NIS5);
				IRI broader = Values.iri(REFNIS + "/" + city);

				double area = getPropertyDouble(feature, Converter.AREA);
				double perim = getPropertyDouble(feature, Converter.PERI);

				m.add(sector, RDF.TYPE, SKOS.CONCEPT);
				m.add(sector, SKOS.PREF_LABEL, Values.literal(nl, "nl"));
				m.add(sector, SKOS.PREF_LABEL, Values.literal(fr, "fr"));
				m.add(sector, SKOS.PREF_LABEL, Values.literal(de, "de"));
				m.add(sector, SKOS.NOTATION, Values.literal(nis));
				m.add(sector, SKOS.BROADER, broader);
				
				if (area > 0) {
					m.add(sector, AREA, Values.literal(area));
					m.add(sector, PERIM, Values.literal(perim));
				}

				// Get the coordinates
				Object geom = feature.getDefaultGeometry();
				if (geom instanceof MultiPolygon poly) {
					m.add(sector, GEO, Values.literal(poly.toText(), WKT));
				} else {
					LOG.error("No coordinates found for {}", nis);
				}
			}
		}
		
		try(OutputStream fos = Files.newOutputStream(outfile)) {
			Rio.write(m, fos, RDFFormat.NTRIPLES);
		}
	}
}

