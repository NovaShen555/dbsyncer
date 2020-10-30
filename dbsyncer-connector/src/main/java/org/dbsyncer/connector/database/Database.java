package org.dbsyncer.connector.database;

import org.dbsyncer.connector.Connector;
import org.dbsyncer.connector.config.DatabaseConfig;
import org.springframework.jdbc.core.JdbcTemplate;

public interface Database extends Connector {

    JdbcTemplate getJdbcTemplate(DatabaseConfig config);

    void close(JdbcTemplate jdbcTemplate);

    /**
     * 获取分页SQL
     *
     * @param tableName
     * @param pk
     * @param querySQL
     * @return
     */
    String getPageSql(String tableName, String pk, String querySQL);

    /**
     * 获取分页参数
     *
     * @param pageIndex
     * @param pageSize
     * @return
     */
    Object[] getPageArgs(int pageIndex, int pageSize);
}