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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Produces a ConceptMapper dictionary file from NCBI gene database file
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@au1.ibm.com)
 *
 */
public class NCBIGene2ConceptMapper {
	private static final Pattern p_tab = Pattern.compile("\\t");
	private static final Pattern p_pipe = Pattern.compile("\\|");

	private static Set<String> getStopwords() throws FileNotFoundException, IOException {
		Set<String> words = new HashSet<>();

		try (BufferedReader b = new BufferedReader(new FileReader("./stopwords.txt"))) {
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

	private static void addTerm(String term, Set<String> terms, Set<String> stopwords) {
		if (!stopwords.contains(term.toLowerCase())) {
			terms.add(term);
		}
	}

	public static void main(String[] argc) throws IOException {
		Set<String> stopwords = getStopwords();

		try (BufferedReader b = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(argc[0]))))) {
			try (BufferedWriter w = new BufferedWriter(
					new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(argc[1]))))) {
				String line;

				w.write("<?xml version='1.0' encoding='UTF8'?>");
				w.newLine();
				w.write("<synonym>");
				w.newLine();

				while ((line = b.readLine()) != null) {
					String[] token = p_tab.split(line);

					if (token.length == 16) {
						Set<String> set = new HashSet<String>();

						addTerm(token[2], set, stopwords);

						for (String synonym : p_pipe.split(token[4])) {
							if (synonym.length() > 1)
								addTerm(synonym, set, stopwords);
						}

						if (token[10].length() > 1)
							addTerm(token[10], set, stopwords);
						if (token[11].length() > 1)
							addTerm(token[11], set, stopwords);

						if (set.size() > 0) {
							w.write("<token canonical=\"" + StringEscapeUtils.escapeXml(token[1]) + "\">");
							w.newLine();

							for (String term : set) {
								w.write("<variant base=\"" + StringEscapeUtils.escapeXml(term) + "\"/>");
								w.newLine();
							}

							w.write("</token>");
							w.newLine();
						}
					}
				}

				w.write("</synonym>");
				w.flush();
			}
		}
	}
}