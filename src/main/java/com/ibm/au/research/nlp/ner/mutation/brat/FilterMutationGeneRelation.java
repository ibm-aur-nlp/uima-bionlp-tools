package com.ibm.au.research.nlp.ner.mutation.brat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.nicta.csp.brateval.Annotations;
import au.com.nicta.csp.brateval.Document;
import au.com.nicta.csp.brateval.Entity;
import au.com.nicta.csp.brateval.Relation;

/**
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@au1.ibm.com)
 *
 */
public class FilterMutationGeneRelation {
	public static void filter(String folderName) throws IOException {
		File WDDFolder = new File(folderName);

		for (File WDDFile : WDDFolder.listFiles()) {

			if (WDDFile.getName().endsWith(".txt")) {
				File WDDAnnotation = new File(WDDFile.getAbsolutePath().replaceAll(".txt$", ".ann"));

				Document d = (WDDAnnotation.exists() ? Annotations.read(WDDAnnotation.getAbsolutePath(), ".ann")
						: new Document());

				List<Entity> removeList = new ArrayList<>();

				for (Entity e : d.getEntities()) {
					if (e.getType().equalsIgnoreCase("dna_mutation") || e.getType().equalsIgnoreCase("protein_mutation")
							|| e.getType().equalsIgnoreCase("rna_mutation") || e.getType().equalsIgnoreCase("dbsnp")
							|| e.getType().equalsIgnoreCase("mutation")) {
						e.setType("Mutation");
					}
					// If no mutation or gene_protein, then remove
					else if (!e.getType().equals("Gene_protein")) {
						removeList.add(e);
					}
				}

				for (Entity e : removeList) {
					d.removeEntity(e.getId());
				}

				// Remove any relation that is not has mutation between gene and
				// mutation
				List<Relation> removeRelation = new ArrayList<>();

				for (Relation r : d.getRelations()) {
					if (!(r.getEntity1().getType().equals("Gene_protein")
							&& r.getEntity2().getType().equals("Mutation"))) {
						removeRelation.add(r);
					}
				}

				for (Relation r : removeRelation) {
					d.removeRelation(r.getId());
				}

				d.getAttributes().clear();

				Annotations.write(WDDAnnotation.getAbsolutePath(), d);
			}
		}
	}
}