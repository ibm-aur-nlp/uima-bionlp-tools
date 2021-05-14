/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
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