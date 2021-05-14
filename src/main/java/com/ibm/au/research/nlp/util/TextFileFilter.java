package com.ibm.au.research.nlp.util;

import java.io.File;

import org.apache.commons.io.filefilter.IOFileFilter;

public final class TextFileFilter implements IOFileFilter {
	public boolean accept(File file) {
		return file.getPath().endsWith(".txt");
	}

	public boolean accept(File dir, String name) {
		return name.endsWith(".txt");
	}
}