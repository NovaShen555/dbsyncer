package org.dbsyncer.connector.enums;

import org.dbsyncer.connector.constant.ConnectorConstant;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilder;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilderDelete;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilderInsert;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilderQuery;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilderQueryCount;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilderQueryExist;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilderQueryCursor;
import org.dbsyncer.connector.database.sqlbuilder.SqlBuilderUpdate;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2019/9/28 22:13
 */
public enum SqlBuilderEnum {

    /**
     * 插入SQL生成器
     */
    INSERT(ConnectorConstant.OPERTION_INSERT, new SqlBuilderInsert()),
    /**
     * 修改SQL生成器
     */
    UPDATE(ConnectorConstant.OPERTION_UPDATE, new SqlBuilderUpdate()),
    /**
     * 删除SQL生成器
     */
    DELETE(ConnectorConstant.OPERTION_DELETE, new SqlBuilderDelete()),
    /**
     * 查询SQL生成器
     */
    QUERY(ConnectorConstant.OPERTION_QUERY, new SqlBuilderQuery()),
    /**
     * 查询游标SQL生成器
     */
    QUERY_CURSOR(ConnectorConstant.OPERTION_QUERY_CURSOR, new SqlBuilderQueryCursor()),
    /**
     * 查询总数
     */
    QUERY_COUNT(ConnectorConstant.OPERTION_QUERY_COUNT, new SqlBuilderQueryCount()),
    /**
     * 查询行数据是否存在
     */
    QUERY_EXIST(ConnectorConstant.OPERTION_QUERY_EXIST, new SqlBuilderQueryExist());

    /**
     * SQL构造器名称
     */
    private String name;

    /**
     * SQL构造器
     */
    private SqlBuilder sqlBuilder;

    SqlBuilderEnum(String name, SqlBuilder sqlBuilder) {
        this.name = name;
        this.sqlBuilder = sqlBuilder;
    }

    public String getName() {
        return name;
    }

    public SqlBuilder getSqlBuilder() {
        return sqlBuilder;
    }

}