package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

public class WDDFilterAnnotations extends JCasAnnotator_ImplBase {

	private Set<String> swSet = new HashSet<>();

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		// Load dictionary file

		try (BufferedReader b = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/stopwords.txt")))) {
			String line;

			while ((line = b.readLine()) != null) {
				if (line.trim().length() > 0)
					swSet.add(line.trim().toLowerCase());
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(WDDFilterAnnotations.class);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (NamedEntityMention namedEntity : JCasUtil.select(jCas, NamedEntityMention.class)) {
			if (namedEntity.getMentionType().equals("Mutation")) {
				if (namedEntity.getCoveredText().length() < 3) {
					namedEntity.removeFromIndexes(jCas);
				} else {
					// Check entity to a stopword list
					if (swSet.contains(namedEntity.getCoveredText().toLowerCase())) {
						namedEntity.removeFromIndexes(jCas);
					}
				}
			}
		}
	}
}