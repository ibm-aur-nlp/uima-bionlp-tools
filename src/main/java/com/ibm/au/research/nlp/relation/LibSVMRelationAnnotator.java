/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.relation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.syntax.dependency.type.DependencyNode;
import org.cleartk.syntax.dependency.type.TopDependencyNode;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;

import com.ibm.au.research.nlp.types.Relation;

public class LibSVMRelationAnnotator extends CleartkAnnotator<String> {
	private static final String PARAM_RELATION_TYPE = "relation_type";
	private static final String PARAM_RELATION_ARG1 = "relation_arg1";
	private static final String PARAM_RELATION_ARG2 = "relation_arg2";
	private static final String PARAM_SENTENCE_BOUNDARY = "sentence_boundary";

	private String relation_type = null;
	private String relation_arg1 = null;
	private String relation_arg2 = null;

	private boolean sentenceBoundary = true;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		// Define the relation to be extracted
		// relation_type, arg1_type, arg2_type
		relation_type = (String) context.getConfigParameterValue(PARAM_RELATION_TYPE);
		relation_arg1 = (String) context.getConfigParameterValue(PARAM_RELATION_ARG1);
		relation_arg2 = (String) context.getConfigParameterValue(PARAM_RELATION_ARG2);

		sentenceBoundary = (Boolean) context.getConfigParameterValue(PARAM_SENTENCE_BOUNDARY);
	}

	private boolean inSentenceCheck(Sentence sentence, NamedEntityMention ne) {
		if (sentenceBoundary) {
			return sentence.getBegin() <= ne.getBegin() && ne.getEnd() <= sentence.getEnd();
		}

		return true;
	}

	private List<Feature> getTokenDistance(JCas jCas, NamedEntityMention ne1, NamedEntityMention ne2) {
		return Collections
				.singletonList(new Feature("token_distance", JCasUtil.selectBetween(Token.class, ne1, ne2).size()));
	}

	private int searchPathLength(DependencyNode n, List<DependencyNode> n2s) {
		if (n2s.contains(n)) {
			return 0;
		} else {
			for (int i = 0; i < n.getChildRelations().size(); i++) {
				int value = searchPathLength(n.getChildRelations(i).getChild(), n2s);

				if (value != Integer.MAX_VALUE) {
					return value + 1;
				}
			}
		}

		return Integer.MAX_VALUE;
	}

	private int searchPathLength(List<DependencyNode> n1s, List<DependencyNode> n2s) {
		int current = Integer.MAX_VALUE;

		for (DependencyNode n1 : n1s) {
			int value = searchPathLength(n1, n2s);

			if (value < current) {
				current = value;
			}
		}

		return (current == Integer.MAX_VALUE ? -1 : current);
	}

	private List<Feature> searchPathRelations(DependencyNode n, List<DependencyNode> n2s) {
		if (n2s.contains(n)) {
			return new ArrayList<Feature>();
		} else {
			for (int i = 0; i < n.getChildRelations().size(); i++) {
				List<Feature> value = searchPathRelations(n.getChildRelations(i).getChild(), n2s);

				if (value != null) {
					value.add(new Feature(n.getChildRelations(i).getRelation(), "1"));
					return value;
				}
			}
		}

		return null;
	}

	private List<Feature> searchPathRelations(List<DependencyNode> n1s, List<DependencyNode> n2s) {
		List<Feature> current = new ArrayList<Feature>();

		for (DependencyNode n1 : n1s) {
			List<Feature> value = searchPathRelations(n1, n2s);

			if (value != null) {
				current.addAll(value);
			}
		}

		return current;
	}

	private List<Feature> getGraphRelations(JCas jCas, NamedEntityMention ne1, NamedEntityMention ne2) {
		List<DependencyNode> n1s = JCasUtil.selectCovered(jCas, DependencyNode.class, ne1.getBegin(), ne1.getEnd());
		List<DependencyNode> n2s = JCasUtil.selectCovered(jCas, DependencyNode.class, ne2.getBegin(), ne2.getEnd());

		List<Feature> features = new ArrayList<Feature>();

		for (Feature feature : searchPathRelations(n1s, n2s)) {
			features.add(new Feature("n1-n2-" + feature.getName(), feature.getValue()));
		}

		for (Feature feature : searchPathRelations(n2s, n1s)) {
			features.add(new Feature("n2-n1-" + feature.getName(), feature.getValue()));
		}

		return features;
	}

	private List<Feature> getGraphDistance(JCas jCas, NamedEntityMention ne1, NamedEntityMention ne2) {
		List<DependencyNode> n1s = JCasUtil.selectCovered(jCas, DependencyNode.class, ne1.getBegin(), ne1.getEnd());
		List<DependencyNode> n2s = JCasUtil.selectCovered(jCas, DependencyNode.class, ne2.getBegin(), ne2.getEnd());

		List<Feature> features = new ArrayList<Feature>();

		features.add(new Feature("n1-n2", searchPathLength(n1s, n2s)));
		features.add(new Feature("n2-n1", searchPathLength(n2s, n1s)));

		return features;
	}

	private List<Feature> getTokensInEntity(JCas jCas, NamedEntityMention en) {
		return JCasUtil.selectCovered(jCas, Token.class, en).stream()
				.map((Token token) -> new Feature("tokenInEntity" + en.getMentionType(), token.getCoveredText()))
				.collect(Collectors.toList());
	}

	private List<Feature> getTokensBetweenEntities(JCas jCas, NamedEntityMention en1, NamedEntityMention en2) {
		return JCasUtil.selectBetween(jCas, Token.class, en1, en2).stream()
				.map((Token token) -> new Feature("tokenBetweenEntities", token.getCoveredText()))
				.collect(Collectors.toList());
	}

	/*** Identify connecting word ***/
	private static Token getDependencyToken(JCas jcas, DependencyNode node) {
		return JCasUtil.selectCovered(jcas, Token.class, node).get(0);
	}

	private static DependencyNode getVerb(JCas jcas, DependencyNode node) {
		DependencyNode current = node;

		// We assume that it is a tree and head relation size is always 1
		while (current.getHeadRelations().size() > 0) {
			if (current.getHeadRelations().size() > 1) {
				System.err.println(current);
			}

			Token token = getDependencyToken(jcas, current.getHeadRelations(0).getHead());

			if (token.getPos().startsWith("VB")) {
				return current.getHeadRelations(0).getHead();
			}

			current = current.getHeadRelations(0).getHead();
		}

		return null;
	}

	private static DependencyNode getPrep(JCas jcas, DependencyNode node) {
		DependencyNode current = node;

		// We assume that it is a tree and head relation size is always 1
		while (current.getHeadRelations().size() > 0) {
			if (current.getHeadRelations().size() > 1) {
				System.err.println(current);
			}

			if (current.getHeadRelations(0).getRelation().startsWith("prep")) {
				return current;
			}

			current = current.getHeadRelations(0).getHead();
		}

		return null;
	}

	private static int getDepth(DependencyNode node, TopDependencyNode top, int depth) {
		if (node == top) {
			return depth;
		} else {
			if (node.getHeadRelations().size() > 0)
				return getDepth(node.getHeadRelations(0).getHead(), top, depth + 1);
			else
				return depth;
		}
	}

	private static int getDepth(DependencyNode node, TopDependencyNode top) {
		return getDepth(node, top, 0);
	}

	private DependencyNode getConnector(JCas jCas, Sentence sentence, NamedEntityMention ne1, NamedEntityMention ne2) {
		System.out.println(sentence.getCoveredText());
		System.out.println("T|" + ne1.getCoveredText());

		TopDependencyNode top = JCasUtil.selectCovering(jCas, TopDependencyNode.class, sentence)
				.toArray(new TopDependencyNode[0])[0];

		DependencyNode v1 = getVerb(jCas, JCasUtil.selectCovered(jCas, DependencyNode.class, ne1).get(0));

		List<DependencyNode> verbs = new ArrayList<DependencyNode>();

		if (v1 == null) {
			System.out.println("V|null");
		} else {
			System.out.println("V|" + v1.getCoveredText());
			if (v1 != top) {
				verbs.add(v1);
			}
		}

		System.out.println("T|" + ne2.getCoveredText());

		List<DependencyNode> l2 = JCasUtil.selectCovered(jCas, DependencyNode.class, ne2);
		DependencyNode v2 = null;

		if (l2 != null && l2.size() > 0) {
			v2 = getVerb(jCas, l2.get(0));
		}

		if (v2 == null) {
			System.out.println("V|null");
		} else {
			System.out.println("V|" + v2.getCoveredText());
			if (v2 != top) {
				verbs.add(v2);
			}
		}

		if (verbs.size() > 0) {
			if (verbs.size() == 1) {
				return verbs.get(0);
			} else {
				if (verbs.size() == 2) {
					if (verbs.get(0) == verbs.get(1)) {
						return verbs.get(0);
					} else {
						int depth1 = getDepth(verbs.get(0), top);
						int depth2 = getDepth(verbs.get(1), top);
						return (depth1 > depth2 ? verbs.get(0) : verbs.get(1));
					}
				}
			}
		}
		// Look for prepositions
		else {
			DependencyNode p1 = getPrep(jCas, JCasUtil.selectCovered(jCas, DependencyNode.class, ne1).get(0));
			DependencyNode p2 = getPrep(jCas, JCasUtil.selectCovered(jCas, DependencyNode.class, ne2).get(0));

			List<DependencyNode> preps = new ArrayList<DependencyNode>();

			if (p1 != null && p1 != top) {
				preps.add(p1);
			}

			if (p2 != null && p2 != top) {
				preps.add(p2);
			}

			if (preps.size() == 2) {
				int depth1 = getDepth(preps.get(0), top);
				int depth2 = getDepth(preps.get(1), top);

				return (depth1 > depth2 ? preps.get(0) : preps.get(1));
			} else if (preps.size() == 1) {
				return preps.get(0);
			}
		}

		return null;
	}

	/*** End identify connecting word ***/

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		// Identify sentences
		for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
			// System.out.println(sentence.getCoveredText());
			/*
			 * for (DependencyNode de : JCasUtil.selectCovered(jCas, DependencyNode.class,
			 * sentence)) { System.out.println(de); }
			 */

			// Identify the entities in the sentence
			for (NamedEntityMention ne_arg1 : JCasUtil.selectCovered(jCas, NamedEntityMention.class, sentence)) {
				// Build the relation feature vectors, for the given relation
				// types
				// for (NamedEntityMention ne_arg2 :
				// JCasUtil.selectCovered(jCas, NamedEntityMention.class,
				// sentence))
				for (NamedEntityMention ne_arg2 : JCasUtil.select(jCas, NamedEntityMention.class)) {
					if ((ne_arg1 != ne_arg2 && ne_arg1.getMentionType().equals(relation_arg1)
							&& ne_arg2.getMentionType().equals(relation_arg2)) && inSentenceCheck(sentence, ne_arg2)) {
						List<Feature> features = new ArrayList<Feature>();
						features.addAll(getTokenDistance(jCas, ne_arg1, ne_arg2));
						// features.addAll(getGraphDistance(jCas, ne_arg1, ne_arg2));
						// features.addAll(getGraphRelations(jCas, ne_arg1, ne_arg2));
						features.addAll(getTokensInEntity(jCas, ne_arg1));
						features.addAll(getTokensInEntity(jCas, ne_arg2));
						features.addAll(getTokensBetweenEntities(jCas, ne_arg1, ne_arg2));
						features.addAll(getTokensBetweenEntities(jCas, ne_arg2, ne_arg1));
						// System.out.println(Arrays.toString(features.toArray(new
						// Feature[0])));

						if (isTraining()) {
							// If positive for relation, look into the relations
							// in the benchmark
							// write the features and outcomes as training
							// instances
							boolean hasRelation = false;

							for (Relation relation : JCasUtil.select(jCas, Relation.class)) {
								if (ne_arg1 == relation.getArg1() && ne_arg2 == relation.getArg2()) {
									hasRelation = true;
									break;
								}
							}

							if (hasRelation) {
								this.dataWriter.write(new Instance<String>("1", features));

								/*
								 * System.out.println("===" + sentence.getCoveredText());
								 * System.out.println("D|" + ne_arg1.getCoveredText()); System.out.println("M|"
								 * + ne_arg2.getCoveredText()); System.out.println(getConnector(jCas, sentence,
								 * ne_arg1, ne_arg2).getCoveredText());
								 */
							} else {
								this.dataWriter.write(new Instance<String>("-1", features));
							}
						} else {
							// Run trained model on data
							if (this.classifier.classify(features).equals("1")) {
								Relation relation = new Relation(jCas);

								relation.setRelationId("");
								relation.setRelationType(relation_type);
								relation.setArg1(ne_arg1);
								relation.setArg2(ne_arg2);

								relation.addToIndexes(jCas);
							}
						}
					}
				}
			}
		}
	}

	public static AnalysisEngineDescription getClassifierDescription(String modelFileName, String relation_type,
			String relation_arg1, String relation_arg2, boolean sentenceBoundary)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(LibSVMRelationAnnotator.class,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelFileName, PARAM_RELATION_TYPE,
				relation_type, PARAM_RELATION_ARG1, relation_arg1, PARAM_RELATION_ARG2, relation_arg2,
				PARAM_SENTENCE_BOUNDARY, sentenceBoundary);
	}

	public static AnalysisEngineDescription getWriterDescription(String outputDirectory, String relation_type,
			String relation_arg1, String relation_arg2, boolean sentenceBoundary)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(LibSVMRelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING, true, DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory, DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				LibSvmStringOutcomeDataWriter.class, PARAM_RELATION_TYPE, relation_type, PARAM_RELATION_ARG1,
				relation_arg1, PARAM_RELATION_ARG2, relation_arg2, PARAM_SENTENCE_BOUNDARY, sentenceBoundary);
	}
}