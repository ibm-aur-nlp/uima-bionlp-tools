package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

public class WDDRemoveNestedAnnotations extends JCasAnnotator_ImplBase {

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(WDDRemoveNestedAnnotations.class);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		Set<NamedEntityMention> removeNEs = new HashSet<>();

		for (NamedEntityMention namedEntity : JCasUtil.select(jCas, NamedEntityMention.class)) {
			if (!removeNEs.contains(namedEntity) && namedEntity.getMentionType().equals("Mutation")) {
				for (NamedEntityMention nestedNamedEntity : JCasUtil.selectCovered(jCas, NamedEntityMention.class,
						namedEntity.getBegin(), namedEntity.getEnd())) {
					if (namedEntity != nestedNamedEntity && nestedNamedEntity.getMentionType().equals("Mutation")) {
						removeNEs.add(nestedNamedEntity);
					}
				}
			}
		}

		for (NamedEntityMention removeNE : removeNEs) {
			removeNE.removeFromIndexes(jCas);
		}
	}
}