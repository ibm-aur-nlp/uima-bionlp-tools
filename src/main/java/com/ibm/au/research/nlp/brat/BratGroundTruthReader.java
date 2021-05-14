/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.brat;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;

import com.ibm.au.research.nlp.types.Event;
import com.ibm.au.research.nlp.types.EventRole;
import com.ibm.au.research.nlp.types.Relation;

import au.com.nicta.csp.brateval.Annotations;
import au.com.nicta.csp.brateval.Document;
import au.com.nicta.csp.brateval.Entity;

public class BratGroundTruthReader extends JCasAnnotator_ImplBase {
	private static Pattern P_COLON = Pattern.compile(":");
	private static final BratToUIMAInserter inserter = new BratToUIMAInserter(false);

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(BratGroundTruthReader.class);
	}

	private NamedEntityMention getEntity(JCas jCas, String id) {
		for (NamedEntityMention e : JCasUtil.select(jCas, NamedEntityMention.class)) {
			if (e.getMentionId().equals(id)) {
				return e;
			}
		}

		return null;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		// Text file
		File textFile = new File(ViewUriUtil.getURI(jCas));

		// Annotation file
		String annFileName = BratUtils.annsFilenameFromText(textFile.getPath());

		// Named entities
		try {
			Document d = Annotations.read(annFileName, "ann");

			// Generate named entities from brat annotation
			for (Entity e : d.getEntities()) {
				// TODO: entities in multiple spans
				inserter.insert(jCas, d, e);
			}

			// Generate named relations from brat annotation
			for (au.com.nicta.csp.brateval.Relation r : d.getRelations()) {
				Relation relation = new Relation(jCas, 0, jCas.getDocumentText().length());
				relation.setRelationId(r.getId());
				relation.setRelationType(r.getRelationType());
				relation.setArg1(getEntity(jCas, r.getEntity1Id()));
				relation.setArg2(getEntity(jCas, r.getEntity2Id()));

				jCas.addFsToIndexes(relation);
			}

			for (au.com.nicta.csp.brateval.Event ev : d.getEvents()) {
				Event event = new Event(jCas);
				event.setEventId(ev.getId());
				event.setEventType(ev.getType());
				event.setTrigger(getEntity(jCas, ev.getEventTrigger()));

				event.setRoles(new FSArray(jCas, ev.getArguments().size()));

				for (int i = 0; i < ev.getArguments().size(); i++) {
					String[] tokens = P_COLON.split(ev.getArguments().get(i));
					EventRole eventRole = new EventRole(jCas);
					eventRole.setRole(tokens[0]);
					eventRole.setEntity(getEntity(jCas, tokens[1]));
					event.getRoles().set(i, eventRole);
				}

				jCas.addFsToIndexes(event);
			}
		} catch (IOException e) { // Probably not the best approach, other
									// solutions welcome
			throw new RuntimeException(e);
		}
	}

}