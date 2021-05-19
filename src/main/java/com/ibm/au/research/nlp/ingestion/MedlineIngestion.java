/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ingestion;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.NamingException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;

import com.ibm.au.research.nlp.ingestion.uima.consumer.MySQLConsumer;
import com.ibm.au.research.nlp.ingestion.uima.reader.MedlineReader;
import com.ibm.au.research.nlp.util.ConfigurationFile;
import com.ibm.au.research.nlp.util.DBConnection;

public class MedlineIngestion implements Runnable {

	private static Stack<File> stack = new Stack<File>();

	private static synchronized File getNext() {
		if (stack.isEmpty())
			return null;
		return stack.pop();
	}

	public static Map<String, Integer> mapCanonical = Collections.synchronizedMap(new HashMap<>());

	@Override
	public void run() {
		File file = null;
		while ((file = getNext()) != null) {
			System.out.println("Indexing: " + file.getName());

			try {
				SimplePipeline.runPipeline(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()),
						// TODO: Annotators
						MySQLConsumer.getDescription()
				 //AnnotationPrint.getDescription()
				);
			} catch (ResourceInitializationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidXMLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UIMAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Finished: " + file.getName());
		}
	}

	private static void updateMySQLCanonical()
			throws ClassNotFoundException, NamingException, SQLException, ConfigurationException {
		Connection con = DBConnection.getConnection();
		PreparedStatement insertCanonical = con
				.prepareStatement("insert into canonical (canonical_id, canonical) values (?,?)");

		int count = 0;
		// Store canonical terms
		for (Map.Entry<String, Integer> entry : mapCanonical.entrySet()) {
			insertCanonical.setString(2, entry.getKey());
			insertCanonical.setInt(1, entry.getValue());

			insertCanonical.addBatch();

			count++;
			if (count == 1000) {
				insertCanonical.executeBatch();
				count = 0;
			}
		}

		if (count > 0) {
			insertCanonical.executeBatch();
		}

		insertCanonical.close();
		con.close();
	}

	public static void main(String[] argc)
			throws ConfigurationException, ClassNotFoundException, NamingException, SQLException {
		Configuration config = ConfigurationFile.configurationFactory();

		File folder = new File(argc[0]);
		int nThreads = Integer.parseInt(argc[1]);

		if (folder.isDirectory() && folder.listFiles() != null) {
			for (File file : folder.listFiles()) {
				if (file.getName().endsWith(".xml.gz")) {
					stack.push(file);
				}
			}
		}

		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		for (int i = 0; i < nThreads; i++) {
			Runnable worker = new MedlineIngestion();
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		System.out.println("Finished all threads");

		// Updating the canonical in the database
		updateMySQLCanonical();
	}
}