package org.dbsyncer.biz.checker.impl.tablegroup;

import org.apache.commons.lang.StringUtils;
import org.dbsyncer.biz.BizException;
import org.dbsyncer.biz.checker.AbstractChecker;
import org.dbsyncer.common.util.CollectionUtils;
import org.dbsyncer.connector.config.Field;
import org.dbsyncer.connector.config.MetaInfo;
import org.dbsyncer.connector.config.Table;
import org.dbsyncer.manager.Manager;
import org.dbsyncer.parser.model.ConfigModel;
import org.dbsyncer.parser.model.FieldMapping;
import org.dbsyncer.parser.model.Mapping;
import org.dbsyncer.parser.model.TableGroup;
import org.dbsyncer.storage.constant.ConfigConstant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2020/1/8 15:17
 */
@Component
public class TableGroupChecker extends AbstractChecker {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Manager manager;

    @Override
    public ConfigModel checkAddConfigModel(Map<String, String> params) {
        logger.info("checkAddConfigModel tableGroup params:{}", params);
        String mappingId = params.get("mappingId");
        String sourceTable = params.get("sourceTable");
        String targetTable = params.get("targetTable");
        Assert.hasText(mappingId, "tableGroup mappingId is empty.");
        Assert.hasText(sourceTable, "tableGroup sourceTable is empty.");
        Assert.hasText(targetTable, "tableGroup targetTable is empty.");
        Mapping mapping = manager.getMapping(mappingId);
        Assert.notNull(mapping, "mapping can not be null.");

        // 检查是否存在重复映射关系
        checkRepeatedTable(mappingId, sourceTable, targetTable);

        TableGroup tableGroup = new TableGroup();
        tableGroup.setName(ConfigConstant.TABLE_GROUP);
        tableGroup.setType(ConfigConstant.TABLE_GROUP);
        tableGroup.setMappingId(mappingId);
        tableGroup.setSourceTable(getTable(mapping.getSourceConnectorId(), sourceTable));
        tableGroup.setTargetTable(getTable(mapping.getTargetConnectorId(), targetTable));

        // 生成command
        setCommand(mapping, tableGroup);

        // 修改基本配置
        this.modifyConfigModel(tableGroup, params);
        return tableGroup;
    }

    @Override
    public ConfigModel checkEditConfigModel(Map<String, String> params) {
        logger.info("checkEditConfigModel tableGroup params:{}", params);
        Assert.notEmpty(params, "TableGroupChecker check params is null.");
        String id = params.get(ConfigConstant.CONFIG_MODEL_ID);
        TableGroup tableGroup = manager.getTableGroup(id);
        Assert.notNull(tableGroup, "Can not find tableGroup.");

        // 修改基本配置
        this.modifyConfigModel(tableGroup, params);

        // 字段映射关系
        String fieldMappingJson = params.get("fieldMapping");
        Assert.hasText(fieldMappingJson, "TableGroupChecker check params fieldMapping is empty");
        setFieldMapping(tableGroup, fieldMappingJson);

        // 生成command
        Mapping mapping = manager.getMapping(tableGroup.getMappingId());
        Assert.notNull(mapping, "mapping can not be null.");
        setCommand(mapping, tableGroup);

        // 修改高级配置：过滤条件/转换配置/插件配置
        this.modifySuperConfigModel(tableGroup, params);

        return tableGroup;
    }

    private Table getTable(String connectorId, String tableName) {
        MetaInfo metaInfo = manager.getMetaInfo(connectorId, tableName);
        Assert.notNull(metaInfo, "无法获取连接信息.");
        return new Table().setName(tableName).setColumn(metaInfo.getColumn());
    }

    private void checkRepeatedTable(String mappingId, String sourceTable, String targetTable) {
        List<TableGroup> list = manager.getTableGroupAll(mappingId);
        if (!CollectionUtils.isEmpty(list)) {
            for (TableGroup g : list) {
                // 数据源表和目标表都存在
                if (StringUtils.equals(sourceTable, g.getSourceTable().getName()) && StringUtils.equals(targetTable,
                        g.getTargetTable().getName())) {
                    final String error = String.format("映射关系已存在.", sourceTable, targetTable);
                    logger.error(error);
                    throw new BizException(error);
                }
            }
        }
    }

    /**
     * 解析映射关系
     *
     * @param tableGroup
     * @param json [{"source":"id","target":"id"}]
     * @return
     */
    private void setFieldMapping(TableGroup tableGroup, String json) {
        try {
            JSONArray mapping = new JSONArray(json);
            if(null == mapping){
                throw new BizException("映射关系不能为空");
            }

            final Map<String, Field> sMap = convert2Map(tableGroup.getSourceTable().getColumn());
            final Map<String, Field> tMap = convert2Map(tableGroup.getTargetTable().getColumn());
            int length = mapping.length();
            List<FieldMapping> list = new ArrayList<>();
            JSONObject row = null;
            Field s = null;
            Field t = null;
            for (int i = 0; i < length; i++) {
                row = mapping.getJSONObject(i);
                s = sMap.get(row.getString("source"));
                t = tMap.get(row.getString("target"));
                t.setPk(row.getBoolean("pk"));
                list.add(new FieldMapping(s, t));
            }
            tableGroup.setFieldMapping(list);
        } catch (JSONException e) {
            logger.error(e.getMessage());
            throw new BizException(e.getMessage());
        }
    }

    private Map<String, Field> convert2Map(List<Field> col) {
        final Map<String, Field> map = new HashMap<>();
        col.forEach(f -> map.put(f.getName(), f));
        return map;
    }

    private void setCommand(Mapping mapping, TableGroup tableGroup) {
        Map<String, String> command = manager.getCommand(mapping.getSourceConnectorId(), mapping.getTargetConnectorId(), tableGroup);
        tableGroup.setCommand(command);
    }
}