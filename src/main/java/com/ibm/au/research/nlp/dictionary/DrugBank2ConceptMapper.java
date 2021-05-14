/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.dictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringEscapeUtils;

public class DrugBank2ConceptMapper {

	private static Pattern pPipe = Pattern.compile("\\|");

	private static Set<String> getStopwords() throws FileNotFoundException, IOException {
		Set<String> words = new HashSet<>();

		try (BufferedReader b = new BufferedReader(new FileReader("./stopwords-drugs.txt"))) {
			String line;

			while ((line = b.readLine()) != null) {
				String word = line.trim();

				if (word.length() > 0) {
					words.add(word.toLowerCase());
				}
			}
		}
		return words;
	}

	private static void addTerm(String term, Set<String> set, Set<String> stopwords) {
		term = term.trim();

		if (term.length() > 3 && !stopwords.contains(term.toLowerCase())) {
			set.add(term);
		}
	}

	public static void main(String[] argc) throws IOException {

		try (Reader reader = Files.newBufferedReader(Paths.get(argc[0]));
				CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
				BufferedWriter w = new BufferedWriter(new FileWriter(argc[1]))) {
			boolean isFirst = true;

			Set<String> stopwords = getStopwords();

			w.write("<?xml version='1.0' encoding='UTF8'?>");
			w.newLine();
			w.write("<synonym>");
			w.newLine();

			for (CSVRecord csvRecord : csvParser) {
				if (!isFirst) {
					// Accessing Values by Column Index
					String id = csvRecord.get(0);

					Set<String> set = new HashSet<>();

					addTerm(csvRecord.get(2), set, stopwords);

					for (String term : pPipe.split(csvRecord.get(5))) {
						addTerm(term, set, stopwords);
					}

					if (set.size() > 0) {
						w.write("<token canonical=\"" + StringEscapeUtils.escapeXml(id) + "\">");
						w.newLine();

						for (String term : set) {
							w.write("<variant base=\"" + StringEscapeUtils.escapeXml(term.trim()) + "\"/>");
							w.newLine();
						}

						w.write("</token>");
						w.newLine();
					}

				} else {
					isFirst = false;
				}
			}

			w.write("</synonym>");
			w.newLine();
		}

	}
}