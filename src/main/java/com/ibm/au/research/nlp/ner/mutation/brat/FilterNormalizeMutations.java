package com.ibm.au.research.nlp.ner.mutation.brat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.nicta.csp.brateval.Annotations;
import au.com.nicta.csp.brateval.Document;
import au.com.nicta.csp.brateval.Entity;

/**
 * Normalizes mutation types into mutation and removes all other entities It
 * removes the relations as well.
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@au1.ibm.com)
 *
 */
public class FilterNormalizeMutations {
	public static void filter(String folderName) throws IOException {
		File WDDFolder = new File(folderName);

		for (File WDDFile : WDDFolder.listFiles()) {

			if (WDDFile.getName().endsWith(".txt")) {
				File WDDAnnotation = new File(WDDFile.getAbsolutePath().replaceAll(".txt$", ".ann"));

				Document d = (WDDAnnotation.exists() ? Annotations.read(WDDAnnotation.getAbsolutePath(), ".ann")
						: new Document());

				List<Entity> removeList = new ArrayList<Entity>();

				for (Entity e : d.getEntities()) {
					if (e.getType().equalsIgnoreCase("dna_mutation") || e.getType().equalsIgnoreCase("protein_mutation")
							|| e.getType().equalsIgnoreCase("rna_mutation") || e.getType().equalsIgnoreCase("dbsnp")
							|| e.getType().equalsIgnoreCase("mutation")) {
						e.setType("Mutation");
					} else {
						removeList.add(e);
					}
				}

				for (Entity e : removeList) {
					d.removeEntity(e.getId());
				}

				d.getRelations().clear();

				d.getAttributes().clear();

				Annotations.write(WDDAnnotation.getAbsolutePath(), d);
			}
		}
	}
}