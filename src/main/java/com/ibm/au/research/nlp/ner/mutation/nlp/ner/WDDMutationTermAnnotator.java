package com.ibm.au.research.nlp.ner.mutation.nlp.ner;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

public class WDDMutationTermAnnotator extends JCasAnnotator_ImplBase {

	public final static String[][] termContexts = { { "LOH", "loss of heterozygosity" },
			{ "SNP(s)?", "polymorphism(s)?" }, { "MSI", "microsatellite instability" },
			{ "CNV(s)?", "copy number (variant|variation)(s)?" } };

	public final static String[][] terms = { { "LOH", "\\b(l|L)oss of heterozygosity(\\b|\\B)" },
			{ "ca", "\\b(c|C)hromosomal aberration(s)?(\\b|\\B)" },
			{ "nsnp", "\\b(n|N)on-synonymous polymorphism(s)?(\\b|\\B)" },
			{ "nssnp", "\\b(n|N)on-synonymous single nucleotide polymorphism(s)?" },
			{ "som", "\\b(s|S)omatic (alteration(s)?|change(s)?)(\\B|\\b)" },
			{ "msi", "\\b(m|M)icrosatellite (instabilit(y|ies)|unstable)(\\b|\\B)" },
			{ "tm", "\\b(t|T)runcating mutation(s)?(\\b|\\B)" }, { "a", "\\b(a|A)neuploidy(\\b|\\B)" },
			{ "nonsense", "\\b(n|N)onsense mutation(s)?(\\b|\\B)" },
			{ "snp", "\\b(s|S)ingle(-| )nucleotide polymorphism(s)?(\\b|\\B)" },
			{ "sbs", "\\b(s|S)ingle-base substitution(\\b|\\B)" }, { "ma", "\\bmutant allele(s)?(\\b|\\B)" },
			{ "mm", "\\b(m|M)issense mutation(s)?(\\b|\\B)" }, { "bm", "\\bbiallelic mutation(s)?(\\b|\\B)" },
			{ "ai", "\\b(a|A)llelic imbalance(s)?(\\b|\\B)" }, { "fm", "\\bframeshift mutation(s)?(\\b|\\B)" },
			{ "dnm", "\\b(d|D)ominant negative mutant(s)?(\\b|\\B)" },
			{ "cnv", "\\b(c|C)opy(-| )?number (amplification|variant|gain|alteration)(s)?(\\b|\\B)" },
			{ "bt", "\\b(b|B)alanced translocation(\\b|\\B)" }, { "acn", "\\b(a|A)berrant copy number(s)?(\\b|\\B)" },
			{ "hm", "\\b(h|H)ypermutability(\\b|\\B)" }, { "sbbm", "\\b(s|S)ingle base-base mismatch(es)?(\\b|\\B)" },
			{ "msh", "\\b(m|M)icrosatellite heteroduplex(es)?(\\b|\\B)" },
			{ "ga", "\\b(g|G)enomic (aberration|alteration)(s)?(\\b|\\B)" },
			{ "dnahete", "\\bDNA heterogeneity(\\b|\\B)" }, { "mutated", "\\b(m|M)utated(\\b|\\B)" },
			{ "mutant", "\\b(m|M)utant(\\b|\\B)" }, { "aneuploid", "\\b(a|A)neuploid(ies)?(\\b|\\B)" } };

	private Map<Pattern, Pattern> patterns;

	private Map<String, Pattern> termPatterns;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		patterns = new HashMap<>();

		for (String[] tc : termContexts) {
			patterns.put(Pattern.compile("(\\b)" + tc[0] + "(\\B|\\b)"),
					Pattern.compile("(\\b)" + tc[1] + "(\\B|\\b)"));
		}

		termPatterns = new HashMap<>();

		for (String[] expression : terms) {
			termPatterns.put(expression[0], Pattern.compile(expression[1]));
		}
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		for (Map.Entry<Pattern, Pattern> entry : patterns.entrySet()) {
			Pattern entity = entry.getKey();
			Pattern support = entry.getValue();

			if (support.matcher(jCas.getDocumentText()).find()) {
				Matcher m = entity.matcher(jCas.getDocumentText());

				while (m.find()) {
					NamedEntityMention ne = new NamedEntityMention(jCas, m.start(), m.end());
					ne.setMentionType("Mutation");
					jCas.addFsToIndexes(ne);
				}
			}
		}

		for (Map.Entry<String, Pattern> entry : termPatterns.entrySet()) {
			Pattern p = entry.getValue();

			Matcher m = p.matcher(jCas.getDocumentText());

			while (m.find()) {
				NamedEntityMention ne = new NamedEntityMention(jCas, m.start(), m.end());
				ne.setMentionType("Mutation");
				jCas.addFsToIndexes(ne);
			}
		}
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(WDDMutationTermAnnotator.class);
	}
}