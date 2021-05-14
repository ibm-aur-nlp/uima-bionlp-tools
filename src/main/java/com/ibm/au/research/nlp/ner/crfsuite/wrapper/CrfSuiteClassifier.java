/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ner.crfsuite.wrapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.encoder.features.FeaturesEncoder;
import org.cleartk.ml.encoder.features.NameNumber;
import org.cleartk.ml.encoder.outcome.OutcomeEncoder;
import org.cleartk.ml.jar.SequenceClassifier_ImplBase;

/**
 * 
 * @author Antonio Jimeno Yepes
 *
 */
public class CrfSuiteClassifier extends SequenceClassifier_ImplBase<List<NameNumber>, String, String> {

	private final String PREFIX_FILE_NAME = "crfsuite";
	private final String SUFFIX_FILE_NAME = "tagging";

	private final CrfSuiteWrapper wrapper = new CrfSuiteWrapper();

	private File trainedModelFileName = null;

	public CrfSuiteClassifier(FeaturesEncoder<List<NameNumber>> featuresEncoder,
			OutcomeEncoder<String, String> outcomeEncoder, File trainedModelFileName) {
		super(featuresEncoder, outcomeEncoder);
		this.featuresEncoder = featuresEncoder;
		this.outcomeEncoder = outcomeEncoder;
		this.trainedModelFileName = trainedModelFileName;
	}

	public List<String> classifyCAS(List<List<List<Feature>>> features) throws CleartkProcessingException {
		// Write temporary file with the annotation
		List<String> output = null;
		File tagging_file = null;

		try {
			tagging_file = File.createTempFile(PREFIX_FILE_NAME, SUFFIX_FILE_NAME);

			// Write tagging file
			try (BufferedWriter w = new BufferedWriter(new FileWriter(tagging_file))) {
				for (List<List<Feature>> sentence : features) {
					for (List<Feature> instance : sentence) {
						for (NameNumber f : featuresEncoder.encodeAll(instance)) {
							w.write("\t");
							w.write(f.name);
						}
						w.newLine();
					}
					w.newLine();
				}
			}

			// Run the classifier
			output = wrapper.classify(tagging_file, trainedModelFileName);

		} catch (IOException | InterruptedException e) {
			throw new CleartkProcessingException(e);
		} finally {
			if (tagging_file != null)
				tagging_file.delete();
		}

		// Return the new list
		return output;
	}

	@Override
	public List<String> classify(List<List<Feature>> features) throws CleartkProcessingException {
		// Write temporary file with the annotation
		List<String> output = null;
		File tagging_file = null;

		try {
			tagging_file = File.createTempFile(PREFIX_FILE_NAME, SUFFIX_FILE_NAME);

			// Write tagging file
			try (BufferedWriter w = new BufferedWriter(new FileWriter(tagging_file))) {
				for (List<Feature> instance : features) {
					for (NameNumber f : featuresEncoder.encodeAll(instance)) {
						w.write("\t");
						w.write(f.name);
					}
					w.newLine();
				}
				w.newLine();
			}

			// Run the classifier
			output = wrapper.classify(tagging_file, trainedModelFileName);

		} catch (IOException | InterruptedException e) {
			throw new CleartkProcessingException(e);
		} finally {
			if (tagging_file != null)
				tagging_file.delete();
		}

		// Return the new list
		return output;
	}
}
