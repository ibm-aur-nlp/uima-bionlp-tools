package com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe;

import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.function.FeatureFunction;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Matthew Downton on 19/5/17.
 */
public class mutateStringFeatureFunction implements FeatureFunction {
	public static final String DEFAULT_NAME = "mutateString";

	static Pattern mutationPattern = Pattern.compile("([mM]utat(e|ed|ions?)|[mM]utants?)");

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
			if (mutationPattern.matcher(value).matches()) {
				return Collections.singletonList(new Feature(featureName, "1"));
			}
		}
		return Collections.singletonList(new Feature(featureName, "0"));
	}
}
