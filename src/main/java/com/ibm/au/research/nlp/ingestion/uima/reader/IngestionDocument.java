/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ingestion.uima.reader;

public class IngestionDocument {
	private String source;
	private String id;
	private String section;
	private String text;

	public IngestionDocument(String source, String id, String section, String text) {
		this.source = source;
		this.id = id;
		this.section = section;
		this.text = text;
	}

	public String getSource() {
		return source;
	}

	public String getId() {
		return id;
	}

	public String getSection() {
		return section;
	}

	public String getText() {
		return text;
	}
}