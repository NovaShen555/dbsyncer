package org.dbsyncer.connector.config;

import org.dbsyncer.common.model.AbstractConnectorConfig;

/**
 * @author AE86
 * @ClassName: ESConfig
 * @Description: ES连接配置
 * @date: 2021年8月23日 下午8:00:00
 */
public class ESConfig extends AbstractConnectorConfig {

    /**
     * 集群地址, http(s)-9200, tcp-9300 http://192.168.1.100:9200,http://192.168.1.200:9200
     */
    private String url;

    /**
     * 帐号
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 索引(相当于数据库)
     */
    private String index;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

}