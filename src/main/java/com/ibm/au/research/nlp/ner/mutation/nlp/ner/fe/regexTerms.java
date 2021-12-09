package com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe;

import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.function.FeatureFunction;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Matthew Downton on 22/5/17.
 */
public class regexTerms implements FeatureFunction {
	public static final String DEFAULT_NAME = "regexTerm";

	static Pattern mutationTerms = Pattern
			.compile("([vV]ariants?|[vV]ariations?|[dD]elet(ions?|ed)|[lL]oss|[gG]ains?)");

	@Override
	public List<Feature> apply(Feature feature) {
		String featureName = Feature.createName(DEFAULT_NAME, feature.getName());
		Object featureValue = feature.getValue();
		if (featureValue == null)
			return Collections.emptyList();
		else if (featureValue instanceof String) {
			String value = featureValue.toString();
			if (value == null || value.length() == 0)
				return Collections.emptyList();
			if (mutationTerms.matcher(value).matches()) {
				return Collections.singletonList(new Feature(featureName, "1"));
			}
		}
		// return Collections.singletonList(new Feature(featureName,"0"));
		return Collections.emptyList();
	}
}
