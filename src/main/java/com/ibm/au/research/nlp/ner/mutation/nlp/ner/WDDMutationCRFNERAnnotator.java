package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
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
import org.cleartk.token.type.Token;

import com.ibm.au.research.nlp.ner.crfsuite.CrfSuiteNERAnnotator;
import com.ibm.au.research.nlp.ner.crfsuite.wrapper.CrfSuiteDataWriter;
import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.MutationTermFE;
import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.additionalRegexTerms;
import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.msiFilter;
import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.mutateStringFeatureFunction;
import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.regexTerms;

public class WDDMutationCRFNERAnnotator extends CrfSuiteNERAnnotator {

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		this.setTokenFeatureExtractor(new FeatureFunctionExtractor<Token>(new CoveredTextExtractor<Token>(),
				// new HGVSMutationRegExFE(),
				new MutationTermFE(), new LowerCaseFeatureFunction(), new additionalRegexTerms(), new msiFilter(),
				new mutateStringFeatureFunction(), new regexTerms(), new CapitalTypeFeatureFunction(),
				new NumericTypeFeatureFunction(), new CharacterNgramFeatureFunction(Orientation.RIGHT_TO_LEFT, 0, 2),
				new CharacterNgramFeatureFunction(Orientation.RIGHT_TO_LEFT, 0, 3)));

		// a feature extractor that extracts the surrounding token texts (within
		// the same sentence)
		this.setContextFeatureExtractor(new CleartkExtractor<Token, Token>(Token.class,
				new CoveredTextExtractor<Token>(), new Preceding(2), new Following(2)));
	}

	public static AnalysisEngineDescription getClassifierDescription(String modelFileName)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(WDDMutationCRFNERAnnotator.class,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelFileName);
	}

	private static final Pattern filterPattern = Pattern.compile("[0-9\\(\\)]");

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		// Annotate documents using CRF trained model
		super.process(jCas);

		// Correct some incorrect annotations by removing annotations with a
		// number or a parenthesis
		JCasUtil.select(jCas, NamedEntityMention.class).stream()
				.filter(ne -> filterPattern.matcher(ne.getCoveredText()).find()).collect(Collectors.toList()).stream()
				.forEach(ne -> ne.removeFromIndexes(jCas));
	}

	public static AnalysisEngineDescription getWriterDescription(String outputDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(WDDMutationCRFNERAnnotator.class,
				CleartkSequenceAnnotator.PARAM_IS_TRAINING, true, DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory, DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				CrfSuiteDataWriter.class);
	}
}