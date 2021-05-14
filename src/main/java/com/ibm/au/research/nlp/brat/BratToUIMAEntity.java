/* Copyright 2020 IBM

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.brat;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import au.com.nicta.csp.brateval.Entity;

/**
 * Created by amack on 28/6/17.
 */
public interface BratToUIMAEntity {
	Annotation convert(JCas jcas, Entity e, int begin, int end);
}
