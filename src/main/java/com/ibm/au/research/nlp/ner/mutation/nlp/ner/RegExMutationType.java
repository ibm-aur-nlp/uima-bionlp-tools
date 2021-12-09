package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

public class RegExMutationType {
	private String normalizedType;
	private int start;
	private int startPlus;
	private int end;
	private int endPlus;

	public String getNormalizedType() {
		return normalizedType;
	}

	public int getStart() {
		return start;
	}

	public int getStartPlus() {
		return startPlus;
	}

	public int getEnd() {
		return end;
	}

	public int getEndPlus() {
		return endPlus;
	}

	public RegExMutationType(String normalizedType, int start, int startPlus, int end, int endPlus) {
		this.normalizedType = normalizedType;
		this.start = start;
		this.startPlus = startPlus;
		this.end = end;
		this.endPlus = endPlus;
	}
}