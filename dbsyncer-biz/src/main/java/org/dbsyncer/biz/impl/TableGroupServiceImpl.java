package org.dbsyncer.biz.impl;

import org.dbsyncer.biz.TableGroupService;
import org.dbsyncer.biz.checker.Checker;
import org.dbsyncer.common.util.CollectionUtils;
import org.dbsyncer.connector.config.Field;
import org.dbsyncer.parser.logger.LogType;
import org.dbsyncer.parser.model.Mapping;
import org.dbsyncer.parser.model.TableGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2019/11/27 23:14
 */
@Service
public class TableGroupServiceImpl extends BaseServiceImpl implements TableGroupService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Checker tableGroupChecker;

    @Override
    public String add(Map<String, String> params) {
        TableGroup model = (TableGroup) tableGroupChecker.checkAddConfigModel(params);
        assertRunning(model);
        log(LogType.TableGroupLog.INSERT, model);

        String id = manager.addTableGroup(model);

        // 合并驱动公共字段
        mergeMappingColumn(model.getMappingId());

        return id;
    }

    @Override
    public String edit(Map<String, String> params) {
        TableGroup model = (TableGroup) tableGroupChecker.checkEditConfigModel(params);
        assertRunning(model);
        log(LogType.TableGroupLog.UPDATE, model);

        return manager.editTableGroup(model);
    }

    @Override
    public boolean remove(String mappingId, String ids) {
        Assert.hasText(mappingId, "Mapping id can not be null");
        Assert.hasText(ids, "TableGroup ids can not be null");
        Mapping mapping = manager.getMapping(mappingId);
        Assert.notNull(mapping, "Mapping can not be null");
        assertRunning(mapping.getMetaId());

        // 批量删除表
        Stream.of(ids.split(",")).parallel().forEach(id -> {
            TableGroup model = manager.getTableGroup(id);
            log(LogType.TableGroupLog.DELETE, model);
            manager.removeTableGroup(id);
        });

        // 合并驱动公共字段
        mergeMappingColumn(mapping.getId());
        return true;
    }

    @Override
    public TableGroup getTableGroup(String id) {
        TableGroup tableGroup = manager.getTableGroup(id);
        Assert.notNull(tableGroup, "TableGroup can not be null");
        return tableGroup;
    }

    @Override
    public List<TableGroup> getTableGroupAll(String mappingId) {
        return manager.getTableGroupAll(mappingId);
    }

    private void mergeMappingColumn(String mappingId) {
        List<TableGroup> groups = manager.getTableGroupAll(mappingId);

        Mapping mapping = manager.getMapping(mappingId);
        Assert.notNull(mapping, "mapping not exist.");

        List<Field> sourceColumn = null;
        List<Field> targetColumn = null;
        for (TableGroup g : groups) {
            sourceColumn = pickCommonFields(sourceColumn, g.getSourceTable().getColumn());
            targetColumn = pickCommonFields(targetColumn, g.getTargetTable().getColumn());
        }

        mapping.setSourceColumn(sourceColumn);
        mapping.setTargetColumn(targetColumn);
        manager.editMapping(mapping);
    }

    private List<Field> pickCommonFields(List<Field> column, List<Field> target) {
        if (CollectionUtils.isEmpty(column) || CollectionUtils.isEmpty(target)) {
            return target;
        }
        List<Field> list = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        column.forEach(f -> keys.add(f.getName()));
        target.forEach(f -> {
            if (keys.contains(f.getName())) {
                list.add(f);
            }
        });
        return list;
    }

}