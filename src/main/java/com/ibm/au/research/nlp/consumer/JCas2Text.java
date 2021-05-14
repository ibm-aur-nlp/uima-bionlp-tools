/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.consumer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;

/**
 * Consumer to write the jCas text in the output folder
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class JCas2Text extends JCasAnnotator_ImplBase {
	public static final String PARAM_OUTPUT_FOLDER_NAME = "outputFolderName";

	private String outputFolderName = null;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		outputFolderName = (String) context.getConfigParameterValue(PARAM_OUTPUT_FOLDER_NAME);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		try {
			String fileName = new File(ViewUriUtil.getURI(jCas)).getName();
			if (!fileName.endsWith(".txt")) {
				fileName = fileName + ".txt";
			}
			try (Writer w = Files.newBufferedWriter(Paths.get(outputFolderName).resolve(fileName))) {
				w.write(jCas.getDocumentText());
			}
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

	public static AnalysisEngineDescription getOutputFolderDescription(String outputFolderName)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(JCas2Text.class, PARAM_OUTPUT_FOLDER_NAME,
				outputFolderName);
	}
}