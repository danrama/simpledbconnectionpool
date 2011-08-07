package com.danrama.simpledbconnectionpool;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import net.jcip.annotations.ThreadSafe;

import com.danrama.simpledbconnectionpool.exception.ConnectionPoolException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolInitializationException;
import com.danrama.simpledbconnectionpool.impl.ExpandingSizeConnectionPool;

/**
 * Creates pre-configured instances of database connection pools.  It uses the <i>factory pattern</i> to hide from the client
 * the details of the creation.
 * <p>
 * Currently this class only returns the "expanding size" connection pool.
 * 
 * @author Daniel Bloomfield Ramagem
 * @see ExpandingSizeConnectionPool 
 */
@ThreadSafe
public class ConnectionPoolFactory {
	// database driver & parameters for connection
	private final String dbConnUrl;
	private final Properties dbConnProps;
	private final Driver dbDriver;
	
	/**
	 * Initialize the factory to create connection pools for a specific database.
	 * 
	 * @param dbDriverClassname Optional database driver classname.  If this is not provided then the client is responsible
	 *                          for loading the database class via JDBC.
	 * @param dbConnUrl optional database connection URL
	 * @param dbConnProps optional database connection properties
	 * @throws SQLException  
	 * @throws ClassNotFoundException if the database driver is not found 
	 */
	public ConnectionPoolFactory(String dbDriverClassname, String dbConnUrl, Properties dbConnProps) 
	throws SQLException, ClassNotFoundException {
		if (dbDriverClassname != null) {
			// this will load the driver and automatically register itself with JDBC
			Class.forName(dbDriverClassname);
		}
		
		this.dbConnUrl = dbConnUrl;
		this.dbConnProps = dbConnProps;
		this.dbDriver = DriverManager.getDriver(dbConnUrl);
	}
	
	/**
	 * Create a new connection pool.
	 * 
	 * @param poolParams Pool configuration parameters.  In the current implementation 
	 *                   it expects two int values: the pool minimum and maximum
	 *                   connections
	 * @return currently returns a new instance of <code>ExpandingSizeConnectionPool</code>
	 *         configured with the pool parameters passed in
	 * @throws ConnectionPoolInitializationException if an error occurs creating the pool
	 * @see ExpandingSizeConnectionPool
	 */
	public ConnectionPool createConnectionPool(Object... poolParams) throws ConnectionPoolException {
		int poolMinConnections = (Integer) poolParams[0];
		int poolMaxConnections = (Integer) poolParams[1];
		return new ExpandingSizeConnectionPool(poolMinConnections, poolMaxConnections, 
				dbDriver, dbConnUrl, dbConnProps);
	}
}
