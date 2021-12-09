package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

/**
 * Remove protein mutations that can be confused with cell lines. Cell lines are obtained from standard cell lines
 * 
 * @author antonio.jimeno@au1.ibm.com
 *
 */
public class WDDCellLineFilter extends JCasAnnotator_ImplBase {

	private static final String[] cellLinesArray = { "t47d", "t98g" };

	private static final Set<String> cellLines = new HashSet<>();

	static {
		for (String cellLine : cellLinesArray) {
			cellLines.add(cellLine);
		}
	}

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (NamedEntityMention namedEntity : JCasUtil.select(jCas, NamedEntityMention.class)) {
			if (namedEntity.getMentionType().equals("Mutation") && cellLines.contains(namedEntity.getCoveredText().toLowerCase())) {
				namedEntity.removeFromIndexes(jCas);
			}
		}
	}
}