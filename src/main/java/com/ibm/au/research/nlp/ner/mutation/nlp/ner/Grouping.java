package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe.MutationRegExFE;

public class Grouping {

	private static void addType(String regEx, Matcher m, Map<String, RegExMutationType> remts) {
		RegExMutationType remt = remts.get(regEx);

		if (remt == null) {
			System.out.println("Type: " + regEx);
		} else {

			System.out.println(remt.getNormalizedType());
			System.out.println(remt.getStart());

			int start = Integer.parseInt(m.group(remt.getStart()))
					+ (remt.getStartPlus() <= 0 || m.group(remt.getStartPlus()) == null ? 0
							: Integer.parseInt(m.group(remt.getStartPlus())));

			System.out.println(start);

			int end = start;
			if (m.group(remt.getEnd()) != null) {
				end = Integer.parseInt(m.group(remt.getEnd()))
						+ (remt.getEndPlus() <= 0 || m.group(remt.getStartPlus()) == null ? 0
								: Integer.parseInt(m.group(remt.getEndPlus())));
			}

			System.out.println(end);
		}
	}

	public static Integer getNumber(String string) {
		try {
			return Integer.parseInt(string);
		} catch (Exception e) {
			return null;
		}
	}

	public static void main(String[] argc) {

		Map<String, Pattern> patterns = MutationRegExFE.getPatterns();
		Map<String, RegExMutationType> remt = MutationRegExFE.getRegExMutationType();

		String[][] mutations = {

				// DNA
				/*
				 * { "substitution", "c.123+12A>C" }, { "substitution", "123 A > C" }, {
				 * "substitution", "123+15 A --> C" }, { "deletion", "g.-123_127del" }, { "del",
				 * "12delT" }, { "duplication", "g.123_345dup" }, { "insertion",
				 * "g.123_124insAGC" }, { "inversion", "g.123_345inv" },
				 * 
				 * { "insdel", "g.123_127delinsAG" }
				 * 
				 * , // Protein
				 */
				{ "substitution",
						"Ile(143)Val" }/*
										 * , { "substitution", "V600E" },{ "substitution", "p.(Arg54Ser)" }, {
										 * "deletion", "p.(Cys76_Glu79del)" }, { "del", "12delA" }, { "deletion",
										 * "p.Cys76_Glu79del" }, { "duplication", "p.(Cys76_Glu79dup)" }, { "insertion",
										 * "p.(Lys23_Leu24insArgSerGln)" }, { "ins", " A12insAla " }, { "insdel",
										 * "p.(Arg123_Lys127delinsSerAsp)" }, { "insdel", "p.Arg123_Lys127delinsSerAsp"
										 * }, { "fs", "p.(Arg123LysfsTer34)" }, { "ter", "p.(A1234Ter)" }, {
										 * "substitution", "123 A > C" }
										 */ };

		for (String[] mu : mutations) {
			int maxLength = 0;

			Mutation mut = null;

			for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {

				Pattern p = entry.getValue();

				Matcher m = p.matcher(mu[1]);

				while (m.find()) {

					int l = m.end() - m.start();

					if (l > maxLength) {
						mut = new Mutation();
						mut.type = entry.getKey() + "|" + entry.getValue();
						mut.groups = new LinkedList<String>();
						for (int i = 0; i < m.groupCount(); i++) {
							mut.groups.add(m.group(i));
						}
						maxLength = l;
					}
					System.out.println("---");
					System.out.println(entry.getKey() + "|" + mu[1]);
					addType(entry.getKey(), m, remt);
				}
			}

			if (mut != null) {
				System.out.println(mu[0]);
				System.out.println(mut.type);
				for (int i = 0; i < mut.groups.size(); i++) {
					System.out.println(i + "|" + mut.groups.get(i));

				}
			}

		}
	}
}