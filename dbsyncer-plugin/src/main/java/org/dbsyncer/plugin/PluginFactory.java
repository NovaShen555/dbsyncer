package org.dbsyncer.plugin;

import org.apache.commons.io.FileUtils;
import org.dbsyncer.common.model.FullConvertContext;
import org.dbsyncer.common.model.IncrementConvertContext;
import org.dbsyncer.common.spi.ConvertService;
import org.dbsyncer.common.spi.ProxyApplicationContext;
import org.dbsyncer.common.util.CollectionUtils;
import org.dbsyncer.plugin.config.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2019/10/1 13:26
 */
@Component
public class PluginFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 插件路径dbsyncer/plugin/
     */
    private final String PLUGIN_PATH = new StringBuilder(System.getProperty("user.dir")).append(File.separatorChar).append("plugins")
            .append(File.separatorChar).toString();

    /**
     * 依赖路径dbsyncer/lib/
     */
    private final String LIBRARY_PATH = new StringBuilder(System.getProperty("user.dir")).append(File.separatorChar).append("lib")
            .append(File.separatorChar).toString();

    private final List<Plugin> plugins = new LinkedList<>();

    @Autowired
    private Map<String, ConvertService> service;

    @Autowired
    private ProxyApplicationContext applicationContextProxy;

    @PostConstruct
    private void init() {
        Map<String, ConvertService> unmodifiable = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(service)) {
            service.forEach((k, s) -> {
                String className = s.getClass().getName();
                unmodifiable.putIfAbsent(className, s);
                plugins.add(new Plugin(s.getName(), className, s.getVersion(), "", true));
            });
        }

        service.clear();
        service.putAll(unmodifiable);
    }

    public synchronized void loadPlugins() {
        if (!CollectionUtils.isEmpty(plugins)) {
            List<Plugin> unmodifiablePlugin = plugins.stream().filter(p -> p.isUnmodifiable()).collect(Collectors.toList());
            plugins.clear();
            plugins.addAll(unmodifiablePlugin);
        }
        try {
            FileUtils.forceMkdir(new File(PLUGIN_PATH));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        Collection<File> files = FileUtils.listFiles(new File(PLUGIN_PATH), new String[]{"jar"}, true);
        if (!CollectionUtils.isEmpty(files)) {
            files.forEach(f -> loadPlugin(f));
        }
        logger.info("PreLoad plugin:{}", plugins.size());
    }

    public String getPluginPath() {
        return PLUGIN_PATH;
    }

    public String getLibraryPath() {
        return LIBRARY_PATH;
    }

    public List<Plugin> getPluginAll() {
        return Collections.unmodifiableList(plugins);
    }

    public void convert(Plugin plugin, String targetTableName, List<Map> sourceList, List<Map> targetList) {
        if (null != plugin && service.containsKey(plugin.getClassName())) {
            service.get(plugin.getClassName()).convert(new FullConvertContext(applicationContextProxy, targetTableName, sourceList, targetList));
        }
    }

    public void convert(Plugin plugin, String targetTableName, String event, List<Map> sourceList, List<Map> targetList) {
        if (null != plugin && service.containsKey(plugin.getClassName())) {
            ConvertService convertService = service.get(plugin.getClassName());
            int size = sourceList.size();
            if (size == targetList.size()) {
                for (int i = 0; i < size; i++) {
                    convertService.convert(new IncrementConvertContext(applicationContextProxy, targetTableName, event, sourceList.get(i), targetList.get(i)));
                }
            }
        }
    }

    /**
     * 完成同步后执行处理
     *
     * @param plugin
     * @param targetTableName
     * @param event
     * @param sourceList
     * @param targetList
     */
    public void postProcessAfter(Plugin plugin, String targetTableName, String event, List<Map> sourceList, List<Map> targetList) {
        if (null != plugin && service.containsKey(plugin.getClassName())) {
            ConvertService convertService = service.get(plugin.getClassName());
            int size = sourceList.size();
            if (size == targetList.size()) {
                for (int i = 0; i < size; i++) {
                    convertService.postProcessAfter(new IncrementConvertContext(applicationContextProxy, targetTableName, event, sourceList.get(i), targetList.get(i)));
                }
            }
        }
    }

    /**
     * SPI, 扫描jar扩展接口实现，注册为本地服务
     *
     * @param jar
     */
    private void loadPlugin(File jar) {
        try {
            String fileName = jar.getName();
            URL url = jar.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
            ServiceLoader<ConvertService> services = ServiceLoader.load(ConvertService.class, loader);
            for (ConvertService s : services) {
                String className = s.getClass().getName();
                service.put(className, s);
                plugins.add(new Plugin(s.getName(), className, s.getVersion(), fileName));
                logger.info("{}, {}_{} {}", fileName, s.getName(), s.getVersion(), className);
            }
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        }

    }

}