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
import java.util.Stack;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MedlineSaxParser extends DefaultHandler {
	private enum Tag {
		PMID, ArticleTitle, AbstractText, NoTag
	}

	private Tag tag = null;
	private String version = null;

	private Stack<IngestionDocument> docs = new Stack<>();

	private StringBuilder pmid = new StringBuilder();
	private StringBuilder articleTitle = new StringBuilder();
	private StringBuilder abstractText = new StringBuilder();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("PMID")) {
			if (pmid.length() == 0) {
				tag = Tag.PMID;
				version = attributes.getValue("Version");
			}
		} else if (qName.equalsIgnoreCase("ArticleTitle")) {
			tag = Tag.ArticleTitle;
		} else if (qName.equalsIgnoreCase("AbstractText")) {
			tag = Tag.AbstractText;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("MedlineCitation")) {
			// Generate document
			docs.push(new IngestionDocument("MEDLINE", pmid.toString() + "-" + version, "TITLE",
					articleTitle.toString().trim()));
			docs.push(new IngestionDocument("MEDLINE", pmid.toString() + "-" + version, "ABSTRACT",
					abstractText.toString().trim()));

			version = null;

			pmid.setLength(0);
			articleTitle.setLength(0);
			abstractText.setLength(0);
		} else { // Clear tag
			tag = null;
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (tag != null) {
			switch (tag) {
			case PMID:
				pmid.append(new String(ch, start, length));
				break;
			case ArticleTitle:
				articleTitle.append(new String(ch, start, length)).append(" ");
				break;
			case AbstractText:
				abstractText.append(new String(ch, start, length)).append(" ");
				break;
			default:
				break;
			}
		}
	}

	public Stack<IngestionDocument> getDocuments() {
		return docs;
	}
}