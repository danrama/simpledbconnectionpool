package com.danrama.simpledbconnectionpool;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for a basic Connection Pool.
 */
public interface ConnectionPool {

    /**
     * Gets a connection from the connection pool.
     * 
     * @return a valid connection from the pool.
     */
    Connection getConnection() throws SQLException;

    /**
     * Releases a connection back into the connection pool.
     * 
     * @param connection the connection to return to the pool
     * @throws java.sql.SQLException
     */
    void releaseConnection(Connection connection) throws SQLException;
}
