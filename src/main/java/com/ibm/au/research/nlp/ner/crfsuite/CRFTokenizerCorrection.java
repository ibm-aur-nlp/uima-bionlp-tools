package com.ibm.au.research.nlp.ner.crfsuite;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.token.type.Token;

/**
 * Colon cases correction for CRFSuite (e.g. 4:30 to 4 : 30).
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@au1.ibm.com)
 *
 */
public class CRFTokenizerCorrection extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (Token t : JCasUtil.select(jCas, Token.class)) {
			if (t.getCoveredText().contains(":")) {
				t.removeFromIndexes();

				int index = t.getCoveredText().indexOf(":");
				(new Token(jCas, t.getBegin(), t.getBegin() + index)).addToIndexes();
				(new Token(jCas, t.getBegin() + index, t.getBegin() + index + 1)).addToIndexes();
				(new Token(jCas, t.getBegin() + index + 1, t.getEnd())).addToIndexes();
			}
		}
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(CRFTokenizerCorrection.class);
	}
}