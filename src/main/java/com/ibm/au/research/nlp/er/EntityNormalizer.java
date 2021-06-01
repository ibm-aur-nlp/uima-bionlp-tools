/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.er;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.naming.NamingException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.ne.type.NamedEntity;
import org.cleartk.ne.type.NamedEntityMention;

import com.ibm.au.research.nlp.types.Relation;
import com.ibm.au.research.nlp.util.DBConnection;

public class EntityNormalizer extends JCasAnnotator_ImplBase {
	static Logger logger = UIMAFramework.getLogger(EntityNormalizer.class);

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(EntityNormalizer.class);
	}

	// DISO|Disorders|T020|Acquired Abnormality
	// DISO|Disorders|T190|Anatomical Abnormality
	// DISO|Disorders|T049|Cell or Molecular Dysfunction
	// DISO|Disorders|T019|Congenital Abnormality
	// DISO|Disorders|T047|Disease or Syndrome
	// DISO|Disorders|T050|Experimental Model of Disease
	// DISO|Disorders|T037|Injury or Poisoning
	// DISO|Disorders|T048|Mental or Behavioral Dysfunction
	// DISO|Disorders|T191|Neoplastic Process
	// DISO|Disorders|T046|Pathologic Function
	// DISO|Disorders|T033|Finding
	// DISO|Disorders|T184|Sign or Symptom
	protected static String semanticTypesDisease = "'T020','T190','T049','T019','T047','T050','T037','T048', 'T191', 'T046', 'T033', 'T184'";

	// ANAT|Anatomy|T017|Anatomical Structure
	// ANAT|Anatomy|T029|Body Location or Region
	// ANAT|Anatomy|T023|Body Part, Organ, or Organ Component
	// ANAT|Anatomy|T030|Body Space or Junction
	// ANAT|Anatomy|T031|Body Substance
	// ANAT|Anatomy|T022|Body System
	// ANAT|Anatomy|T025|Cell
	// ANAT|Anatomy|T026|Cell Component
	// ANAT|Anatomy|T018|Embryonic Structure
	// ANAT|Anatomy|T021|Fully Formed Anatomical Structure
	// ANAT|Anatomy|T024|Tissue
	protected static String semanticTypesAnatomy = "'T017','T029','T023','T030','T031','T022','T025','T026','T018','T021','T024'";

	// PROC|Procedures|T060|Diagnostic Procedure
	// PROC|Procedures|T065|Educational Activity
	// PROC|Procedures|T058|Health Care Activity
	// PROC|Procedures|T059|Laboratory Procedure
	// PROC|Procedures|T063|Molecular Biology Research Technique
	// PROC|Procedures|T062|Research Activity
	// PROC|Procedures|T061|Therapeutic or Preventive Procedure
	protected static String semanticTypesDiagnostic = "'T060','T065','T058','T059','T063','T062','T061'";

	// DISO|Disorders|T020|Acquired Abnormality
	// DISO|Disorders|T190|Anatomical Abnormality
	// DISO|Disorders|T049|Cell or Molecular Dysfunction
	// DISO|Disorders|T019|Congenital Abnormality
	// DISO|Disorders|T047|Disease or Syndrome
	// DISO|Disorders|T050|Experimental Model of Disease
	// DISO|Disorders|T037|Injury or Poisoning
	// DISO|Disorders|T048|Mental or Behavioral Dysfunction
	// DISO|Disorders|T191|Neoplastic Process
	// DISO|Disorders|T046|Pathologic Function
	// DISO|Disorders|T033|Finding
	// DISO|Disorders|T184|Sign or Symptom
	protected static String semanticTypesSymptom = "'T020','T190','T049','T019','T047','T050','T037','T048', 'T191', 'T046', 'T033', 'T184'";

	// PHYS|Physiology|T043|Cell Function
	// PHYS|Physiology|T201|Clinical Attribute
	// PHYS|Physiology|T045|Genetic Function
	// PHYS|Physiology|T041|Mental Process
	// PHYS|Physiology|T044|Molecular Function
	// PHYS|Physiology|T032|Organism Attribute
	// PHYS|Physiology|T040|Organism Function
	// PHYS|Physiology|T042|Organ or Tissue Function
	// PHYS|Physiology|T039|Physiologic Function
	protected static String semanticTypesChracteristic = "'T043','T201','T045', 'T041', 'T044', 'T032', 'T040', 'T042', 'T039'";

	private Connection con = null;
	private HashMap<String, PreparedStatement> findCUIs = new HashMap<>();

	private PreparedStatement setupType(Connection con, String types) throws SQLException {
		/*
		 * PreparedStatement findCUI = con.prepareStatement(
		 * "select distinct c.cui, code from MRCONSO c, MRSTY s where str = ? and c.cui=s.cui and code like 'C%' and code not like 'CDR%' and sab like 'NCI%' and s.tui in ("
		 * + types + ")");
		 */

		PreparedStatement findCUI = con.prepareStatement(
				"select distinct c.cui, code from MRCONSO c, MRSTY s where str = ? and c.cui=s.cui and s.tui in ("
						+ types + ")");
		return findCUI;
	}

	private void setup() throws ClassNotFoundException, NamingException, SQLException, ConfigurationException {
		con = DBConnection.getConnection();

		findCUIs.put("Anatomy", setupType(con, semanticTypesAnatomy));
		findCUIs.put("Characteristic", setupType(con, semanticTypesChracteristic));
		findCUIs.put("Diagnostic_tool", setupType(con, semanticTypesDiagnostic));
		findCUIs.put("Disease", setupType(con, semanticTypesDisease));
		findCUIs.put("Symptom", setupType(con, semanticTypesSymptom));
	}

	public void initialize(UimaContext context) throws ResourceInitializationException {
		try {
			setup();
		} catch (ClassNotFoundException | NamingException | SQLException | ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		// Check for entities
		for (NamedEntityMention e : JCasUtil.select(aJCas, NamedEntityMention.class)) {
			try {
				String CUI = normalize(e.getCoveredText(), e.getMentionType());

				if (CUI != null) {
					NamedEntity ne = new NamedEntity(aJCas);
					ne.setEntityId(CUI);
					e.setMentionedEntity(ne);
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// After checking for entities, update the aliases
		for (Relation r : JCasUtil.select(aJCas, Relation.class)) {
			// 1. Look for alias, create a table if at least one of the entities has a CUI
			if (r.getRelationType().startsWith("Alias")) {
				String cui1 = (r.getArg1().getMentionedEntity() == null ? null
						: r.getArg1().getMentionedEntity().getEntityId());
				String cui2 = (r.getArg2().getMentionedEntity() == null ? null
						: r.getArg2().getMentionedEntity().getEntityId());

				if (cui1 != null && cui2 != null && !cui1.equals(cui2)) {
					logger.log(Level.WARNING, "Alias entities have different ids.");
					logger.log(Level.WARNING, r.getArg1().toString());
					logger.log(Level.WARNING, r.getArg2().toString());
					continue;
				}

				NamedEntity ne = (cui1 == null ? r.getArg2().getMentionedEntity() : r.getArg1().getMentionedEntity());

				r.getArg1().setMentionedEntity(ne);
				r.getArg2().setMentionedEntity(ne);

				// 2. Update all alias in the jcas -- CUI not set and is of the same type and
				// one of the names match
				for (NamedEntityMention e : JCasUtil.select(aJCas, NamedEntityMention.class)) {
					if (e.getType().equals(r.getArg1().getType())
							&& (e.getCoveredText().equals(r.getArg1().getCoveredText())
									|| e.getCoveredText().equals(r.getArg2().getCoveredText()))) {
						e.setMentionedEntity(r.getArg1().getMentionedEntity());
					}
				}
			}
		}
	}

	private String normalize(String term, String type) throws SQLException {
		// Look for the term in UMLS -- found in NCI -- done -- use semantic type for
		// verification
		// If found in UMLS -- identify how to relate it to NCI -- done
		// If not found -- is it an acronym? (look at alias) -- restart looking for the
		// long form
		// If not found -- does it need to be split? -- split and try again
		// If not, then use term similarity to find potential candidate hyponyms
		PreparedStatement findCUI = findCUIs.get(type);

		if (findCUI != null) {
			findCUI.setString(1, term);
			ResultSet rs = findCUI.executeQuery();

			// System.out.println(term);
			/*
			 * while (rs.next()) { System.out.println(String.format("%s|%s|%s|%s", type,
			 * term, rs.getString(1), rs.getString(2))); }
			 */

			if (rs.next()) {
				return rs.getString(1);
			}
		}

		return null;
	}

	public static void main(String[] argc) throws SQLException, FileNotFoundException, IOException,
			ClassNotFoundException, NamingException, ConfigurationException {
		EntityNormalizer en = new EntityNormalizer();
		en.setup();

		// Test
		String[][] typesTest = { { "Anatomy", "/Users/ajimeno/Documents/work/ai4eye/nlp/normalization/anatomy.txt" },
				{ "Characteristic", "/Users/ajimeno/Documents/work/ai4eye/nlp/normalization/characteristic.txt" },
				{ "Diagnostic_tool", "/Users/ajimeno/Documents/work/ai4eye/nlp/normalization/diagnostic.txt" },
				{ "Disease", "/Users/ajimeno/Documents/work/ai4eye/nlp/normalization/disease.txt" },
				{ "Symptom", "/Users/ajimeno/Documents/work/ai4eye/nlp/normalization/symptom.txt" } };

		for (String[] entry : typesTest) {

			try (BufferedReader b = new BufferedReader(new FileReader(entry[1]))) {
				String line;
				while ((line = b.readLine()) != null) {
					line = line.trim().replaceAll("\n", "");

					if (line.length() > 0) {
						en.normalize(line, entry[0]);
					}
				}
			}
		}
	}
}