package org.dbsyncer.listener.enums;

import org.apache.commons.lang.StringUtils;
import org.dbsyncer.connector.enums.ConnectorEnum;
import org.dbsyncer.listener.oracle.OracleExtractor;
import org.dbsyncer.listener.quartz.QuartzExtractor;
import org.dbsyncer.listener.ListenerException;
import org.dbsyncer.listener.mysql.MysqlExtractor;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2020/04/24 14:19
 */
public enum ListenerEnum {

    /**
     * 定时
     */
    DEFAULT(ListenerTypeEnum.TIMING.getType(), QuartzExtractor.class),
    /**
     * Mysql
     */
    MYSQL(ConnectorEnum.MYSQL.getType(), MysqlExtractor.class),
    /**
     * Oracle
     */
    ORACLE(ConnectorEnum.ORACLE.getType(), OracleExtractor.class);

    private String type;
    private Class<?> clazz;

    ListenerEnum(String type, Class<?> clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    /**
     * 获取抽取器
     *
     * @param type
     * @return
     * @throws ListenerException
     */
    public static Class<?> getExtractor(String type) throws ListenerException {
        for (ListenerEnum e : ListenerEnum.values()) {
            if (StringUtils.equals(type, e.getType())) {
                return e.getClazz();
            }
        }
        throw new ListenerException(String.format("Extractor type \"%s\" does not exist.", type));
    }

    public String getType() {
        return type;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}