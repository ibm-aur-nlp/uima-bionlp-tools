/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ingestion.kg;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;

import org.apache.commons.configuration2.ex.ConfigurationException;

import com.ibm.au.research.nlp.util.DBConnection;

/**
 * 
 * Generate graph of highly probable correlated terms
 * 
 * @author Antonio Jimeno Yepes
 *
 */
public class CreateCooccurrenceGraph {
	// Consider adding Mutation
	private static String[] types = { "Mutation", "Disease", "Anatomy", "Symptom", "Drug", "Gene_protein" };

	private static String[][] diseases = { {"D-C0270549", "Generalized anxiety disorder"}};  
		//{ { "D-C0017601", "Glaucoma" }, { "D-C0271051", "Macular edema" },
		//	{ "D-C0242383", "AMD" }, { "D-C0730285", "Diabetic macular edema" },
		//	{ "D-C0002395", "Alzheimer disease" } };

	public static void main(String[] argc)
			throws ClassNotFoundException, SQLException, NamingException, ConfigurationException, IOException {
		// Find a paper that describes this approach and sounds right

		// Main concept of interest, focused on a disease or group of diseases

		// Recover concepts from different types
		try (Connection con = DBConnection.getConnection();
				PreparedStatement terms = con.prepareStatement(
						// "select count(e2.db_id), e2.db_id from entity e1, entity e2 where e1.db_id =
						// ? and e1.source = e2.source and e1.source_id = e2.source_id and e2.type = ?
						// group by e2.db_id;");
						"select count(*), e2.db_id from entity e2 where exists (select 1 from entity e1 where e1.db_id = ? and e1.source = e2.source and e1.source_id = e2.source_id) and type = ? group by e2.db_id");
				PreparedStatement count = con.prepareStatement("select count(*) from entity e where db_id = ?");
				PreparedStatement text = con.prepareStatement("select text from entity e where db_id = ? limit 1");
				BufferedWriter w = new BufferedWriter(
						new FileWriter("/Users/ajimeno/Documents/work/ai4eye/test/out.txt"))) {

			for (String[] disease : diseases) {
				for (String type : types) {
					terms.setString(1, disease[0]); // Disease id
					terms.setString(2, type); // Type search

					try (ResultSet rs = terms.executeQuery()) {
						while (rs.next()) {
							count.setString(1, rs.getString(2));
							try (ResultSet crs = count.executeQuery()) {
								while (crs.next()) {
									text.setString(1, rs.getString(2));
									try (ResultSet trs = text.executeQuery()) {
										while (trs.next()) {
											double log_prob = (Math.log(rs.getInt(1)) - Math.log(crs.getInt(1)));

											// if (log_prob > -0.5)
											{
												w.write(rs.getInt(1) + "|" + rs.getString(2) + "|" + crs.getInt(1) + "|"
														+ log_prob + "|" + trs.getString(1) + "|" + type + "|"
														+ disease[0] + "|" + disease[1]);
												w.newLine();
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}