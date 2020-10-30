package org.dbsyncer.connector;

import org.dbsyncer.common.model.Result;
import org.dbsyncer.connector.config.CommandConfig;
import org.dbsyncer.connector.config.ConnectorConfig;
import org.dbsyncer.connector.config.Field;
import org.dbsyncer.connector.config.MetaInfo;

import java.util.List;
import java.util.Map;

/**
 * 连接器基础功能
 *
 * @author AE86
 * @version 1.0.0
 * @date 2019/9/18 23:30
 */
public interface Connector {

    /**
     * 检查连接器是否连接正常
     *
     * @param config 连接器配置
     * @return
     */
    boolean isAlive(ConnectorConfig config);

    /**
     * 获取所有表名
     *
     * @param config
     * @return
     */
    List<String> getTable(ConnectorConfig config);

    /**
     * 获取表元信息
     *
     * @param config
     * @param tableName
     * @return
     */
    MetaInfo getMetaInfo(ConnectorConfig config, String tableName);

    /**
     * 获取数据源同步参数
     *
     * @param commandConfig
     * @return
     */
    Map<String, String> getSourceCommand(CommandConfig commandConfig);

    /**
     * 获取目标源同步参数
     *
     * @param commandConfig
     * @return
     */
    Map<String, String> getTargetCommand(CommandConfig commandConfig);

    /**
     * 获取总数
     *
     * @param config
     * @param command
     * @return
     */
    long getCount(ConnectorConfig config, Map<String, String> command);

    /**
     * 分页获取数据源数据
     *
     * @param config    连接器配置
     * @param command   执行命令
     * @param args      命令参数
     * @param pageIndex 页数
     * @param pageSize  页大小
     * @return
     */
    Result reader(ConnectorConfig config, Map<String, String> command, List<Object> args, int pageIndex, int pageSize);

    /**
     * 批量写入目标源数据
     *
     * @param config  连接器配置
     * @param command 执行命令
     * @param fields  字段信息
     * @param data    数据
     * @return
     */
    Result writer(ConnectorConfig config, Map<String, String> command, List<Field> fields, List<Map<String, Object>> data);

    /**
     * 写入目标源数据
     *
     * @param config  连接器配置
     * @param fields  字段信息
     * @param command 执行命令
     * @param event   事件
     * @param data    数据
     * @return
     */
    Result writer(ConnectorConfig config, List<Field> fields, Map<String, String> command, String event, Map<String, Object> data);
}