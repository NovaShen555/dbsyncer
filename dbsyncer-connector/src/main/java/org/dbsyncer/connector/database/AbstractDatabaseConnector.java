package org.dbsyncer.connector.database;

import org.apache.commons.lang.StringUtils;
import org.dbsyncer.common.util.CollectionUtils;
import org.dbsyncer.connector.ConnectorException;
import org.dbsyncer.connector.config.*;
import org.dbsyncer.connector.enums.OperationEnum;
import org.dbsyncer.connector.enums.SqlBuilderEnum;
import org.dbsyncer.connector.template.CommandTemplate;
import org.dbsyncer.connector.util.DatabaseUtil;
import org.dbsyncer.connector.util.JDBCUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractDatabaseConnector implements Database {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 获取表元信息SQL, 具体实现交给对应的连接器
     *
     * @param config
     * @param tableName
     * @return
     */
    protected abstract String getMetaSql(DatabaseConfig config, String tableName);

    @Override
    public boolean isAlive(ConnectorConfig config) {
        DatabaseConfig cfg = (DatabaseConfig) config;
        Connection connection = null;
        try {
            connection = JDBCUtil.getConnection(cfg.getDriverClassName(), cfg.getUrl(), cfg.getUsername(), cfg.getPassword());
        } catch (Exception e) {
            logger.error("Failed to connect:{}", cfg.getUrl(), e.getMessage());
        } finally {
            JDBCUtil.close(connection);
        }
        return null != connection;
    }

    @Override
    public List<String> getTable(ConnectorConfig config) {
        List<String> tables = new ArrayList<>();
        DatabaseConfig databaseConfig = (DatabaseConfig) config;
        JdbcTemplate jdbcTemplate = null;
        try {
            jdbcTemplate = getJdbcTemplate(databaseConfig);
            String sql = "show tables";
            tables = jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            logger.error("getTable failed", e.getMessage());
        } finally {
            // 释放连接
            this.close(jdbcTemplate);
        }
        return tables;
    }

    @Override
    public MetaInfo getMetaInfo(ConnectorConfig config, String tableName) {
        DatabaseConfig cfg = (DatabaseConfig) config;
        JdbcTemplate jdbcTemplate = null;
        MetaInfo metaInfo = null;
        try {
            jdbcTemplate = getJdbcTemplate(cfg);
            String metaSql = getMetaSql(cfg, tableName);
            metaInfo = DatabaseUtil.getMetaInfo(jdbcTemplate, metaSql);
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            // 释放连接
            this.close(jdbcTemplate);
        }
        return metaInfo;
    }

    @Override
    public Map<String, String> getSourceCommand(CommandTemplate commandTemplate) {
        // 获取过滤SQL
        List<Filter> filter = commandTemplate.getFilter();
        String queryFilterSql = getQueryFilterSql(filter);

        // 获取查询SQL
        Table table = commandTemplate.getTable();
        String type = SqlBuilderEnum.QUERY.getName();
        String querySql = getQuerySql(type, table, queryFilterSql);
        Map<String, String> map = new HashMap<>();
        map.put(type, querySql);
        return map;
    }

    @Override
    public Map<String, String> getTargetCommand(CommandTemplate commandTemplate) {
        // 获取增删改SQL
        Map<String, String> map = new HashMap<>();
        Table table = commandTemplate.getTable();

        String insert = SqlBuilderEnum.INSERT.getName();
        map.put(insert, getQuerySql(insert, table, null));

        String update = SqlBuilderEnum.UPDATE.getName();
        map.put(update, getQuerySql(update, table, null));

        String delete = SqlBuilderEnum.DELETE.getName();
        map.put(delete, getQuerySql(delete, table, null));
        return map;
    }

    @Override
    public JdbcTemplate getJdbcTemplate(DatabaseConfig config) {
        return DatabaseUtil.getJdbcTemplate(config);
    }

    @Override
    public void close(JdbcTemplate jdbcTemplate) {
        try {
            DatabaseUtil.close(jdbcTemplate);
        } catch (SQLException e) {
            logger.error("Close jdbcTemplate failed: {}", e.getMessage());
        }
    }

    /**
     * 获取DQL表信息
     *
     * @param config
     * @return
     */
    protected List<String> getDqlTable(ConnectorConfig config) {
        MetaInfo metaInfo = getDqlMetaInfo(config);
        Assert.notNull(metaInfo, "SQL解析异常.");
        DatabaseConfig cfg = (DatabaseConfig) config;
        return Arrays.asList(cfg.getSql());
    }

    /**
     * 获取DQl元信息
     *
     * @param config
     * @return
     */
    protected MetaInfo getDqlMetaInfo(ConnectorConfig config) {
        DatabaseConfig cfg = (DatabaseConfig) config;
        JdbcTemplate jdbcTemplate = null;
        MetaInfo metaInfo = null;
        try {
            jdbcTemplate = getJdbcTemplate(cfg);
            metaInfo = DatabaseUtil.getMetaInfo(jdbcTemplate, cfg.getSql());
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            // 释放连接
            this.close(jdbcTemplate);
        }
        return metaInfo;
    }

    /**
     * 获取查询SQL
     *
     * @param type           {@link SqlBuilderEnum}
     * @param table
     * @param queryFilterSQL
     * @return
     */
    private String getQuerySql(String type, Table table, String queryFilterSQL) {
        if (null == table) {
            logger.error("Table can not be null.");
            throw new ConnectorException("Table can not be null.");
        }
        List<Field> column = table.getColumn();
        if (CollectionUtils.isEmpty(column)) {
            logger.error("Table column can not be empty.");
            throw new ConnectorException("Table column can not be empty.");
        }
        // 获取主键
        String pk = null;
        // 去掉重复的查询字段
        List<String> filedNames = new ArrayList<>();
        for (Field c : column) {
            if (c.isPk()) {
                pk = c.getName();
            }
            String name = c.getName();
            // 如果没有重复
            if (StringUtils.isNotBlank(name) && !filedNames.contains(name)) {
                filedNames.add(name);
            }
        }
        if (CollectionUtils.isEmpty(filedNames)) {
            logger.error("The filedNames can not be empty.");
            throw new ConnectorException("The filedNames can not be empty.");
        }
        String tableName = table.getName();
        if (StringUtils.isBlank(tableName)) {
            logger.error("Table name can not be empty.");
            throw new ConnectorException("Table name can not be empty.");
        }
        return SqlBuilderEnum.getSqlBuilder(type).buildSql(tableName, pk, filedNames, queryFilterSQL, this);
    }

    /**
     * 获取查询条件SQL
     *
     * @param filter
     * @return
     */
    private String getQueryFilterSql(List<Filter> filter) {
        if (CollectionUtils.isEmpty(filter)) {
            return "";
        }
        // 过滤条件SQL
        StringBuilder condition = new StringBuilder();

        // 拼接并且SQL
        String addSql = getFilterSql(OperationEnum.AND.getName(), filter);
        // 如果Add条件存在
        if (StringUtils.isNotBlank(addSql)) {
            condition.append(addSql);
        }

        // 拼接或者SQL
        String orSql = getFilterSql(OperationEnum.OR.getName(), filter);
        if (StringUtils.isNotBlank(orSql)) {
            condition.append(orSql);
            // 如果Or条件和Add条件都存在
            if (StringUtils.isNotBlank(addSql)) {
                condition.append(" OR ").append(orSql);
            }
        }

        // 如果有条件加上 WHERE
        StringBuilder sql = new StringBuilder();
        if (StringUtils.isNotBlank(condition.toString())) {
            // WHERE (USER.USERNAME = 'zhangsan' AND USER.AGE='20') OR (USER.TEL='18299996666')
            sql.insert(0, " WHERE ").append(condition);
        }
        return sql.toString();
    }

    /**
     * 根据过滤条件获取查询SQL
     *
     * @param queryOperator and/or
     * @param filter
     * @return
     */
    private String getFilterSql(String queryOperator, List<Filter> filter) {
        List<Filter> list = filter.stream().filter(f -> StringUtils.equals(f.getOperation(), queryOperator)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return "";
        }

        int size = list.size();
        int end = size - 1;
        StringBuilder sql = new StringBuilder();
        sql.append("(");
        Filter c = null;
        for (int i = 0; i < size; i++) {
            c = list.get(i);
            // USER = 'zhangsan'
            sql.append(c.getName()).append(c.getFilter()).append("'").append(c.getValue()).append("'");
            if (i < end) {
                sql.append(" ").append(queryOperator).append(" ");
            }
        }
        sql.append(")");
        return sql.toString();
    }

    /**
     * @param ps     参数构造器
     * @param fields 同步字段，例如[{name=ID, type=4}, {name=NAME, type=12}]
     * @param row    同步字段对应的值，例如{ID=123, NAME=张三11}
     */
    private void batchRowsSetter(PreparedStatement ps, List<Field> fields, Map<String, Object> row) {
        if (CollectionUtils.isEmpty(fields)) {
            logger.error("Rows fields can not be empty.");
            throw new ConnectorException(String.format("Rows fields can not be empty."));
        }
        int fieldSize = fields.size();
        Field f = null;
        int type;
        Object val = null;
        for (int i = 0; i < fieldSize; i++) {
            // 取出字段和对应值
            f = fields.get(i);
            type = f.getType();
            val = row.get(f.getName());
            DatabaseUtil.preparedStatementSetter(ps, i + 1, type, val);
        }
    }

}