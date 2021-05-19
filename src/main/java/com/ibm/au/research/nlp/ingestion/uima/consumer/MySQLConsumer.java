/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ingestion.uima.consumer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;

import com.ibm.au.research.nlp.ingestion.MedlineIngestion;
import com.ibm.au.research.nlp.types.Relation;
import com.ibm.au.research.nlp.util.DBConnection;

public class MySQLConsumer extends JCasAnnotator_ImplBase {

	private Connection con = null;
	private PreparedStatement insertDocument = null;
	private PreparedStatement insertEntity = null;
	private PreparedStatement insertRelation = null;

	private Pattern p = Pattern.compile("/");

	private int batchCount = 0;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		try {
			con = DBConnection.getConnection();
			insertDocument = con
					.prepareStatement("insert into document (source, source_id, section, text) values (?,?,?,?)");

			insertEntity = con.prepareStatement(
					"insert into entity (source, source_id, section, id, type, begin, end, text, db_id, canonical_id) values (?,?,?,?,?,?,?,?,?,?)");

			insertRelation = con.prepareStatement(
					"insert into relation (source, source_id, section, id, type, entity_id1, entity_id2) values (?,?,?,?,?,?,?)");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void destroy() {
		try {
			insertRelation.executeBatch();
			insertEntity.executeBatch();
			insertDocument.executeBatch();

			insertRelation.close();
			insertEntity.close();
			insertDocument.close();

			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static synchronized int getCanonicalId(String canonical) {
		String canonicalString = StringUtils.abbreviate(canonical.toLowerCase().trim(), 100);

		Integer id = MedlineIngestion.mapCanonical.get(canonicalString);

		if (id == null) {

			id = MedlineIngestion.mapCanonical.size();
			MedlineIngestion.mapCanonical.put(canonicalString, id);
		}

		return id;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		try {
			String[] doc_id = p.split(ViewUriUtil.getURI(jCas).toString());

			// Add the document
			insertDocument.setString(1, doc_id[0]);
			insertDocument.setString(2, doc_id[1]);
			insertDocument.setString(3, doc_id[2]);
			insertDocument.setString(4, jCas.getDocumentText());

			insertDocument.addBatch();

			Map<NamedEntityMention, Integer> mapEntity = new HashMap<>();

			int entityId = 1;

			// Add the entities
			for (NamedEntityMention e : JCasUtil.select(jCas, NamedEntityMention.class)) {
				// document_id, type, begin, end, text, canonical

				insertEntity.setString(1, doc_id[0]);
				insertEntity.setString(2, doc_id[1]);
				insertEntity.setString(3, doc_id[2]);
				insertEntity.setInt(4, entityId);
				insertEntity.setString(5, e.getMentionType());
				insertEntity.setInt(6, e.getBegin());
				insertEntity.setInt(7, e.getEnd());
				insertEntity.setString(8, e.getCoveredText());
				insertEntity.setString(9, e.getMentionId());
				// TODO Change to canonical form
				insertEntity.setInt(10, getCanonicalId(e.getCoveredText()));

				insertEntity.addBatch();

				// Get entity id - add id to Map

				mapEntity.put(e, entityId);

				// Update id
				entityId++;
			}

			int relationId = 1;
			// Add the relations
			for (Relation r : JCasUtil.select(jCas, Relation.class)) {
				insertRelation.setString(1, doc_id[0]);
				insertRelation.setString(2, doc_id[1]);
				insertRelation.setString(3, doc_id[2]);
				insertRelation.setInt(4, relationId);
				insertRelation.setString(5, r.getRelationType());
				insertRelation.setInt(6, mapEntity.get(r.getArg1()));
				insertRelation.setInt(7, mapEntity.get(r.getArg2()));

				insertRelation.addBatch();

				// Update relation id
				relationId++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		batchCount++;

		if (batchCount == 1000) {
			// Reset batch count
			batchCount = 0;

			try {
				insertRelation.executeBatch();
				insertEntity.executeBatch();
				insertDocument.executeBatch();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(MySQLConsumer.class);
	}
}