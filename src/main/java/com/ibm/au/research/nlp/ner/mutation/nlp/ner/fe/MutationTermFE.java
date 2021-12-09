package com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.function.FeatureFunction;

import com.ibm.au.research.nlp.ner.mutation.nlp.ner.utils.MeSHMutationTerms;

public class MutationTermFE implements FeatureFunction {

	private static String DEFAULT_NAME = "MutationTerm";

	private static String[] terms = { "mutant", "mutation", "mutations", "deleted", "deletion", "inversion",
			"translocation", "imbalance", "SNP", "SNPs", "polymorphism", "polymorphisms", "allele", "imbalance",
			"instability", "microsatellite", "insertion", "substitution", "MSI", "variant", "variants", "loss",
			"variations", "variation", "aberration", "aberrations", "lack", "gain", "cnv", "cnvs", "aberrant", "copy",
			"number", "transversion", "transversions", "aneuploid", "nonaneuploid", "DNA", "heterogeneity", "ploidy",
			"msi", "variant", "aneuploidy", "mitotic", "aberrations", "improper", "alignment", "inactivated",
			"microsatellite", "unstable", "chromosome", "changes", "lacking", "chromosome", "loss", "defects", "loh",
			"loss", "heterozygosity", "missense", "change", "truncation", "truncated", "truncating", "defective",
			"hypermutability", "microsatellite", "stable", "single", "base", "substitution", "heteroduplexes" };

	private Set<String> termSet = new HashSet<>();

	public MutationTermFE()  {
		for (String term : terms) {
			termSet.add(term.toLowerCase());
		}
/*
		try {
			// Mutation
			termSet.addAll(MeSHMutationTerms.getTerms("D009154"));
			// SNP
			termSet.addAll(MeSHMutationTerms.getTerms("D011110"));

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		try (BufferedWriter w = new BufferedWriter(new FileWriter("mutation-terms.txt"))) {
			for (String term : termSet) {
				w.write(term);
				w.newLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public List<Feature> apply(Feature feature) {

		String featureName = Feature.createName(DEFAULT_NAME, feature.getName());
		Object featureValue = feature.getValue();
		if (featureValue == null) {
			return Collections.emptyList();
		} else if (featureValue instanceof String) {
			String value = featureValue.toString().toLowerCase();
			if (value == null || value.length() < 3)
				return Collections.emptyList();

			for (String term : termSet) {
				if (term.startsWith(value) || value.startsWith(term)) {
					return Collections.singletonList(new Feature(featureName, "Term"));
				}
			}
		}

		return Collections.emptyList();
	}
}