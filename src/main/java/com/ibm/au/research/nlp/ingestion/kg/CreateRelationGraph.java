/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ingestion.kg;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.ibm.au.research.nlp.brat.BratGroundTruthReader;
import com.ibm.au.research.nlp.er.EntityNormalizer;
import com.ibm.au.research.nlp.util.DBConnection;
import com.ibm.au.research.nlp.util.TextFileFilter;

/**
 * 
 * Generate graph of highly probably correlated terms
 * 
 * @author Antonio Jimeno Yepes
 *
 */
public class CreateRelationGraph {
	private static String getEntityId(NamedEntityMention e) {
		return (e.getMentionedEntity() != null ? e.getMentionedEntity().getEntityId() : null);

	}

	public static void main(String[] argc) throws ClassNotFoundException, SQLException, NamingException,
			ConfigurationException, IOException, UIMAException {
		Map<String, Integer> count = new HashMap<>();

		// Run annotator
		// Perform annotation
		AggregateBuilder annotation = new AggregateBuilder();
		annotation.add(UriToDocumentTextAnnotator.getDescription());
		annotation.add(BratGroundTruthReader.getDescription());
		annotation.add(EntityNormalizer.getDescription());

		// annotation.add(JCas2Text.getOutputFolderDescription(annotationTestAnnotationFolderName));
		// annotation.add(JCas2Brat.getOutputFolderDescription(annotationTestAnnotationFolderName));
		CollectionReaderDescription reader = UriCollectionReader.getDescriptionFromFiles(
				// FileUtils.listFiles(new
				// File("/Users/ajimeno/Documents/git/ai4eye-manual-annotation/indiv/antonio/00"),
				// FileUtils.listFiles(new
				// File("/Users/ajimeno/Documents/work/ai4eye/documents/eye-diseases"),
				FileUtils.listFiles(new File("/Users/ajimeno/Documents/work/ai4eye/documents/neurology"),
						new TextFileFilter(), null));

		AnalysisEngineDescription ae = annotation.createAggregateDescription();

		Map<Relation, Integer> relCount = new HashMap<Relation, Integer>();

		// Annotate
		for (JCas jcas : SimplePipeline.iteratePipeline(reader, ae)) {
			// Counting entity frequency
			for (NamedEntityMention e : JCasUtil.select(jcas, NamedEntityMention.class)) {
				String id = getEntityId(e);

				if (id != null) {
					count.merge(id, 1, Integer::sum);
				}
			}

			// Relating entities
			for (com.ibm.au.research.nlp.types.Relation r : JCasUtil.select(jcas,
					com.ibm.au.research.nlp.types.Relation.class)) {

				String id1 = getEntityId(r.getArg1());
				id1 = (id1 != null ? id1 : r.getArg1().getCoveredText());
				String id2 = getEntityId(r.getArg2());
				id2 = (id2 != null ? id2 : r.getArg2().getCoveredText());

				Relation rel = new Relation(r.getRelationType(), r.getArg1().getMentionType(), id1,
						r.getArg2().getMentionType(), id2);

				relCount.merge(rel, 1, Integer::sum);
			}
		}

		// Collect annotation data
		for (Map.Entry<String, Integer> entry : count.entrySet()) {
			System.out.println(entry.getKey() + "|" + entry.getValue());
		}

		try (Connection con = DBConnection.getConnection();
				PreparedStatement terms = con
						.prepareStatement("select str from MRCONSO where cui = ? and ispref='Y';")) {
			for (Map.Entry<Relation, Integer> entry : relCount.entrySet()) {
				if (entry.getValue() >= 1) {
					if (!entry.getKey().toString().startsWith("Alias")) {
						String[] tokens = entry.getKey().toString().split("\\|");

						String term1 = "";
						String term2 = "";

						terms.setString(1, tokens[4]);
						try (ResultSet rs = terms.executeQuery()) {
							if (rs.next()) {
								term1 = rs.getString(1);
							} else {
								term1 = tokens[4] + "-NN";
							}
						}

						terms.setString(1, tokens[6]);
						try (ResultSet rs = terms.executeQuery()) {
							if (rs.next()) {
								term2 = rs.getString(1);
							} else {
								term2 = tokens[6] + "-NN";
							}
						}

						System.out.println(term1.replaceAll(" ", "-").toLowerCase() + "\t" + tokens[0] + "\t"
								+ term2.replaceAll(" ", "-").toLowerCase());

						// System.out.println(entry.getKey() + "|" + entry.getValue() + "|" + term1 +
						// "|" + term2);
					}
				}
			}
		}

		// Generate graph

		// Find a paper that describes this approach and sounds right

		// Main concept of interest, focused on a disease or group of diseases

		// Recover concepts from different types
		/*
		 * try (Connection con = DBConnection.getConnection(); PreparedStatement terms =
		 * con.prepareStatement( // "select count(e2.db_id), e2.db_id from entity e1,
		 * entity e2 where e1.db_id = // ? and e1.source = e2.source and e1.source_id =
		 * e2.source_id and e2.type = ? // group by e2.db_id;");
		 * "select count(*), e2.db_id from entity e2 where exists (select 1 from entity e1 where e1.db_id = ? and e1.source = e2.source and e1.source_id = e2.source_id) and type = ? group by e2.db_id"
		 * ); PreparedStatement count =
		 * con.prepareStatement("select count(*) from entity e where db_id = ?");
		 * PreparedStatement text =
		 * con.prepareStatement("select text from entity e where db_id = ? limit 1");
		 * BufferedWriter w = new BufferedWriter( new
		 * FileWriter("/Users/ajimeno/Documents/work/ai4eye/test/out.txt"))) {
		 * 
		 * for (String[] disease : diseases) { for (String type : types) {
		 * terms.setString(1, disease[0]); // Disease id terms.setString(2, type); //
		 * Type search
		 * 
		 * // Generate graph try (ResultSet rs = terms.executeQuery()) { while
		 * (rs.next()) { count.setString(1, rs.getString(2)); try (ResultSet crs =
		 * count.executeQuery()) { while (crs.next()) { text.setString(1,
		 * rs.getString(2)); try (ResultSet trs = text.executeQuery()) { while
		 * (trs.next()) { double log_prob = (Math.log(rs.getInt(1)) -
		 * Math.log(crs.getInt(1)));
		 * 
		 * // if (log_prob > -0.5) { w.write(rs.getInt(1) + "|" + rs.getString(2) + "|"
		 * + crs.getInt(1) + "|" + log_prob + "|" + trs.getString(1) + "|" + type + "|"
		 * + disease[0] + "|" + disease[1]); w.newLine(); } } } } } } } } } }
		 */
	}
}