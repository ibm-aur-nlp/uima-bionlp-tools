/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ingestion.uima.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.cleartk.util.ViewUriUtil;

public class MedlineReader extends JCasCollectionReader_ImplBase {
	public static final String PARAM_FILE_NAME = "fileName";
	public Stack<IngestionDocument> documents = new Stack<>();

	public String fileName = null;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		fileName = (String) context.getConfigParameterValue(PARAM_FILE_NAME);

		try {
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			SAXParser saxParser = saxParserFactory.newSAXParser();
			MedlineSaxParser handler = new MedlineSaxParser();
			saxParser.parse(new GZIPInputStream(new FileInputStream(fileName)), handler);
			documents = handler.getDocuments();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return !documents.empty();
	}

	@Override
	public Progress[] getProgress() {
		return null;
	}

	public void getNext(JCas jCas) throws IOException, CollectionException {
		if (this.hasNext()) {
			IngestionDocument d = documents.pop();
			jCas.setDocumentText(d.getText());
			try {
				ViewUriUtil.setURI(jCas, new URI(d.getSource() + "/" + d.getId() + "/" + d.getSection()));
				//System.out.println("World: " + ViewUriUtil.getURI(jCas));
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static CollectionReaderDescription getDescriptionFromFiles(String fileName)
			throws ResourceInitializationException {
		return CollectionReaderFactory.createReaderDescription(MedlineReader.class, null, PARAM_FILE_NAME, fileName);
	}
}