/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ner.crfsuite;

import java.io.File;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.jar.Train;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.ibm.au.research.nlp.brat.BratGroundTruthReader;
import com.ibm.au.research.nlp.util.TextFileFilter;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class BuildNERModel {
	public interface Options {
		@Option(longName = "train-dir", description = "The directory containing BRAT-annotated files", defaultValue = "C:\\work\\data\\mutation-tagger\\almaden-gt\\brat_files_only_2016-06-10_gt")
		public File getTrainDirectory();
	}

	public static void main(String[] argc) throws Exception {
		Options options = CliFactory.parseArguments(Options.class, argc);

		String outputDirectory = "target/examples/ner";

		CollectionReaderDescription reader = UriCollectionReader
				.getDescriptionFromDirectory(options.getTrainDirectory(), TextFileFilter.class, null);

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(UriToDocumentTextAnnotator.getDescription());
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(BratGroundTruthReader.getDescription());
		builder.add(CrfSuiteNERAnnotator.getWriterDescription(outputDirectory));

		SimplePipeline.runPipeline(reader, builder.createAggregateDescription());

		System.out.println("training data written to " + outputDirectory);
		System.out.println("training model...");

		Train.main(outputDirectory);

		System.out.println("model written to "
				+ JarClassifierBuilder.getModelJarFile(outputDirectory).getPath());
	}
}