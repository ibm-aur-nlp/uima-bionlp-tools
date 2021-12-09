package com.ibm.au.research.nlp.ner.mutation.nlp.ner.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.configuration2.ex.ConfigurationException;

import com.ibm.au.research.nlp.util.DBConnection;

public class MeSHMutationTerms {

	private static Set<String> termSet = null;

	private static void getTerms(String cui, Set<String> termSet, PreparedStatement relationStmt,
			PreparedStatement conceptTermsStmt) throws SQLException {
		// Loop along the MeSH hierarchy
		relationStmt.setString(1, cui);
		ResultSet set = relationStmt.executeQuery();

		Set<String> cuis = new HashSet<String>();

		while (set.next()) {
			conceptTermsStmt.setString(1, set.getString(1));
			cuis.add(set.getString(1));

			ResultSet termRS = conceptTermsStmt.executeQuery();

			while (termRS.next()) {
				// System.out.println(termSet.getString(2));
				String term = termRS.getString(2);

				if (!term.contains(",")) {
					String[] terms = term.split(" ");

					for (String t : terms) {
						if (t.length() > 2)
							termSet.add(t.toLowerCase().replaceAll("\\(", "").replaceAll("\\)", ""));
					}
				}
			}

			termRS.close();
		}

		set.close();

		// Traverse the concept
		for (String cuiChild : cuis)
			getTerms(cuiChild, termSet, relationStmt, conceptTermsStmt);
	}

	public static Set<String> getTerms(String MeSHCode)
			throws ClassNotFoundException, NamingException, SQLException, ConfigurationException {
		// Lazy initialization of the term list
		if (termSet == null) {
			Connection con = DBConnection.getConnection();

			termSet = new HashSet<String>();

			PreparedStatement MeSHConceptStmt = con
					.prepareStatement("select cui from umls2015aa.MRCONSO where SAB = 'MSH' and code = ? ;");
			PreparedStatement relationStmt = con.prepareStatement(
					"select cui2 from umls2015aa.MRREL where cui1 = ? and SAB = 'MSH' and REL = 'CHD' ;");
			PreparedStatement conceptTermsStmt = con
					.prepareStatement("select cui, str from umls2015aa.MRCONSO where LAT = 'ENG' and cui = ? ;");

			MeSHConceptStmt.setString(1, MeSHCode);
			ResultSet MeSHConceptRS = MeSHConceptStmt.executeQuery();

			if (MeSHConceptRS.next())
				getTerms(MeSHConceptRS.getString(1), termSet, relationStmt, conceptTermsStmt);

			MeSHConceptStmt.close();
			relationStmt.close();
			conceptTermsStmt.close();
			con.close();
		}
		return termSet;
	}

	public static void main(String[] argc)
			throws ClassNotFoundException, NamingException, SQLException, ConfigurationException {
		// Mutation
		System.out.println(getTerms("D009154"));
		System.out.println(getTerms("D011110"));
	}
}