/* Copyright 2020 IBM
 Author: Antonio Jimeno Yepes antonio.jimeno@gmail.com

 This is free software; you can redistribute it and/or modify
 it under the terms of the Apache 2.0 License.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Apache 2.0 License for more details.*/
package com.ibm.au.research.nlp.util;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;

//import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;

public class DBConnection {
	private static DataSource ds = null;

	public static Connection getConnection()
			throws NamingException, SQLException, ClassNotFoundException, ConfigurationException {
		Configuration config = ConfigurationFile.configurationFactory();
		return getConnection(config.getString("db.host"), config.getString("db.user"), config.getString("db.password"));
	}

	public static Connection getConnection(String host, String user, String password)
			throws NamingException, SQLException, ClassNotFoundException, ConfigurationException {
		// Lazy initialization of the connection pooling
		if (ds == null) {
			MysqlDataSource mysqlDS = new MysqlDataSource();

			mysqlDS.setURL(host);
			mysqlDS.setUser(user);
			mysqlDS.setPassword(password);

			mysqlDS.setAutoReconnect(true);
			mysqlDS.setFailOverReadOnly(false);
			mysqlDS.setMaxReconnects(100);

			ds = (DataSource) mysqlDS;
		}

		return ds.getConnection();
	}
}