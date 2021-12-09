package com.ibm.au.research.nlp.ner.mutation.nlp.ner.mt;

import java.util.Map;
import java.util.regex.Pattern;

import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.MutationRegExFE;

/**
 * Given a string, provide the mutation type
 * 
 * @author antonio.jimeno@au1.ibm.com
 *
 */
public class MutationType {

	private static Map<String, Pattern> patterns = MutationRegExFE.getPatterns();

	public static String getMutationType(String string) {
		
		// Match a regular expression
		for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
			Pattern p = entry.getValue();

			if (p.matcher(string).matches()) {
				return entry.getKey();
			}
		}

		// Identify if protein-gene mutation? - we could normalize the mentions

		return null;
	}
}