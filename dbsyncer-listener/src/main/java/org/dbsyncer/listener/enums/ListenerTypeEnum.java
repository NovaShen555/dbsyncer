package org.dbsyncer.listener.enums;

import org.apache.commons.lang.StringUtils;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2020/04/24 14:19
 */
public enum ListenerTypeEnum {

    /**
     * 定时
     */
    TIMING("timing"),
    /**
     * 日志
     */
    LOG("log");

    private String type;

    ListenerTypeEnum(String type) {
        this.type = type;
    }

    public static boolean isTiming(String type) {
        return StringUtils.equals(TIMING.getType(), type);
    }

    public static boolean isLog(String type) {
        return StringUtils.equals(LOG.getType(), type);
    }

    public String getType() {
        return type;
    }

}