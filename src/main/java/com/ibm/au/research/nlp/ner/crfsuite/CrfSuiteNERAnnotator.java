/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ner.crfsuite;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instances;
import org.cleartk.ml.chunking.BioChunking;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;
import org.cleartk.ml.feature.function.CapitalTypeFeatureFunction;
import org.cleartk.ml.feature.function.CharacterNgramFeatureFunction;
import org.cleartk.ml.feature.function.CharacterNgramFeatureFunction.Orientation;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.cleartk.ml.feature.function.LowerCaseFeatureFunction;
import org.cleartk.ml.feature.function.NumericTypeFeatureFunction;
import org.cleartk.ml.jar.DefaultSequenceDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;

import com.ibm.au.research.nlp.ner.crfsuite.wrapper.CrfSuiteClassifier;
import com.ibm.au.research.nlp.ner.crfsuite.wrapper.CrfSuiteDataWriter;

/**
 * 
 * Example class to train a named entity annotator using CrfSuite implementation
 * for CRFs
 *
 */
public class CrfSuiteNERAnnotator extends CleartkSequenceAnnotator<String> {
	private FeatureExtractor1<Token> tokenFeatureExtractor;

	private FeatureExtractor1<Token> posFeatureExtractor;

	private CleartkExtractor<Token, Token> contextFeatureExtractor;

	private BioChunking<Token, NamedEntityMention> chunking;

	protected void setTokenFeatureExtractor(FeatureFunctionExtractor<Token> fe) {
		this.tokenFeatureExtractor = fe;
	}

	protected void setPosFeatureExtractor(FeatureFunctionExtractor<Token> fe) {
		this.posFeatureExtractor = fe;
	}

	protected void setContextFeatureExtractor(CleartkExtractor<Token, Token> fe) {
		this.contextFeatureExtractor = fe;
	}

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		// a feature extractor that creates features corresponding to the word,
		// the word lower cased
		// the capitalization of the word, the numeric characterization of the
		// word, and character ngram
		// suffixes of length 2 and 3.
		this.tokenFeatureExtractor = new FeatureFunctionExtractor<Token>(new CoveredTextExtractor<Token>(),
				new LowerCaseFeatureFunction(), new CapitalTypeFeatureFunction(), new NumericTypeFeatureFunction(),
				new CharacterNgramFeatureFunction(Orientation.RIGHT_TO_LEFT, 0, 2),
				new CharacterNgramFeatureFunction(Orientation.RIGHT_TO_LEFT, 0, 3));

		this.posFeatureExtractor = new FeatureFunctionExtractor<Token>(
				new TypePathExtractor<Token>(Token.class, "pos"));

		// a feature extractor that extracts the surrounding token texts (within
		// the same sentence)
		this.contextFeatureExtractor = new CleartkExtractor<Token, Token>(Token.class,
				new CoveredTextExtractor<Token>(), new Preceding(2), new Following(2));

		this.chunking = new BioChunking<Token, NamedEntityMention>(Token.class, NamedEntityMention.class,
				"mentionType");
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		// for each sentence in the document, generate training/classification
		// instances
		List<List<List<Feature>>> featuresListsCAS = new ArrayList<>();

		for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
			List<List<Feature>> featureLists = new ArrayList<List<Feature>>();

			List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);

			for (Token token : tokens) {
				// apply the two feature extractors
				List<Feature> tokenFeatures = new ArrayList<Feature>();
				tokenFeatures.addAll(this.tokenFeatureExtractor.extract(jCas, token));
				tokenFeatures.addAll(this.posFeatureExtractor.extract(jCas, token));
				tokenFeatures.addAll(this.contextFeatureExtractor.extractWithin(jCas, token, sentence));
				featureLists.add(tokenFeatures);
			}

			// for training, write instances to the data write
			if (this.isTraining()) {
				// extract the gold (human annotated) NamedEntityMention
				// annotations
				List<NamedEntityMention> namedEntityMentions = JCasUtil.selectCovered(jCas, NamedEntityMention.class,
						sentence);

				// convert the NamedEntityMention annotations into token-level
				// BIO outcome labels
				List<String> outcomes = this.chunking.createOutcomes(jCas, tokens, namedEntityMentions);

				// write the features and outcomes as training instances
				this.dataWriter.write(Instances.toInstances(outcomes, featureLists));
			} else {
				featuresListsCAS.add(featureLists);
			}
		}

		// for classification, create the named entities
		if (!this.isTraining()) {
			List<String> outcomesCAS = ((CrfSuiteClassifier) this.classifier).classifyCAS(featuresListsCAS);

			List<List<String>> outcomes = new ArrayList<>();

			// Split the output into a list of lists
			List<String> elements = new ArrayList<String>();
			outcomes.add(elements);

			for (String outcome : outcomesCAS) {
				if (outcome.trim().length() == 0) {
					elements = new ArrayList<>();
					outcomes.add(elements);
				} else {
					elements.add(outcome);
				}
			}

			// Traverse the list of elements
			int count = 0;

			for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
				List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);

				this.chunking.createChunks(jCas, tokens, outcomes.get(count));
				count++;
			}
		}
	}

	public static AnalysisEngineDescription getClassifierDescription(String modelFileName)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(CrfSuiteNERAnnotator.class,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelFileName);
	}

	public static AnalysisEngineDescription getWriterDescription(String outputDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(CrfSuiteNERAnnotator.class,
				CleartkSequenceAnnotator.PARAM_IS_TRAINING, true, DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory, DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				CrfSuiteDataWriter.class);
	}
}