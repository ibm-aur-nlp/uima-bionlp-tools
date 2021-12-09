package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.CYPAlleleRegExFE;
import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.HLARegExFE;
import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.MutationRegExFE;

public class WDDMutationRegExAnnotator extends JCasAnnotator_ImplBase {

	private Map<String, Pattern> patterns = new HashMap<String, Pattern>();

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		patterns = MutationRegExFE.getPatterns();
		patterns.putAll(CYPAlleleRegExFE.getPatterns());
		patterns.putAll(HLARegExFE.getPatterns());
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(WDDMutationRegExAnnotator.class);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
			Pattern p = entry.getValue();

			Matcher m = p.matcher(jCas.getDocumentText());

			while (m.find()) {
				NamedEntityMention ne = new NamedEntityMention(jCas, m.start(), m.end());
				ne.setMentionType("Mutation");
				jCas.addFsToIndexes(ne);
			}

		}

	}

}