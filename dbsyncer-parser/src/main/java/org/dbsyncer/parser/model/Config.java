package org.dbsyncer.parser.model;

/**
 * @version 1.0.0
 * @Author AE86
 * @Date 2020-05-29 20:13
 */
public class Config extends ConfigModel {

    private int refreshInterval = 5;

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

}