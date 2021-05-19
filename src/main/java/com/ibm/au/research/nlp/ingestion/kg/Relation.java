/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.ingestion.kg;

class Relation {
	private String type;
	private String type1;
	private String CUI1;
	private String type2;
	private String CUI2;

	public Relation(String type, String type1, String CUI1, String type2, String CUI2) {
		this.type = type;
		this.type1 = type1;
		this.CUI1 = CUI1;
		this.type2 = type2;
		this.CUI2 = CUI2;
	}

	public String getType() {
		return type;
	}

	public String getType1() {
		return type1;
	}

	public String getCUI1() {
		return CUI1;
	}

	public String getType2() {
		return type1;
	}

	public String getCUI2() {
		return CUI1;
	}

	public String toString() {
		return type + "|" + type1 + "|" + CUI1 + "|" + type2 + "|" + CUI2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((CUI1 == null) ? 0 : CUI1.hashCode());
		result = prime * result + ((CUI2 == null) ? 0 : CUI2.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((type1 == null) ? 0 : type1.hashCode());
		result = prime * result + ((type2 == null) ? 0 : type2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Relation other = (Relation) obj;
		if (CUI1 == null) {
			if (other.CUI1 != null)
				return false;
		} else if (!CUI1.equals(other.CUI1))
			return false;
		if (CUI2 == null) {
			if (other.CUI2 != null)
				return false;
		} else if (!CUI2.equals(other.CUI2))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (type1 == null) {
			if (other.type1 != null)
				return false;
		} else if (!type1.equals(other.type1))
			return false;
		if (type2 == null) {
			if (other.type2 != null)
				return false;
		} else if (!type2.equals(other.type2))
			return false;
		return true;
	}
}