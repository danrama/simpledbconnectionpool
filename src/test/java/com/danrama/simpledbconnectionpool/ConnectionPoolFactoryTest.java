package com.danrama.simpledbconnectionpool;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.util.Properties;

import org.junit.Test;

import com.danrama.simpledbconnectionpool.impl.ExpandingSizeConnectionPool;

/**
 * Tests the use of a real database driver (HSQLDB) for the connection pool.
 * 
 * @author Daniel Bloomfield Ramagem
 */
public class ConnectionPoolFactoryTest {

	@Test
	public void testCreateConnectionPoolWithRealDatabaseDriver() throws Exception {
		// setup the database connection parameters
		String driverClassname = "org.hsqldb.jdbcDriver";
		String url = "jdbc:hsqldb:mem:mydatabase;shutdown=true";
		Properties props = new Properties();
		props.setProperty("user", "SA");
		props.setProperty("password", "");

		// configure the ConnectionPoolFactory
		ConnectionPoolFactory poolFactory = new ConnectionPoolFactory(driverClassname, url, props);

		// create a new expanding pool with minimum size 5, maximum 10
		int min = 5;
		int max = 10;
		ExpandingSizeConnectionPool connPool = (ExpandingSizeConnectionPool) poolFactory.createConnectionPool(min, max);

		// use the pool
		Connection conn = connPool.getConnection();
		assertEquals(min, connPool.getNumConnectionsInPool());
		assertEquals(min + 1, connPool.getTotalConnections());

		// release the connection
		connPool.releaseConnection(conn);
		assertEquals(min, connPool.getNumConnectionsInPool());
		assertEquals(min, connPool.getTotalConnections());
	}
}
