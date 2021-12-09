package com.ibm.au.research.nlp.ner.mutation.brat;

import java.io.File;
import java.io.IOException;

import au.com.nicta.csp.brateval.Annotations;
import au.com.nicta.csp.brateval.Document;

public class FilterRemoveRelations {
	public static void filter(String folderName) throws IOException {
		File WDDFolder = new File(folderName);

		for (File WDDFile : WDDFolder.listFiles()) {

			if (WDDFile.getName().endsWith(".txt")) {
				File WDDAnnotation = new File(WDDFile.getAbsolutePath().replaceAll(".txt$", ".ann"));

				Document d = (WDDAnnotation.exists() ? Annotations.read(WDDAnnotation.getAbsolutePath(), ".ann")
						: new Document());

				d.getRelations().clear();

				Annotations.write(WDDAnnotation.getAbsolutePath(), d);
			}
		}
	}
}