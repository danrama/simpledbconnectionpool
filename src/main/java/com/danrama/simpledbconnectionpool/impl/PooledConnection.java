package com.danrama.simpledbconnectionpool.impl;

import java.sql.Connection;

/**
 * The interface that will serve as the proxied wrapper around raw database connections obtained from an underlying database
 * driver.  The proxy will keep track of whether the connection has been released back to the pool and will prevent further
 * releases.
 * 
 * @author Daniel Bloomfield Ramagem
 * @see PooledConnectionFactory
 * @see PooledConnectionProxy
 * @see ConnectionPoolAlreadyReleasedConnectionException
 */
public interface PooledConnection extends Connection {
	/**
	 * Gets the connection pool to which this connection belongs.
	 * 
	 * @return the originating pool of the connection
	 */
	AbstractConnectionPool getPool();
	
	/**
	 * Verifies if this connection has already been released back into its
	 * originating pool.
	 * 
	 * @return <code>true</code> if the connection has already been released (and is no longer usable), <code>false</code>
     *         otherwise
	 */
	boolean isReleasedBackToPool();
	
	/**
	 * Mark this connection as being released back into its pool and no longer usable.
	 */
	void setReleasedBackToPool();
}
