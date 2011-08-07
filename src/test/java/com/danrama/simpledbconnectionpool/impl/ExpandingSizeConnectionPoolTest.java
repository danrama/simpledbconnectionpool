/*
Copyright 2011 Daniel Bloomfield Ramagem

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.danrama.simpledbconnectionpool.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.danrama.simpledbconnectionpool.exception.ConnectionPoolAlreadyReleasedConnectionException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolNewConnectionException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolOutOfConnectionsException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolReleaseConnectionException;
/**
 * Tests for verifying and validating the behavior of the ExpandingSizeConnectionPool class.
 * <p>
 * This class uses a mock object for the database driver and the database connections.  This way no real database is needed for
 * the testing.
 *  
 * @author Daniel Bloomfield Ramagem
 */
public class ExpandingSizeConnectionPoolTest {
	// the mock objects for database driver and connection
	private Connection mockConnection;
	private Driver mockDriver;
	private String dbConnUrl; // dummy param
	private Properties dbConnProps; // dumy param
	
	// connection pool params and reference to be used in each test
	private int poolMinSize;
	private int poolMaxSize;
	private ExpandingSizeConnectionPool connPool;

	@Before
	public void setUp() throws Exception {
		// create a mock Connection object that will always respond that it is valid
		int timeout = 0;
		mockConnection = createMock(Connection.class);
		expect(mockConnection.isValid(timeout)).andStubReturn(true);
		mockConnection.close();
		expectLastCall().asStub();
		replay(mockConnection);
		
		// create a mock Driver object that returns mock Connections
		String dbConnUrl = null;
		Properties dbConnProps = null;
		mockDriver = createMock(Driver.class);
		expect(mockDriver.connect(dbConnUrl, dbConnProps)).andStubReturn(mockConnection);
		replay(mockDriver);
	}

	@After
	public void tearDown() throws Exception {
		reset(mockConnection);
		reset(mockDriver);
	}

	/*
	 * Helper method to obtain an initial connection pool for tests.
	 */
	private void setupExpandingConnectionPool(int min, int max) throws Exception {
		poolMinSize = min;
		poolMaxSize = max;
		connPool = new ExpandingSizeConnectionPool(poolMinSize, poolMaxSize, mockDriver, dbConnUrl, dbConnProps);
	}
	
	@Test
	public void testPoolCreationWithBadParams() throws Exception {
		try {
			new ExpandingSizeConnectionPool(-1, 5, mockDriver, dbConnUrl, dbConnProps);
			fail("should not be able to create the pool with bad params");
		} catch (IllegalArgumentException e) {
			; // this is expected
		}
		
		try {
			new ExpandingSizeConnectionPool(1, -5, mockDriver, dbConnUrl, dbConnProps);
			fail("should not be able to create the pool with bad params");
		} catch (IllegalArgumentException e) {
			; // this is expected
		}
		
		try {
			new ExpandingSizeConnectionPool(1, 5, null, dbConnUrl, dbConnProps);
			fail("should not be able to create the pool with bad params");
		} catch (IllegalArgumentException e) {
			; // this is expected
		}
		
		try {
			new ExpandingSizeConnectionPool(5, 1, mockDriver, dbConnUrl, dbConnProps);
			fail("should not be able to create the pool with bad params");
		} catch (IllegalArgumentException e) {
			; // this is expected
		}
	}
	
	@Test
	public void testPoolCreationWithGoodParams() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		// some basic validation that the connection pool was initialized correctly
		assertEquals(poolMinSize, connPool.getPoolMinConnections());
		assertEquals(poolMaxSize, connPool.getPoolMaxConnections());
		assertEquals(poolMinSize, connPool.getNumConnectionsInPool());
		assertEquals(poolMinSize, connPool.getTotalConnections());
	}

	@Test
	public void testPoolOfFixedSizeBasicRetrievalAndRelease() throws Exception {
		int poolSize = 5;
		setupExpandingConnectionPool(poolSize, poolSize); // setup a new pool with (min, max) connections
		
		// retrieve a connection and verify that the pool is properly refilled to the minimum level and that the number of
		// connections obtained so far is also updated
		Connection conn = connPool.getConnection();
		assertEquals(poolSize - 1, connPool.getNumConnectionsInPool());
		assertEquals(poolSize, connPool.getTotalConnections());

		// return the connection to the pool and verify that the the pool continues to be filled with a minimum number of
		// connections and that the returned connection was discarded
		connPool.releaseConnection(conn);
		assertEquals(poolSize, connPool.getNumConnectionsInPool());
		assertEquals(poolSize, connPool.getTotalConnections());
	}

	@Test
	public void testPoolWithZeroMinimumSizeBasicRetrievalAndRelease() throws Exception {
		setupExpandingConnectionPool(0, 4); // setup a new pool with (min, max) connections
		
		// retrieve a connection and verify that the pool is properly refilled to the minimum level and that the number of
		// connections obtained so far is also updated
		Connection conn = connPool.getConnection();
		assertEquals(poolMinSize, connPool.getNumConnectionsInPool());
		assertEquals(poolMinSize + 1, connPool.getTotalConnections());
		
		// return the connection to the pool and verify that the the pool continues to be filled with a minimum number of
		// connections and that the returned connection was discarded
		connPool.releaseConnection(conn);
		assertEquals(poolMinSize, connPool.getNumConnectionsInPool());
		assertEquals(poolMinSize, connPool.getTotalConnections());
	}
	
	@Test
	public void testPoolBasicRetrievalAndRelease() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		// retrieve a connection and verify that the pool is properly refilled to the minimum level and that the number of
		// connections obtained so far is also updated
		Connection conn = connPool.getConnection();
		assertEquals(poolMinSize, connPool.getNumConnectionsInPool());
		assertEquals(poolMinSize + 1, connPool.getTotalConnections());

		// return the connection to the pool and verify that the the pool continues to be filled with a minimum number of
		// connections and that the returned connection was discarded
		connPool.releaseConnection(conn);
		assertEquals(poolMinSize, connPool.getNumConnectionsInPool());
		assertEquals(poolMinSize, connPool.getTotalConnections());
	}

	@Test
	public void testPoolUsageGetMaximumConnections() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		// obtain the maximum number of connections from the pool
		List<Connection> clients = new ArrayList<Connection>();
		for (int i=0; i < poolMaxSize; i++)
			clients.add(connPool.getConnection());
		
		// verify the internal pool state
		assertEquals(0, connPool.getNumConnectionsInPool());
		assertEquals(poolMaxSize, connPool.getTotalConnections());
		
		// now return all of the connections
		for (Connection conn : clients) {
			connPool.releaseConnection(conn);
		}

		// verify the internal pool state
		assertEquals(poolMinSize, connPool.getNumConnectionsInPool());
		assertEquals(poolMinSize, connPool.getTotalConnections());
	}
	
	@Test
	public void testPoolUsageExceedMaxConnections() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		// obtain the maximum number of connections from the pool
		List<Connection> clients = new ArrayList<Connection>();
		for (int i=0; i < poolMaxSize; i++)
			clients.add(connPool.getConnection());
		
		// try to obtain one more connection to exceed the maximum, verify that this is not allowed
		try {
			connPool.getConnection();
			fail("should not have been able to retrieve another connection");
		} catch (ConnectionPoolOutOfConnectionsException e) {
			; // do nothing, this is expected
		}
	}
	
	@Test
	public void testPoolUsageReleaseTheSameConnectionMoreThanOnce() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		// obtain a connection from the pool
		Connection conn = connPool.getConnection();
		
		// try to release more connections than were retrieved by releasing the connection twice
		connPool.releaseConnection(conn);
		try {
			connPool.releaseConnection(conn);
			fail("should not have gotten here");
		} catch (ConnectionPoolReleaseConnectionException e) {
			; // do nothing, this is expected
		}

		// verify the internal pool state
		assertEquals(poolMinSize, connPool.getNumConnectionsInPool());
		assertEquals(poolMinSize, connPool.getTotalConnections());
	}
	

	@Test
	public void testPoolUsageTryToReuseReleasedConnection() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		Connection conn = connPool.getConnection();
		connPool.releaseConnection(conn);
		
		try {
			conn.isValid(0);
			fail("should not have gotten here");
		} catch (ConnectionPoolAlreadyReleasedConnectionException e) {
			; // do nothing, this is expected
		}
	}

	@Test
	public void testPoolUsageReturnAClosedConnection() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		// modify the mock connection object to say that it is not valid
		reset(mockConnection);
		int timeout = 0;
		expect(mockConnection.isValid(timeout)).andStubReturn(false);
		mockConnection.close();
		expectLastCall().asStub();
		replay(mockConnection);
		
		// obtain enough connections to leave the pool with less than the minimum, forcing it to subsequently cache released
		// connections
		List<Connection> clients = new ArrayList<Connection>();
		for (int i=0; i < poolMaxSize; i++)
			clients.add(connPool.getConnection());

		// release one of the connections, which the mock Connection object will
		// say is not a valid connection
		connPool.releaseConnection(clients.get(0));

		// verify the internal pool state
		assertEquals(1, connPool.getNumConnectionsInPool());
		assertEquals(poolMaxSize, connPool.getTotalConnections());
	}
	
	@Test
	public void testPoolUsageSimulateGettingAConnectionWithSQLException() throws Exception {
		// we use a pool with caching set to zero on purpose here, since we want to test how the pool handles a database
		// driver get connection error
		setupExpandingConnectionPool(0, 4); // setup a new pool with (min, max) connections
		
		// modify the mock Driver object so that it throws an exception when a connection is attempted
		reset(mockDriver);
		expect(mockDriver.connect(dbConnUrl, dbConnProps)).andStubThrow(new SQLException("oh no there was a database error"));
		replay(mockDriver);
		
		// remember the pool state before the error
		int poolSize = connPool.getNumConnectionsInPool();
		int totalConnections = connPool.getTotalConnections();
		
		// get a connection from the pool
		try {
			connPool.getConnection();
			fail("should not have gotten this far");
		} catch (ConnectionPoolNewConnectionException e) {
			; // do nothing, this is expected
		}
		
		// verify the internal pool state is the same
		assertEquals(poolSize, connPool.getNumConnectionsInPool());
		assertEquals(totalConnections, connPool.getTotalConnections());
	}
	
	@Test
	public void testPoolUsageSimulateReturningAConnectionWithSQLException() throws Exception {
		setupExpandingConnectionPool(2, 4); // setup a new pool with (min, max) connections
		
		// modify the mock Connection object so that it throws an exception when it is validated
		reset(mockConnection);
		int timeout = 0;
		expect(mockConnection.isValid(timeout)).andStubThrow(new SQLException("oh no there was a database error"));
		mockConnection.close();
		expectLastCall().asStub();
		replay(mockConnection);
		
		// obtain enough connections to leave the pool with less than the minimum, forcing it to subsequently cache released
		// connections
		List<Connection> clients = new ArrayList<Connection>();
		for (int i=0; i < poolMaxSize; i++)
			clients.add(connPool.getConnection());

		// release one of the connections, which forces an SQLException from the mock Connection object
		try {
			connPool.releaseConnection(clients.get(0));
			fail("should not have gotten this far");
		} catch (ConnectionPoolReleaseConnectionException e) {
			; // do nothing, this is expected
		}

		// Verify the internal pool state: 1) the pool still doesn't have cached connections because the release failed; 2)
		// we consider the failed connection "lost" and no longer track it in the pool.
		assertEquals(0, connPool.getNumConnectionsInPool());
		assertEquals(poolMaxSize - 1, connPool.getTotalConnections());
	}
}
