package com.danrama.simpledbconnectionpool.impl;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import net.jcip.annotations.ThreadSafe;

import com.danrama.simpledbconnectionpool.ConnectionPool;

/**
 * A common starting point for connection pool implementations to inherit.  This abstract class holds on to the underlying
 * database driver and associated parameters.  It serves as a factory for creating database connections that will be used by
 * the subclasses.
 * 
 * @author Daniel Bloomfield Ramagem
 */
@ThreadSafe
public abstract class AbstractConnectionPool implements ConnectionPool {
	// database driver & parameters for connection
	private final Driver dbDriver;
	private final String dbConnUrl;
	private final Properties dbConnProps;
	
	/**
	 * Initialize the pool for a specific database.
	 * 
	 * @param dbDriver the database driver
	 * @param dbConnUrl optional database connection URL
	 * @param dbConnProps optional database connection properties
	 */
	public AbstractConnectionPool(Driver dbDriver, String dbConnUrl, Properties dbConnProps) {
		this.dbDriver = dbDriver;
		this.dbConnUrl = dbConnUrl;
		this.dbConnProps = dbConnProps;
	}

	/**
	 * Get a new raw database connection.
	 * 
	 * @return a database connection
	 * @throws SQLException
	 */
	protected Connection getNewRawDbConnection() throws SQLException {
		return dbDriver.connect(dbConnUrl, dbConnProps);
	}

	/**
	 * Get a new pooled database connection.
	 * 
	 * @return a raw database connection wrapped inside a pooled connection proxy class
	 * @throws SQLException
	 */
	protected PooledConnection getNewPooledConnection() throws SQLException {
		return PooledConnectionFactory.createPooledConnection(this, getNewRawDbConnection());
	}

	public abstract Connection getConnection() throws SQLException;

	public abstract void releaseConnection(Connection connection) throws SQLException;
}
