package org.dbsyncer.listener.file;

import org.apache.commons.io.IOUtils;
import org.dbsyncer.common.event.ChangedOffset;
import org.dbsyncer.common.event.RowChangedEvent;
import org.dbsyncer.common.file.BufferedRandomAccessFile;
import org.dbsyncer.common.util.CollectionUtils;
import org.dbsyncer.common.util.NumberUtil;
import org.dbsyncer.common.util.StringUtil;
import org.dbsyncer.connector.config.FileConfig;
import org.dbsyncer.connector.constant.ConnectorConstant;
import org.dbsyncer.connector.file.FileConnectorMapper;
import org.dbsyncer.connector.file.FileResolver;
import org.dbsyncer.connector.model.Field;
import org.dbsyncer.connector.model.FileSchema;
import org.dbsyncer.listener.AbstractExtractor;
import org.dbsyncer.listener.ListenerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2022/5/6 21:42
 */
public class FileExtractor extends AbstractExtractor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String POS_PREFIX = "pos_";
    private static final String CHARSET_NAME = "UTF-8";
    private final Lock connectLock = new ReentrantLock();
    private volatile boolean connected;
    private FileConnectorMapper connectorMapper;
    private WatchService watchService;
    private Worker worker;
    private Map<String, PipelineResolver> pipeline = new ConcurrentHashMap<>();
    private final FileResolver fileResolver = new FileResolver();
    private char separator;

    @Override
    public void start() {
        try {
            connectLock.lock();
            if (connected) {
                logger.error("FileExtractor is already started");
                return;
            }

            connectorMapper = (FileConnectorMapper) connectorFactory.connect(connectorConfig);
            final FileConfig config = connectorMapper.getConfig();
            final String mapperCacheKey = connectorFactory.getConnector(connectorMapper).getConnectorMapperCacheKey(connectorConfig);
            connected = true;

            separator = config.getSeparator();
            initPipeline(config.getFileDir());
            watchService = FileSystems.getDefault().newWatchService();
            Path p = Paths.get(config.getFileDir());
            p.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            for (String fileName : pipeline.keySet()) {
                parseEvent(fileName);
            }

            worker = new Worker();
            worker.setName(new StringBuilder("file-parser-").append(mapperCacheKey).append("_").append(worker.hashCode()).toString());
            worker.setDaemon(false);
            worker.start();
        } catch (Exception e) {
            logger.error("启动失败:{}", e.getMessage());
            closePipelineAndWatch();
            throw new ListenerException(e);
        } finally {
            connectLock.unlock();
        }
    }

    private void initPipeline(String fileDir) throws IOException {
        for (FileSchema fileSchema : connectorMapper.getFileSchemaList()) {
            String fileName = fileSchema.getFileName();
            String file = fileDir.concat(fileName);
            Assert.isTrue(new File(file).exists(), String.format("found not file '%s'", file));

            final RandomAccessFile raf = new BufferedRandomAccessFile(file, "r");
            final String filePosKey = getFilePosKey(fileName);
            if (snapshot.containsKey(filePosKey)) {
                raf.seek(NumberUtil.toLong(snapshot.get(filePosKey), 0L));
            } else {
                raf.seek(raf.length());
                snapshot.put(filePosKey, String.valueOf(raf.getFilePointer()));
                super.forceFlushEvent();
            }

            pipeline.put(fileName, new PipelineResolver(fileSchema.getFields(), raf));
        }
    }

    @Override
    public void close() {
        try {
            closePipelineAndWatch();
            connected = false;
            if (null != worker && !worker.isInterrupted()) {
                worker.interrupt();
                worker = null;
            }
        } catch (Exception e) {
            logger.error("关闭失败:{}", e.getMessage());
        }
    }

    @Override
    public void refreshEvent(ChangedOffset offset) {
        if (offset.getNextFileName() != null && offset.getPosition() != null) {
            snapshot.put(offset.getNextFileName(), String.valueOf(offset.getPosition()));
        }
    }

    private void closePipelineAndWatch() {
        try {
            pipeline.values().forEach(pipelineResolver -> IOUtils.closeQuietly(pipelineResolver.raf));
            pipeline.clear();

            if (null != watchService) {
                watchService.close();
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
    }

    private String getFilePosKey(String fileName) {
        return POS_PREFIX.concat(fileName);
    }

    private void parseEvent(String fileName) throws IOException {
        if (pipeline.containsKey(fileName)) {
            PipelineResolver pipelineResolver = pipeline.get(fileName);
            final RandomAccessFile raf = pipelineResolver.raf;

            final String filePosKey = getFilePosKey(fileName);
            List<List> list = new ArrayList<>();
            String line;
            while (null != (line = pipelineResolver.readLine())) {
                if (StringUtil.isNotBlank(line)) {
                    list.add(fileResolver.parseList(pipelineResolver.fields, separator, line));
                }
            }

            if (!CollectionUtils.isEmpty(list)) {
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    RowChangedEvent event = new RowChangedEvent(fileName, ConnectorConstant.OPERTION_UPDATE, list.get(i));
                    if (i == size - 1) {
                        event.setNextFileName(filePosKey);
                        event.setPosition(raf.getFilePointer());
                    }
                    changeEvent(event);
                }
            }
        }
    }

    final class PipelineResolver {
        List<Field> fields;
        RandomAccessFile raf;
        byte[] b;
        long filePointer;

        public PipelineResolver(List<Field> fields, RandomAccessFile raf) {
            this.fields = fields;
            this.raf = raf;
        }

        public String readLine() throws IOException {
            this.filePointer = raf.getFilePointer();
            if (filePointer >= raf.length()) {
                b = new byte[0];
                return null;
            }
            if (b == null || b.length == 0) {
                b = new byte[(int) (raf.length() - filePointer)];
            }
            raf.read(b);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            int read = 0;
            for (int i = 0; i < b.length; i++) {
                read++;
                if (b[i] == '\n' || b[i] == '\r') {
                    break;
                }
                stream.write(b[i]);
            }
            b = Arrays.copyOfRange(b, read, b.length);

            raf.seek(this.filePointer + read);
            byte[] _b = stream.toByteArray();
            stream.close();
            stream = null;
            return new String(_b, CHARSET_NAME);
        }
    }

    final class Worker extends Thread {

        @Override
        public void run() {
            while (!isInterrupted() && connected) {
                WatchKey watchKey = null;
                try {
                    watchKey = watchService.take();
                    List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                    for (WatchEvent<?> event : watchEvents) {
                        parseEvent(event.context().toString());
                    }
                } catch (ClosedWatchServiceException e) {
                    break;
                } catch (Exception e) {
                    logger.error(e.getMessage());
                } finally {
                    if (null != watchKey) {
                        watchKey.reset();
                    }
                }
            }
        }

    }

}