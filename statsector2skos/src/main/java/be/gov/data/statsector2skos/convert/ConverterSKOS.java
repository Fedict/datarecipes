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
import java.util.List;
import java.util.Optional;
import org.eclipse.rdf4j.model.BNode;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert stat sectors to SKOS
 * 
 * @author Bart Hanssens
 */
public class ConverterSKOS extends Converter {
	private final static Logger LOG = LoggerFactory.getLogger(ConverterSKOS.class);

	private final static IRI LEVEL = Values.iri("http://rdf-vocabulary.ddialliance.org/xkos#ClassificationLevel");
	private final static IRI LEVELS = Values.iri("http://rdf-vocabulary.ddialliance.org/xkos#levels");
	private final static IRI DEPTH = Values.iri("http://rdf-vocabulary.ddialliance.org/xkos#depth");
	private final static IRI MPL = Values.iri("http://www.opengis.net/ont/geosparql#hasMetricPerimeterLength");
	private final static IRI MA = Values.iri("http://www.opengis.net/ont/geosparql#hasMetricArea");
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
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("Statistische sectoren van België op 01/01/2023, geldig tot de volgende wijziging/verbetering van de gemeentegrenzen.", "nl"));
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("Secteurs statistiques de Belgique au 01/01/2023, valable jusqu’à la prochaine modification/correction des limites communales.", "fr"));
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("Statistische Sektoren Belgiens am 01/01/2023, gültig bis zur nächsten Aktualisierung/Korrektur der Gemeindegrenzen.", "de"));
		m.add(iri, DCTERMS.DESCRIPTION, Values.literal("Statistical sectors of Belgium on 01/01/2023, valid until the next update/correction of the municipal boundaries.", "en"));
		m.add(iri, OWL.VERSIONINFO, Values.literal("Draft 2023-10-09"));
		m.add(iri, DCTERMS.MODIFIED, Values.literal(LocalDateTime.now()));
		m.add(iri, DCTERMS.LICENSE, Values.iri("https://statbel.fgov.be/sites/default/files/files/opendata/Licence%20open%20data_EN.pdf"));
		m.add(iri, DCTERMS.SOURCE, Values.iri("https://statbel.fgov.be/en/open-data/statistical-sectors-2023"));
	}
		
	@Override
	public void convert(Path indir, String base, Path outfile) throws IOException {
		Model m = new LinkedHashModel();
		m.setNamespace(SKOS.PREFIX, SKOS.NAMESPACE);
		m.setNamespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE);
		m.setNamespace(XSD.PREFIX, XSD.NAMESPACE);
		m.setNamespace("geo", "http://www.opengis.net/ont/geosparql#");
		m.setNamespace("xkos", "http://rdf-vocabulary.ddialliance.org/xkos#");

		IRI iri = Values.iri(base);
		addHeader(m, iri);

		CoordinateReferenceSystem L08;
		CoordinateReferenceSystem WGS;
		MathTransform transform;
		PrecisionModel pm = new PrecisionModel(100_000);

		try {
			L08 = CRS.decode("EPSG:3812");
			WGS = CRS.decode("EPSG:4326");
			transform = CRS.findMathTransform(L08, WGS);
		} catch (FactoryException ex) {
			throw new IOException(ex);
		}

		// XKOS levels
		IRI coll9 = Values.iri(BASE + "/level/nis9");
		m.add(coll9, RDF.TYPE, LEVEL);
		m.add(coll9, RDF.TYPE, SKOS.COLLECTION);
		m.add(coll9, DEPTH, Values.literal("9", XSD.POSITIVE_INTEGER));
		
		IRI coll6 = Values.iri(BASE + "/level/nis6");
		m.add(coll6, RDF.TYPE, LEVEL);
		m.add(coll6, RDF.TYPE, SKOS.COLLECTION);
		m.add(coll6, DEPTH, Values.literal("6", XSD.POSITIVE_INTEGER));
		
		BNode bn = Values.bnode();
		m.add(iri, LEVELS, bn);
		m.addAll(RDFCollections.asRDF(List.of(coll6, coll9), bn, new LinkedHashModel()));

		SimpleFeatureCollection collection = getFeatures(indir.toFile(), Converter.SHP);
		try (SimpleFeatureIterator features = collection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();

				// Get the NIS9 code
				String nis9 = getProperty(feature, Converter.ID);
				IRI sector = Values.iri(BASE + "/" + nis9);

				// Get the NIS9 names in 1 or more languages
				String nis9_nl = getProperty(feature, Converter.NL);
				String nis9_fr = getProperty(feature, Converter.FR);
				String nis9_de = getProperty(feature, Converter.DE);

				// Also add non-translated names
				if (nis9_nl.isEmpty()) {
					nis9_nl = nis9_fr;
				}
				if (nis9_fr.isEmpty()) {
					nis9_fr = nis9_nl;
				}
				if (nis9_de.isEmpty()) {
					nis9_de = nis9_fr;
				}

				// Get the NIS6 code
				String nis6 = getProperty(feature, Converter.NIS6);
				IRI sub = Values.iri(BASE + "/" + nis6);

				// Get the NIS6 names in 1 or more languages
				String nis6_nl = getProperty(feature, Converter.NIS6_NL);
				String nis6_fr = getProperty(feature, Converter.NIS6_FR);

				// Also add non-translated names
				if (nis6_nl.isEmpty()) {
					nis6_nl = nis6_fr;
				}
				if (nis6_fr.isEmpty()) {
					nis6_fr = nis6_nl;
				}
	
				String nis5 = getProperty(feature, Converter.NIS5);
				IRI city = Values.iri(REFNIS + "/" + nis5);

				double area = getPropertyDouble(feature, Converter.AREA);
				double perim = getPropertyDouble(feature, Converter.PERIM);

				m.add(sector, RDF.TYPE, SKOS.CONCEPT);
				m.add(sector, SKOS.PREF_LABEL, Values.literal(nis9_nl, "nl"));
				m.add(sector, SKOS.PREF_LABEL, Values.literal(nis9_fr, "fr"));
				m.add(sector, SKOS.PREF_LABEL, Values.literal(nis9_de, "de"));
				m.add(sector, SKOS.NOTATION, Values.literal(nis9));
				m.add(sector, SKOS.IN_SCHEME, iri);
				m.add(sector, SKOS.BROADER, sub);
				m.add(sub, SKOS.NARROWER, sector);

				m.add(sub, RDF.TYPE, SKOS.CONCEPT);
				m.add(sub, SKOS.PREF_LABEL, Values.literal(nis6_nl, "nl"));
				m.add(sub, SKOS.PREF_LABEL, Values.literal(nis6_fr, "fr"));
				m.add(sub, SKOS.NOTATION, Values.literal(nis6));
				m.add(sub, SKOS.BROADER, city);
				m.add(sub, SKOS.TOP_CONCEPT_OF, iri);
				m.add(sub, SKOS.IN_SCHEME, iri);
				m.add(iri, SKOS.HAS_TOP_CONCEPT, sub);

				m.add(coll9, SKOS.MEMBER, sector);
				m.add(coll6, SKOS.MEMBER, sub);

				//m.add(sector, DEPTH, Values.literal("9"));
				
				if (area > 0) {
					m.add(sector, MA, Values.literal(area));
					m.add(sector, MPL, Values.literal(perim));
				}

				// Get the coordinates of the polygon
				Object geom = feature.getDefaultGeometry();
				if (geom instanceof MultiPolygon poly) {
					try {
						// Convert Lambert 2008 to ETRS89/WGS84
						Geometry wgs = JTS.transform(poly, transform);
						String shape = GeometryPrecisionReducer.reduce(wgs, pm).toText();
						m.add(sector, GEO, Values.literal(shape, WKT));
					} catch (MismatchedDimensionException|TransformException ex) {
						LOG.error("Could not convert coordinates for {}", nis9);
					}
				} else {
					LOG.error("No coordinates found for {}", nis9);
				}
			}
		}

		Optional<RDFFormat> fmt = RDFFormat.matchFileName(outfile.getFileName().toString(), 
										List.of(RDFFormat.JSONLD, RDFFormat.NTRIPLES, RDFFormat.TURTLE));

		if (fmt.isPresent()) {
			try(OutputStream fos = Files.newOutputStream(outfile)) {
				Rio.write(m, fos, fmt.get());
			}
		} else {
			LOG.error("Unknown format for {}", outfile);			
		}
	}
}

