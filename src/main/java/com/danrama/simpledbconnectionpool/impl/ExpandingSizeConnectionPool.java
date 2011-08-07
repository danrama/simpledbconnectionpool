package com.danrama.simpledbconnectionpool.impl;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;

import com.danrama.simpledbconnectionpool.exception.ConnectionPoolException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolInitializationException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolNewConnectionException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolOutOfConnectionsException;
import com.danrama.simpledbconnectionpool.exception.ConnectionPoolReleaseConnectionException;

/**
 * A connection pool implementation that expands, up to a maximum, as connections are obtained and contracts,
 * down to a minimum, as they are returned.
 * 
 * @author Daniel Bloomfield Ramagem
 */
@ThreadSafe
public class ExpandingSizeConnectionPool extends AbstractConnectionPool {
	private static final Logger logger = Logger.getLogger(ExpandingSizeConnectionPool.class);
	
	// a cache of database connections that are available for clients
	@GuardedBy("this") private final List<PooledConnection> pool;
	
	// Low-water mark for the pool.  If we haven't exceeded the maximum number of connections in the pool, then we should
	// refill the pool to this minimum level of connections.
	private final int poolMinConnections;
	
	// High-water mark for the pool.  This is the maximum number of connections that can ever be outstanding.
	private final int poolMaxConnections;
	
	// keep track of the number of all connections obtained from the database so far
	@GuardedBy("this") private int totalConnections;

	// ----- getters for the pool state variables, useful for tests -----
	public int getPoolMinConnections() {
		return poolMinConnections;
	}
	public int getPoolMaxConnections() {
		return poolMaxConnections;
	}
	public int getTotalConnections() {
		return totalConnections;
	}
	// Note that we are careful here not to "leak" the pool reference to an external client.  All that is accessible should
	// be the connection count for the pool, not the pool itself.
	public synchronized int getNumConnectionsInPool() {
		return pool.size();
	}

	/**
	 * Create a database connection pool that will expand in size up to a fixed maximum, and will contract as connections are
     * returned down to a fixed minimum.
	 * 
	 * @param poolMinConnections the minimum pool size
	 * @param poolMaxConnections the maximum pool size
	 * @param dbConnUrl database connection URL
	 * @param dbConnProps database connection properties
	 * @throws ConnectionPoolInitializationException
	 */
	public ExpandingSizeConnectionPool(int poolMinConnections, int poolMaxConnections, Driver dbDriver, 
			String dbConnUrl, Properties dbConnProps) throws ConnectionPoolInitializationException {
		super(dbDriver, dbConnUrl, dbConnProps);
		
		// check that valid params were passed
		if (poolMinConnections < 0)
			throw new IllegalArgumentException("pool minimum connections must be > 0");
		if (poolMaxConnections < 0)
			throw new IllegalArgumentException("pool maximum connections must be > 0");
		if (poolMaxConnections < poolMinConnections)
			throw new IllegalArgumentException("pool maximum must be >= to the minimum connections");
		if (dbDriver == null)
			throw new IllegalArgumentException("pool database Driver must be supplied");
		
		// initialize the pool 
		pool = new ArrayList<PooledConnection>();
		this.poolMinConnections = poolMinConnections;
		this.poolMaxConnections = poolMaxConnections;
		totalConnections = 0;

		// fill the pool up to the minimum level of connections
		try {
			refillPoolToMinimumLevel();
		} catch (SQLException e) {
			throw new ConnectionPoolInitializationException("pool initialization failed", e);
		}
		
		logger.info("created expanding pool(min:" + poolMinConnections + ", max:" + poolMaxConnections + ")");
	}

	/*
	 * Log with debug level a brief summary of the state of the pool.  
	 */
	private void logDebugPoolStatusMessage() {
		logger.debug("pool state: max=" + poolMaxConnections + ", total=" + totalConnections + 
				", cached=" + pool.size());
	}
	
	/*
	 * Refills the pool to the minimum level, but only if the maximum number of connections hasn't been already dispensed.
	 * 
	 * @throws SQLException is a database error occurs
	 */
	private void refillPoolToMinimumLevel() throws SQLException {
		boolean refillNeeded = totalConnections < poolMaxConnections && pool.size() < poolMinConnections;
		if (refillNeeded)
			logger.debug("refilling pool");
		
		// fill the pool with more connections if possible and needed
		while (totalConnections < poolMaxConnections && pool.size() < poolMinConnections) {
			PooledConnection conn = getNewPooledConnection();
			pool.add(conn);
			totalConnections++;
		}
		
		if (refillNeeded)
			logDebugPoolStatusMessage();
	}
	
	/**
	 * Obtains a connection from the pool if one is available.  When done with its use the client should return it to the
     * pool via <code>releaseConnection</code>.
	 * 
	 * @return Connection returns a pooled connection
	 * @throws ConnectionPoolOutOfConnectionsException when the pool has run out of available connections to hand out.  The
     *         client will have to retry later.
	 * @see #releaseConnection(Connection)
	 */
	@Override
	public synchronized Connection getConnection() throws ConnectionPoolException {
		// first check if we are supposed to cache connections in the pool at all
		if (poolMinConnections == 0) {
			// Nope, we are not supposed to be caching connections.  That means we will get a new connection directly from the
			// database driver f we haven't already reached the pool maximum connections allowed
			if (totalConnections < poolMaxConnections) {
				try {
					PooledConnection conn = getNewPooledConnection();
					totalConnections++;
					return conn;
				} catch (SQLException e) {
					throw new ConnectionPoolNewConnectionException("a database error occurred trying to get a new connection", e);
				}
			} else {
				throw new ConnectionPoolOutOfConnectionsException("maximum number of connections reached");
			}
		}
		
		// ok, if we got here it means that the pool is supposed to cache connections
		// check if there are any available connections in the pool
		if (pool.isEmpty()) {
			logger.warn("get connection from pool was unsuccessful: pool is out of connections");
			throw new ConnectionPoolOutOfConnectionsException();
		}

		// pool is not empty, so get a cached connection from the pool
		Connection conn = pool.remove(0);
		
		// refill the pool to the minimum size if necessary
		try {
			refillPoolToMinimumLevel();
		} catch (SQLException e) {
			throw new ConnectionPoolNewConnectionException("a database error occured while trying to refill the pool", e);
		}

		logger.info("obtained connection from pool");
		logDebugPoolStatusMessage();
		
		return PooledConnectionFactory.createPooledConnection(this, conn);
	}

	/**
	 * Return a connection to the pool.  A connection can only be released back in the pool once, otherwise an exception is
     * thrown.  Also, references to released connections become unusable.
	 * 
	 * @param connection the pooled connection being returned to the pool, must have been previously obtained via
	 *                   <code>getConnection</code>
	 * @throws ConnectionPoolReleaseConnectionException when a database access error occurs while the connection is being
     *         validated or a new connection being created to replace a closed connection; also if a connection previously
     *         released is attempted to be released again
	 * @see #getConnection()
	 * @see ConnectionPoolAlreadyReleased
	 */
	@Override
	public synchronized void releaseConnection(Connection connection) throws ConnectionPoolReleaseConnectionException {
		// fail-fast check, no need to process further if null was passed in
		if (connection == null)
			throw new IllegalArgumentException("connection cannot be null");
		
		// make sure the passed-in connection is of type PooledConnection
		if (!(connection instanceof PooledConnection))
			throw new IllegalArgumentException("connection must be of PooledConnection type");
		
		// cast the passed in connection to its real type
		PooledConnection pooledConnection = (PooledConnection) connection;

		// check if the connection has already been released previously
		if (pooledConnection.isReleasedBackToPool())
			throw new ConnectionPoolReleaseConnectionException("connection already released back to pool");
		
		try {
			// check if we currently have enough available connections in the pool
			// if we do, then we simply close the connection being returned instead of putting it back in the pool
			if (pool.size() >= poolMinConnections) {
				logger.debug("releaseConnection: pool has enough connections, so the returned connection will simply be closed");
				try {
					pooledConnection.close();
				} catch (SQLException e) {
					// We will ignore any database errors for closing the connection, hopefully this was just a temporary
					// fluke.  Otherwise we are sure to get additional errors on other operations on the connection.
					; // do nothing
					logger.warn("there was an error closing the connection: " + e);
				}
			} else { // the pool is low on connections, so let's add this connection back in
				// Check if the connection passed is still valid.  It's possible the client may have closed it before sending
				// it back to the pool.
				logger.info("checking the connection being released is still valid...");
				if (pooledConnection.isValid(0)) {
					// the connection is good, so let's add it back to the pool and make it available again
					logger.info("connection is good, so it is being placed back in the pool");
					pool.add(pooledConnection);
					totalConnections++;
				} else { 
					// the connection being returned is no longer valid, so let's create a new one for the pool
					logger.warn("connection is no longer valid, a new connection is being placed in the pool");
					pool.add(getNewPooledConnection());
					totalConnections++;
				}
			}
		} catch (SQLException e) {
			// something bad happened while either trying to determine if the connection was valid or creating a new
			// connection
			logger.error("a database error occurred during a connection release", e);
			throw new ConnectionPoolReleaseConnectionException(e);
		} finally {
			// Regardless of what happens here we reduce the total connections we are tracking for the pool.  If there was a
			// problem closing or validating the incoming connection we want to open up a slot for a new connection to take
			// place.
			totalConnections--;
			
			// mark the connection as released
			pooledConnection.setReleasedBackToPool();
		}

		logDebugPoolStatusMessage();
	}
}
