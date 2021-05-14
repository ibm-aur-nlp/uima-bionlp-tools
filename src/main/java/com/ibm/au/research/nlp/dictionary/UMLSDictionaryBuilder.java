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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang.StringEscapeUtils;

import com.ibm.au.research.nlp.util.DBConnection;

public class UMLSDictionaryBuilder {
	private static Set<String> getStopwords() throws FileNotFoundException, IOException {
		Set<String> words = new HashSet<>();

		try (BufferedReader b = new BufferedReader(new FileReader("./stopwords-dictionary-builder.txt"))) {
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

	private static void addStopwords(String swFileName, Set<String> words) throws FileNotFoundException, IOException {
		try (BufferedReader b = new BufferedReader(new FileReader(swFileName))) {
			String line;

			while ((line = b.readLine()) != null) {
				String word = line.trim();

				if (word.length() > 0) {
					words.add(word.toLowerCase());
				}
			}
		}
	}

	private static void generateWords(BufferedWriter w, String typePrefix, String semanticTypes, String stopwordsFile)
			throws FileNotFoundException, IOException, ClassNotFoundException, SQLException, NamingException,
			ConfigurationException {
		Set<String> stopwords = getStopwords();
		addStopwords(stopwordsFile, stopwords);

		try (Connection con = DBConnection.getConnection();
				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct m.cui, str from MRCONSO m, MRSTY s\n"
						+ " where lat = 'ENG' and suppress = \"N\" and str not like '% NOS' and str not like '%,%' and str not like '%--%' and str not like '%(%' and str not like '%[%' and str not like '%/%' " // and
																																																					// (select
																																																					// count(mi.str)
																																																					// from
																																																					// MRCONSO
																																																					// mi
																																																					// where
																																																					// mi.str
																																																					// =
																																																					// m.str)
																																																					// <
																																																					// 10\n"
						+ "   and m.cui = s.cui\n" + "   and s.sty in (" + semanticTypes + ")\n"
						+ " order by cui, str"))

		{
			String id = null;

			while (rs.next()) {
				if (!stopwords.contains(rs.getString(2).toLowerCase())) {
					if (id == null) {
						id = rs.getString(1);
						w.write("<token canonical=\"" + typePrefix + "-" + StringEscapeUtils.escapeXml(id) + "\">");
						w.newLine();
					} else if (!id.equals(rs.getString(1))) {
						w.write("</token>");
						w.newLine();
						id = rs.getString(1);
						w.write("<token canonical=\"" + typePrefix + "-" + StringEscapeUtils.escapeXml(id) + "\">");
						w.newLine();
					}

					w.write("<variant base=\"" + StringEscapeUtils.escapeXml(rs.getString(2)) + "\"/>");
					w.newLine();
				}
			}

			// Close current variant
			if (id != null) {
				w.write("</token>");
				w.newLine();
			}
		}
	}

	public static void main(String[] argc)
			throws ClassNotFoundException, NamingException, SQLException, ConfigurationException, IOException {

		try (BufferedWriter w = new BufferedWriter(new FileWriter(argc[0]))) {
			w.write("<?xml version='1.0' encoding='UTF8'?>");
			w.newLine();
			w.write("<synonym>");
			w.newLine();

			generateWords(w, "D",
					"\"Disease or Syndrome\", \"Neoplastic Process\", \"Congenital Abnormality\", \"Mental or Behavioral Dysfunction\", \"Experimental Model of Disease\", \"Acquired Abnormality\"",
					"./diseaseStopwords.txt");
			generateWords(w, "S", "\"Sign or Symptom\", \"Finding\"", "./symptomStopwords.txt");
			generateWords(w, "A", "\"Body Part, Organ, or Organ Component\", \"Body Space or Junction\"",
					"./anatomyStopwords.txt");

			w.write("</synonym>");
			w.newLine();
		}
	}
}