/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ner.crfsuite.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.util.InputStreamHandler;
import org.cleartk.util.PlatformDetection;

/**
 * 
 * @author Antonio Jimeno Yepes
 *
 */
public class CrfSuiteWrapper {
	static Logger logger = UIMAFramework.getLogger(CrfSuiteWrapper.class);

	Path executable = null;

	private Path getCrfSuiteExecutable() {
		if (this.executable == null) {

			// Check for OS - then return executable
			PlatformDetection pd = new PlatformDetection();

			String jarExecutableName = null;

			if (pd.getOs().equals(PlatformDetection.OS_WINDOWS)) {
				jarExecutableName = "crfsuite/crfsuite-0.12_win32/crfsuite.exe";
			} else if (pd.getOs().equals(PlatformDetection.OS_LINUX)
					&& pd.getArch().equals(PlatformDetection.ARCH_X86_64)) {
				jarExecutableName = "crfsuite/crfsuite-0.12-x86_64/bin/crfsuite";
			} else if (pd.getOs().equals(PlatformDetection.OS_LINUX)
					&& pd.getArch().equals(PlatformDetection.ARCH_PPC)) {
				jarExecutableName = "crfsuite/linux_ppc64/bin/crfsuite";
			} else if (pd.getOs().equals(PlatformDetection.OS_OSX)
					&& pd.getArch().equals(PlatformDetection.ARCH_X86_64)) {
				jarExecutableName = "crfsuite/osx_x86_64/bin/crfsuite";
			}

			if (jarExecutableName == null) {
				throw new RuntimeException("Platform not supported for crfsuite");
			} else {
				// Copy executable to a temporary file
				try {
					File executableFile = File.createTempFile("crfsuite", "exe");
					executableFile.deleteOnExit();
					executableFile.setExecutable(true);

					try (InputStream in = CrfSuiteWrapper.class.getClassLoader().getResourceAsStream(jarExecutableName);
							OutputStream out = new FileOutputStream(executableFile)) {
						int n;
						byte[] buffer = new byte[1024];
						while ((n = in.read(buffer)) > -1) {
							out.write(buffer, 0, n);
						}
					}
					
					this.executable = executableFile.toPath();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return this.executable;
	}

	public void train(File dir, String... args) throws IOException, InterruptedException {
		String trainingDataFileName = new File(dir, CrfSuiteClassifierBuilder.TRAINING_DATA_FILE_NAME)
				.getAbsolutePath();
		String trainingModelFileName = new File(dir, CrfSuiteClassifierBuilder.TRAINING_MODEL_FILE_NAME)
				.getAbsolutePath();

		Path executable = getCrfSuiteExecutable();

		// crfsuite learn [args] -m model_file training_data

		final List<String> params = new ArrayList<>();
		params.addAll(Arrays.asList(executable.toString(), "learn"));
		params.addAll(Arrays.asList(args));
		params.addAll(Arrays.asList("-m", trainingModelFileName, trainingDataFileName));
		Process p = robustProcessStart(params.toArray(new String[0]));

		try (InputStream in = p.getInputStream(); InputStream err = p.getErrorStream()) {
			InputStreamHandler<List<String>> inList = InputStreamHandler.getInputStreamAsList(in);

			InputStreamHandler<List<String>> errList = InputStreamHandler.getInputStreamAsList(err);

			p.waitFor();

			if (p.exitValue() != 0)
			{ throw new RuntimeException("CRFSuite exit value: " + p.exitValue()); }

			inList.join();
			errList.join();

			logger.log(Level.INFO, inList.getBuffer().toString().replaceAll(",", "\n"));
			logger.log(Level.WARNING, errList.getBuffer().toString().replaceAll(",", "\n"));
		}
	}

	public List<String> classify(File taggingFile, File modelFile) throws IOException, InterruptedException {
		String taggingFileName = taggingFile.getAbsolutePath();
		String modelFileName = modelFile.getAbsolutePath();

		Path executable = getCrfSuiteExecutable();

		// crfsuite tag [args] -m model_file training_data

		Process p = robustProcessStart(executable.toString(), "tag", "-m", modelFileName, taggingFileName);
		List<String> outputTags;

		try (InputStream in = p.getInputStream(); InputStream err = p.getErrorStream()) {
			InputStreamHandler<List<String>> inList = InputStreamHandler.getInputStreamAsList(in);

			InputStreamHandler<List<String>> errList = InputStreamHandler.getInputStreamAsList(err);

			p.waitFor();

			if (p.exitValue() != 0)
			{ throw new RuntimeException("CRFSuite exit value: " + p.exitValue()); }

			inList.join();
			errList.join();

			outputTags = inList.getBuffer();

			if (outputTags.size() > 0) {
				if (outputTags.get(outputTags.size() - 1).trim().length() == 0) {
					outputTags.remove(outputTags.size() - 1);
				}
			}
		}

		return outputTags;
	}

	/** attempt to work around https://bugs.openjdk.java.net/browse/JDK-8068370 which means
	 * that sometimes execution fails on Linux when running in parallel
	 * @param params
	 * @return
	 * @throws IOException
	 */
	private Process robustProcessStart(String... params) throws IOException {
		int errorCount = 0;
		while (true) {
			try {
				return new ProcessBuilder().command(params).start();
			} catch (IOException e) {
				errorCount++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					throw new IOException(e1);
				}
				if (errorCount >= 4) // ugh, ok fine we'll give up after four tries
					throw e;
				logger.log(Level.SEVERE, "Got error invoking external executable; will retry: {0}", params[0]);
			}
		}
	}
}
