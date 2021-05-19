/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.er;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.ne.type.NamedEntity;
import org.cleartk.ne.type.NamedEntityMention;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MetaMapWebServiceAnnotator extends JCasAnnotator_ImplBase {
	// TODO: test the class in an annotation project
	static Logger logger = UIMAFramework.getLogger(MetaMapWebServiceAnnotator.class);

	public static String callMetaMap(String query) throws IOException {
		URL url = new URL("https://ii-public2.nlm.nih.gov/metamaplite/rest/annotate");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", "text/plain");
		con.setDoOutput(true);

		String jsonInputString = "inputtext=" + query + "&docformat=freetext&resultformat=json&resultformat=json";
		try (OutputStream os = con.getOutputStream()) {
			byte[] input = jsonInputString.getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			return response.toString();
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String text = jcas.getDocumentText();

		JSONParser parser = new JSONParser();

		try {
			JSONArray o = (JSONArray) parser.parse(callMetaMap(text));

			for (Object spanMatched : o) {
				JSONArray evlist = (JSONArray) ((JSONObject) spanMatched).get("evlist");

				for (Object an : evlist) {
					JSONObject jan = (JSONObject) an;

					int begin = ((Long) jan.get("start")).intValue();
					int end = begin + ((Long) jan.get("length")).intValue();

					JSONObject cinfo = (JSONObject) jan.get("conceptinfo");

					String cui = (String) cinfo.get("cui");

					JSONArray jsts = (JSONArray) cinfo.get("semantictypes");

					for (Object jst : jsts) {
						NamedEntityMention ne = new NamedEntityMention(jcas, begin, end);
						ne.setMentionType((String) jst);

						NamedEntity neId = new NamedEntity(jcas);
						neId.setEntityId(cui);
						ne.setMentionedEntity(neId);

						ne.addToIndexes(jcas);
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage());
		}
	}
}