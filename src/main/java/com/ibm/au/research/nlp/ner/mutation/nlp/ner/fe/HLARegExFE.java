package com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HLARegExFE {

	// Strings provided by Benjamin Goudey
	private static final String hlaRegEx = "HLA-(D[RQOMP][AB]|[A-Z])[0-9]*\\**([0-9]+)*(:[0-9]+)*[LSCAQN]*";
	private static final String hlaSeroRegEx = "HLA-(A|B|C|DR|DQ)[0-9]+(\\.[0-9]+)*";

	public static Map<String, Pattern> getPatterns() {
		Map<String, Pattern> patterns = new HashMap<>();
		patterns.put("HLA", Pattern.compile(hlaRegEx));
		patterns.put("HLASero", Pattern.compile(hlaSeroRegEx));
		return patterns;
	}
}