package de.bb.bejy.j2ee;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import de.bb.bejy.ServerThread;
import de.bb.sql.JdbcFactory;
import de.bb.util.Pool;

/**
 * The data source implementation. Uses a JdbcFactory to manage the JDBC connections.
 * 
 * @author stefan franke
 * 
 */
public class DS implements DataSource {

    private Pool pool;

    public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setLoginTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub

    }

    public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public Connection getConnection() throws SQLException {
        try {
            final ThreadContext tc = ThreadContext.currentThreadContext();
            if (tc.connection == null) {
                if (pool == null) {
                    pool = new Pool(new JdbcFactory("org.postgresql.Driver",
                            "jdbc:postgresql://127.0.0.1:5432/dzdb2", "dzbank_prod", "dzbank_prod"));
                }
                tc.pool = pool;
                tc.connection = (Connection) pool.obtain(Thread.currentThread());
            }
            return tc.connection;
        } catch (SQLException sqle) {
            throw sqle;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

}
