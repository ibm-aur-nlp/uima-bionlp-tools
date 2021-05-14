/* Copyright 2020 IBM

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.brat;

import java.io.File;
import java.nio.file.Path;

/**
 * Created by amack on 18/5/17.
 */
public class BratUtils {
	public static final String DOC_SUFFIX = ".txt"; // suffix of source files
	public static final String BRAT_ANN_SUFFIX = ".ann"; // suffix of BRAT-format standoff annotation files

	public static String annsFilenameFromText(String basename) {
		return replaceSuffix(basename, DOC_SUFFIX, BRAT_ANN_SUFFIX);
	}

	public static String textFilenameFromAnns(String basename) {
		return replaceSuffix(basename, BRAT_ANN_SUFFIX, DOC_SUFFIX);
	}

	public static boolean isAnnotationFile(String filename) {
		return filename.endsWith(BRAT_ANN_SUFFIX);
	}

	public static boolean isAnnotationFile(Path filepath) {
		return isAnnotationFile(filepath.toString());
	}

	private static String replaceSuffix(String basename, String origSuffix, String replacementSuffix) throws BRATFilenameMappingException {
		if (!basename.endsWith(origSuffix)) {
			throw new BRATFilenameMappingException(basename + " does not end with expected suffix " + origSuffix);
		}
		return basename.substring(0, basename.length() - origSuffix.length()) + replacementSuffix;
	}

	public static File annsFileFromTextFile(File sourceText) {
		String annsBasename = annsFilenameFromText(sourceText.getName());
		return new File(sourceText.getParentFile(), annsBasename);
	}

	public static class BRATFilenameMappingException extends RuntimeException {
		public BRATFilenameMappingException(String s) {
			super(s);
		}
	}
}