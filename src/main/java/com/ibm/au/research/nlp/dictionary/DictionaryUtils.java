package com.ibm.au.research.nlp.dictionary;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

public class DictionaryUtils {
	public static Set<String> getStopwords(String resourceFileName) throws FileNotFoundException, IOException {
		BufferedReader b = new BufferedReader(
				new InputStreamReader(DictionaryUtils.class.getClassLoader().getResourceAsStream(resourceFileName)));

		return b.lines().filter(e -> e.length() == 0).map(String::trim).map(String::toLowerCase)
				.collect(Collectors.toSet());
	}
}