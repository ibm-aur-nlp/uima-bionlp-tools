package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.token.type.Token;

public class WDDGeneMutationAnnotator extends JCasAnnotator_ImplBase {

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(WDDGeneMutationAnnotator.class);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (Token token : JCasUtil.select(jCas, Token.class)) {
		  if (token.getCoveredText().equalsIgnoreCase("brafv600e"))
		  {
			// Generate gene, mutation and the relation between both entities
			
		  }
		}
	}
}