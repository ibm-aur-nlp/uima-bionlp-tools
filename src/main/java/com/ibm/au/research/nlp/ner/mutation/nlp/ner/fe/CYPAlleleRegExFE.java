package com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CYPAlleleRegExFE {

	/*
	 * References: http://www.cypalleles.ki.se - The Human Cytochrome P450 (CYP)
	 * Allele Nomenclature Database https://www.snpedia.com - No information has
	 * been extracted from this database but it is good for checking SNPs
	 */
	private static String[][] expressions = { { "CYP_alleles",
			"\\b(?i)(por|cyp1A1|cyp1a2|cyp1b1|cyp2a6|cyp2a13|cyp2b6|cyp2c8|cyp2c9|cyp2c19|cyp2d6|cyp2e1|cyp2f1|cyp2j2|cyp2r1|cyp2s1|cyp2w1|cyp3a4|cyp3a5|cyp3a7|cyp3a43|cyp4a11|cypa422|cyp4b1|cyp4f2|cyp5a1|cyp8a1|cyp19a1|cyp21a2|cyp26a1|cyp17a1)(\\*[0-9a-zA-Z]+)((,| and) \\*[0-9a-zA-Z]+)*(\\b|\\B)" } };

	public static Map<String, Pattern> getPatterns() {

		Map<String, Pattern> patterns = new HashMap<>();

		for (String[] expression : expressions) {
			patterns.put(expression[0], Pattern.compile(expression[1]));
		}

		return patterns;
	}
}