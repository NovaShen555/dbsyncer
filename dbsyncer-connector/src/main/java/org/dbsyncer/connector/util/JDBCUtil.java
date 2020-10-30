package org.dbsyncer.connector.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class JDBCUtil {

    private final static Logger logger = LoggerFactory.getLogger(JDBCUtil.class);

    public static Connection getConnection(String driver, String url, String username, String password) throws SQLException {
        // com.mysql.jdbc.JDBC4Connection 
        // 不需要显式调用 Class.forName(driver), DriverManager.getConnection会自动加载合适的驱动
        return DriverManager.getConnection(url, username, password);
    }

    public static void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error(e.getClass() + " >> " + e.getLocalizedMessage());
            }
        }
    }

    public static void close(Connection conn) {
        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error(e.getClass() + " >> " + e.getLocalizedMessage());
            }
        }
    }

    public static void close(Statement statement, Connection conn) {
        close(statement);
        close(conn);
    }

}