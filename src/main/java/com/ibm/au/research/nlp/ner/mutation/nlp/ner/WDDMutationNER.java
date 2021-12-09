package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.io.File;

import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.token.tokenizer.TokenAnnotator;

import com.ibm.au.research.nlp.brat.BratGroundTruthReader;
import com.ibm.au.research.nlp.consumer.JCas2Brat;
import com.ibm.au.research.nlp.consumer.JCas2Text;
import com.ibm.au.research.nlp.ner.crfsuite.CRFTokenizerCorrection;

public class WDDMutationNER {
	public static AggregateBuilder getTrainingPipeline(String modelDirectory) throws ResourceInitializationException {
		// The pipeline of annotators
		AggregateBuilder builder = new AggregateBuilder();
		// A sentence annotator
		builder.add(SentenceAnnotator.getDescription());
		// An annotator that adds Token annotations
		builder.add(TokenAnnotator.getDescription());
		builder.add(CRFTokenizerCorrection.getDescription());
		
		//builder.add(PosTagger.getDescription());

		// An annotator that uses the brat annotation to add tokens and
		// NER tags to the CAS
		builder.add(BratGroundTruthReader.getDescription());

		// The CRF annotator, configured to write training data
		builder.add(WDDMutationCRFNERAnnotator.getWriterDescription(modelDirectory));

		return builder;
	}

	public static AggregateBuilder getAnnotationPipeline(String modelDirectory) throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		// An annotator that adds Sentence annotations
		builder.add(SentenceAnnotator.getDescription());
		// An annotator that adds Token annotations
		builder.add(TokenAnnotator.getDescription());
		builder.add(CRFTokenizerCorrection.getDescription());

		//builder.add(PosTagger.getDescription());

		builder.add(WDDMutationCRFNERAnnotator
				.getClassifierDescription(new File(modelDirectory, "model.jar").getAbsolutePath()));

		builder.add(WDDMutationRegExAnnotator.getDescription());
		builder.add(WDDMutationTermAnnotator.getDescription());
		builder.add(WDDRemoveNestedAnnotations.getDescription());

		builder.add(JCas2Text.getOutputFolderDescription(new File(modelDirectory, "brat").getAbsolutePath()));
		builder.add(JCas2Brat.getOutputFolderDescription(new File(modelDirectory, "brat").getAbsolutePath()));

		return builder;
	}

	public static AggregateBuilder getAnnotationComponentPipeline(String modelDirectory)
			throws ResourceInitializationException {
		return getAnnotationComponentPipeline(modelDirectory, true, true, true);
	}

	public static AggregateBuilder getAnnotationComponentPipeline(String modelDirectory, boolean crf, boolean regex,
			boolean term) throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		// An annotator that adds Sentence annotations
		builder.add(SentenceAnnotator.getDescription());
		// An annotator that adds Token annotations
		builder.add(TokenAnnotator.getDescription());
		builder.add(CRFTokenizerCorrection.getDescription());
		
		//builder.add(PosTagger.getDescription());

		if (crf) {
			builder.add(WDDMutationCRFNERAnnotator
					.getClassifierDescription(new File(modelDirectory, "model.jar").getAbsolutePath()));
		}
		
		if (regex) {
			builder.add(WDDMutationRegExAnnotator.getDescription());
		}
		
		if (term) {
			builder.add(WDDMutationTermAnnotator.getDescription());
		}
		
		builder.add(WDDRemoveNestedAnnotations.getDescription());
		
		// Remove entities from annotation
		builder.add(WDDFilterAnnotations.getDescription());

		return builder;
	}

}