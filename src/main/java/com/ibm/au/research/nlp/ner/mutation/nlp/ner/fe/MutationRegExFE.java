package com.ibm.au.research.nlp.ner.mutation.nlp.ner.fe;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.function.FeatureFunction;

import com.ibm.au.research.nlp.ner.mutation.nlp.ner.RegExMutationType;

public class MutationRegExFE implements FeatureFunction {

	private final static String DEFAULT_NAME = "HGVS";

	private static final String AMINO_ACIDS = "((A|a)la|(A|a)rg|(A|a)sn|(A|a)sp|(C|c)ys|(G|g)lu|(G|g)ln|(G|g)ly|(H|h)is|(I|i)le|(L|l)eu|(L|l)ys|(M|m)et|(P|p)he|(P|p)ro|(S|s)er|(T|t)hr|(T|t)rp|(T|t)yr|(V|v)al|(C)|(D)|(E)|(Q)|(G)|(H)|(I)|(L)|(K)|(M)|(F)|(S)|(P)|(T)|(N)|(W)|(Y)|(R)|(V)|(A))";
	private static final String[][] expressions = {
			// DNA mutations
			{ "cDNA_mutation_substitution",
					"\\b(c|C|g|G)\\.( )?(-|\\*)?([1-9][0-9]*)((\\+|-|_)[1-9][0-9]*)?( )?[ACTGactg]( )?+(>|[0-9])( )?[ACTGactg]+(\\B|\\b)" },
			{ "cDNA_mutation_deletion",
					"\\b(c|C|g|G)\\.( )?((-|\\*)?[1-9][0-9]*)((-|\\+)[1-9][0-9]*)?(_((-|\\*)?[1-9][0-9]*((-|\\+)[1-9][0-9]*)?))?del([ACTGactg]*)?(\\B|\\b)" },
			{ "cDNA_mutation_duplication", "\\b(c|C|g|G)\\.([1-9][0-9]*)_([0-9]+)dup[ACTGactg]*(\\B|\\b)" },
			{ "cDNA_mutation_insertion", "\\b(c|C|g|G)\\.([1-9][0-9]*)_([1-9][0-9]*)ins[ACTGactg]+(\\B|\\b)" },
			{ "cDNA_mutation_deletion_insertion",
					"\\b(c|C|g|G)\\.([1-9][0-9]*)(\\_([1-9][0-9]*))?del[ACTGactg]*ins[ACTGactg]+(\\B|\\b)" },
			// Protein mutations
			{ "protein_mutation_substitution_missense",
					"\\b((P|p)\\.)?" + AMINO_ACIDS + "(\\()?([1-9][0-9]*)(\\))?" + AMINO_ACIDS + "(\\(-/-\\)|\\b)" },
			{ "protein_mutation_deletion",
					"\\b((P|p)\\.)?" + AMINO_ACIDS + "?([1-9][0-9]*)(_" + AMINO_ACIDS + "([1-9][0-9]*))?(D|d)(E|e)(L|l)"
							+ AMINO_ACIDS + "*\\b" },
			{ "protein_mutation_deletion_parenthesis",
					"\\b((P|p)\\.)?\\(" + AMINO_ACIDS + "?([1-9][0-9]*)(_" + AMINO_ACIDS
							+ "([1-9][0-9]*))?(D|d)(E|e)(L|l)" + AMINO_ACIDS + "*\\)(\\B|\\b)" },
			{ "protein_mutation_termination",
					"\\b((P|p)\\.)?" + AMINO_ACIDS + "([1-9][0-9]*)((T|t)(E|e)(R|r)|\\*|X)(\\B|\\b)" },
			{ "protein_mutation_termination_parenthesis",
					"\\b((P|p)\\.)?\\(" + AMINO_ACIDS + "([1-9][0-9]*)((T|t)(E|e)(R|r)|\\*|X)\\)(\\B|\\b)" },
			{ "protein_mutation_duplication",
					"\\b((P|p)\\.)?" + AMINO_ACIDS + "([1-9][0-9]*)_" + AMINO_ACIDS
							+ "([1-9][0-9]*)(D|d)(U|u)(P|p)(\\B|\\b)" },
			{ "protein_mutation_insertion",
					"\\b((P|p)\\.)?" + AMINO_ACIDS + "([1-9][0-9]*)_" + AMINO_ACIDS + "([1-9][0-9]*)(I|i)(N|n)(S|s)("
							+ AMINO_ACIDS + "+|[1-9][0-9]*)(\\B|\\b)" },
			{ "protein_mutation_insdel",
					"\\b((P|p)\\.)?" + AMINO_ACIDS + "([1-9][0-9]*)(_" + AMINO_ACIDS
							+ "([1-9][0-9]*))?(D|d)(E|e)(L|l)(I|i)(N|n)(S|s)(" + AMINO_ACIDS + "+)(\\B|\\b)" },
			{ "protein_mutation_insdel_parenthesis",
					"\\b((P|p)\\.)?\\(" + AMINO_ACIDS + "([1-9][0-9]*)(_" + AMINO_ACIDS
							+ "([1-9][0-9]*))?(D|d)(E|e)(L|l)(I|i)(N|n)(S|s)(" + AMINO_ACIDS + "+)\\)(\\B|\\b)" },
			{ "protein_mutation_frameshift",
					"\\b((P|p)\\.)?" + AMINO_ACIDS + "([1-9][0-9]*)(_" + AMINO_ACIDS
							+ "([1-9][0-9]*))?((D|d)(E|e)(L|l)(I|i)(N|n)(S|s))?(" + AMINO_ACIDS
							+ "+)?(fs)(((T|t)(E|e)(R|r)|\\*|X)[1-9][0-9]+)?(\\B|\\b)" },
			// RNA mutations
			{ "rna_mutation_substitution", "\\b(R|r)\\.([1-9][0-9]+)[acguACGU]+>[acguACGU]+(\\B|\\b)" },
			{ "rna_mutation_multiple_transcripts",
					"\\b(R|r)\\.\\[(=|[1-9][0-9]+[agcuAGCU]>[agcuAGCU])(,|;)( )*[1-9][0-9]+(_[1-9][0-9]+)?(del|ins)\\](\\B|\\b)" },
			{ "rna_mutation_unknown", "\\b(R|r)\\.(\\?|\\(\\?\\)|\\(=\\))(\\B|\\b)" },
			{ "rna_mutation_probable", "\\b(R|r)\\.\\([1-9][0-9]+[acguACGU]+>[acguACGU]+\\)(\\B|\\b)" },
			{ "rna_mutation_amount", "\\b(R|r)\\.0\\??(\\B|\\b)" },
			{ "rna_mutation_splicing", "\\b(R|r)\\.(spl\\?|\\(spl\\?\\))(\\B|\\b)" },
			{ "dbnsp_id", "\\brs[0-9]+(\\B|\\b)" },
			// knockout
			{ "knockout", "\\-/\\-" }, { "plus-minus", "\\+/\\-" },
			// translocation
			{ "translocation", "\\bt\\(\\d+(;\\d+)+\\)" },
			// inversion
			{ "inversion", "\\binv\\(\\d+\\)" },
			//
			{ "c_subs1", "\\b[ACTG](:[ACTG])? to [ACTG](:[ACTG])?( transversion)?(\\B|\\b)" },
			{ "c_subs2", "(\\b|\\bSNP|(\\b|-|\\+)?)([0-9]+)?( )?[ACTG]( )?>( )?[ACTG](\\b)" },
			{ "c_subs3",
					"(\\b((c|C)\\.( )?)?(-|\\+|\\*)?|((\b|-|\\+|\\*)))([1-9][0-9]*)((\\+|-|_)[1-9][0-9]*)?( )?[ACTGactg]( )?+(/|-->)( )?[ACTGactg]+(\\B|\\b)" },
			{ "p_ins1", "\\b" + AMINO_ACIDS + "([0-9]+)(I|i)(N|n)(S|s)" + AMINO_ACIDS + "(\\b|\\B)" },
			{ "c_del1", "\\b([0-9]+)del[ACTG](\\B|\\b)" }, { "dna_lesion", "\\b(8-oxoA.G|A.8-oxoG)(\\B|\\b)" },
			{ "iscn",
					"\\b([0-9][0-9]?,)?(XY|(i)?der\\((X|Y)\\))(((\\+|-)?([0-9]+|,|/| |i|\\(|\\)|p|q|;|t|der|\\?|(\\\\+|-)(X|Y)|(\\((X|Y)(;[0-9]+)?\\))+)))+([0-9](\\B|\\b)|\\))" },
			{ "snp_complex",
					"(\\b|(\\b|-|\\+)?)[0-9]+(((D|d)(E|e)(L|l))|((I|i)(N|n)(S|s)))?[ACTG](/(-|\\+)?[0-9]+(((D|d)(E|e)(L|l))|((I|i)(N|n)(S|s)))?[ACTG])+( polymorphism(s)?)?(\\B|\\b)" } };

	public static Map<String, RegExMutationType> getRegExMutationType() {
		Map<String, RegExMutationType> remt = new HashMap<String, RegExMutationType>();

		remt.put("cDNA_mutation_substitution", new RegExMutationType("cDNA_mutation_substitution", 4, 5, 4, 5));
		remt.put("c_subs2", new RegExMutationType("cDNA_mutation_substitution", 3, -1, 3, -1));
		remt.put("c_subs3", new RegExMutationType("cDNA_mutation_substitution", 8, 9, 8, 9));
		remt.put("cDNA_mutation_deletion", new RegExMutationType("cDNA_mutation_deletion", 3, -1, 8, -1));
		remt.put("c_del1", new RegExMutationType("cDNA_mutation_deletion", 1, -1, 1, -1));
		remt.put("cDNA_mutation_duplication", new RegExMutationType("cDNA_mutation_duplication", 2, -1, 3, -1));
		remt.put("cDNA_mutation_insertion", new RegExMutationType("cDNA_mutation_insertion", 2, -1, 3, -1));
		remt.put("cDNA_mutation_deletion_insertion",
				new RegExMutationType("cDNA_mutation_deletion_insertion", 2, -1, 4, -1));

		remt.put("protein_mutation_substitution_missense",
				new RegExMutationType("protein_mutation_substitution_missense", 45, -1, 45, -1));

		remt.put("protein_mutation_deletion", new RegExMutationType("protein_mutation_deletion", 44, -1, 87, -1));
		remt.put("protein_mutation_deletion_parenthesis",
				new RegExMutationType("protein_mutation_deletion", 44, -1, 87, -1));
		remt.put("protein_mutation_termination", new RegExMutationType("protein_mutation_termination", 44, -1, 44, -1));
		remt.put("protein_mutation_termination_parenthesis",
				new RegExMutationType("protein_mutation_termination", 44, -1, 44, -1));
		remt.put("protein_mutation_duplication", new RegExMutationType("protein_mutation_duplication", 44, -1, 86, -1));
		remt.put("protein_mutation_insertion", new RegExMutationType("protein_mutation_insertion", 44, -1, 86, -1));
		remt.put("protein_mutation_insdel", new RegExMutationType("protein_mutation_insdel", 44, -1, 87, -1));
		remt.put("protein_mutation_insdel_parenthesis",
				new RegExMutationType("protein_mutation_insdel", 44, -1, 87, -1));

		remt.put("protein_mutation_frameshift", new RegExMutationType("protein_mutation_frameshift", 44, -1, 44, -1));
		remt.put("p_ins1", new RegExMutationType("protein_mutation_insertion", 42, -1, 42, -1));

		return remt;
	}

	private Map<String, Pattern> patterns = null;

	public static Map<String, Pattern> getPatterns() {

		Map<String, Pattern> patterns = new HashMap<>();

		for (String[] expression : expressions) {
			patterns.put(expression[0], Pattern.compile(expression[1]));
		}

		return patterns;
	}

	public MutationRegExFE() {
		patterns = getPatterns();
	}

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

			for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
				Pattern p = entry.getValue();

				if (p.matcher(value).matches()) {
					return Collections.singletonList(new Feature(featureName, "HGVS"));

				}
			}
		}

		return Collections.emptyList();
	}
}