package com.ibm.au.research.nlp.ner.crfsuite.wrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.encoder.features.BooleanEncoder;
import org.cleartk.ml.encoder.features.NameNumber;
import org.cleartk.ml.encoder.features.NameNumberFeaturesEncoder;
import org.cleartk.ml.encoder.features.NumberEncoder;
import org.cleartk.ml.encoder.features.StringEncoder;
import org.cleartk.ml.encoder.outcome.StringToStringOutcomeEncoder;
import org.cleartk.ml.jar.SequenceDataWriter_ImplBase;

/**
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@au1.ibm.com)
 *
 */
public class CrfSuiteDataWriter
		extends SequenceDataWriter_ImplBase<CrfSuiteClassifierBuilder, List<NameNumber>, String, String> {

	public CrfSuiteDataWriter(File outputDirectory) throws FileNotFoundException {
		super(outputDirectory);
		NameNumberFeaturesEncoder fe = new NameNumberFeaturesEncoder();
		fe.addEncoder(new NumberEncoder());
		fe.addEncoder(new BooleanEncoder());
		fe.addEncoder(new StringEncoder());
		this.setFeaturesEncoder(fe);
		this.setOutcomeEncoder(new StringToStringOutcomeEncoder());
	}

	@Override
	protected void writeEncoded(List<NameNumber> features, String outcome) throws CleartkProcessingException {
		this.trainingDataWriter.print(outcome);
		features.stream().forEach(f -> {
			this.trainingDataWriter.print("\t");
			this.trainingDataWriter.print(f.name);
		});
		this.trainingDataWriter.println();
	}

	@Override
	protected void writeEndSequence() {
		this.trainingDataWriter.println();
	}

	@Override
	protected CrfSuiteClassifierBuilder newClassifierBuilder() {
		return new CrfSuiteClassifierBuilder();
	}
}