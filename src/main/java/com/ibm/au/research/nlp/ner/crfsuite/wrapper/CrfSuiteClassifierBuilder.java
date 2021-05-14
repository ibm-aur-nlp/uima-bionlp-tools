/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ner.crfsuite.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.cleartk.ml.encoder.features.NameNumber;
import org.cleartk.ml.jar.JarStreams;
import org.cleartk.ml.jar.SequenceClassifierBuilder_ImplBase;

/**
 * 
 * @author Antonio Jimeno Yepes
 *
 */
public class CrfSuiteClassifierBuilder
		extends SequenceClassifierBuilder_ImplBase<CrfSuiteClassifier, List<NameNumber>, String, String> {

	protected static final String TRAINING_DATA_FILE_NAME = "training-data.crfsuite";
	protected static final String TRAINING_MODEL_FILE_NAME = "model.crfsuite";

	protected static final String MODEL_FILE_PREFIX = "model";
	protected static final String MODEL_FILE_SUFFIX = "crfsuite";

	private File trainedModelFileName = null;

	@Override
	public File getTrainingDataFile(File dir) {
		return new File(dir, TRAINING_DATA_FILE_NAME);
	}

	@Override
	public void trainClassifier(File dir, String... args) throws Exception {
		CrfSuiteWrapper crfSuite = new CrfSuiteWrapper();
		crfSuite.train(dir, args);
	}

	@Override
	protected void packageClassifier(File dir, JarOutputStream modelStream) throws IOException {
		super.packageClassifier(dir, modelStream);
		JarStreams.putNextJarEntry(modelStream, TRAINING_MODEL_FILE_NAME, new File(dir, TRAINING_MODEL_FILE_NAME));
	}

	@Override
	protected void unpackageClassifier(JarInputStream modelStream) throws IOException {
		super.unpackageClassifier(modelStream);
		JarStreams.getNextJarEntry(modelStream, TRAINING_MODEL_FILE_NAME);

		this.trainedModelFileName = File.createTempFile(MODEL_FILE_PREFIX, MODEL_FILE_SUFFIX);
		this.trainedModelFileName.deleteOnExit();
		OutputStream out = new FileOutputStream(this.trainedModelFileName);

		int n;
		byte[] buffer = new byte[1024];
		while ((n = modelStream.read(buffer)) > -1) {
			out.write(buffer, 0, n);
		}
		out.close();
	}

	@Override
	protected CrfSuiteClassifier newClassifier() {
		return new CrfSuiteClassifier(this.featuresEncoder, this.outcomeEncoder, this.trainedModelFileName);
	}
}