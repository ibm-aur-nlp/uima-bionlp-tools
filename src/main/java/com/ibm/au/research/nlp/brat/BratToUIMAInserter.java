/* Copyright 2020 IBM

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.brat;

import java.util.LinkedList;
import java.util.stream.Collectors;

import org.apache.uima.jcas.JCas;
import org.cleartk.ne.type.NamedEntityMention;

import au.com.nicta.csp.brateval.Document;
import au.com.nicta.csp.brateval.Entity;
import au.com.nicta.csp.brateval.Location;

/**
 * Created by amack on 20/6/17.
 */
public class BratToUIMAInserter {
	private final boolean checkText;
	private static final BratToUIMAEntity DEFAULT_CONVERTER  = ((jcas, e, begin, end) -> {
		NamedEntityMention mention = new NamedEntityMention(jcas, begin, end);
		mention.setMentionId(e.getId());
		mention.setMentionType(e.getType());
		return mention;
	});

	private final BratToUIMAEntity converter;

	public BratToUIMAInserter(boolean checkText, BratToUIMAEntity converter) {
		this.checkText = checkText;
		this.converter = converter;
	}

	public BratToUIMAInserter(boolean checkText) {
		this(checkText, DEFAULT_CONVERTER);
	}

	public BratToUIMAInserter() {
		this(false, DEFAULT_CONVERTER);
	}
	
	public void insert(JCas jCas, Document d, Entity e, int offset) {
		final LinkedList<Location> locs = e.getLocations();
		final int begin = locs.stream().mapToInt(Location::getStart).min().getAsInt() + offset;
		final int end = locs.stream().mapToInt(Location::getEnd).max().getAsInt() + offset;
		if (checkText) {
			String targetText = locs.stream()
					.map(loc -> jCas.getDocumentText().substring(loc.getStart() + offset, loc.getEnd() + offset))
					.collect(Collectors.joining(" "));
			if (!targetText.equals(e.getString()))
				throw new BratAlignmentException(
						String.format("Text at (%d, %d) does not match Brat annotation %s", begin, end, e));
		}
		updateJcas(jCas, d, e, begin, end);
	}

	/** Stores the supplied Brat-derived entity in the CAS
	 * @param jCas The CAS to which the UIMA version of the entity should be added.
	 * @param d The Brat document from which the entity was derived
	 * @param e The entity which should be converted and stored
	 * @param begin The span start <strong>in UIMA coordinates</strong>
	 * @param end The span end <strong>in UIMA coordinates</strong>
	 */
	protected void updateJcas(JCas jCas, Document d, Entity e, int begin, int end) {
		jCas.addFsToIndexes(converter.convert(jCas, e, begin, end));
	}

	public void insert(JCas jCas, Document d, Entity e) {
		insert(jCas, d, e, 0);
	}
}
