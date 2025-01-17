package org.dbsyncer.listener;

import org.dbsyncer.listener.enums.ListenerTypeEnum;
import org.dbsyncer.listener.enums.LogExtractorEnum;
import org.dbsyncer.listener.enums.TimingExtractorEnum;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class ListenerFactory implements Listener {

    private Map<ListenerTypeEnum, Function<String, Class>> map = new LinkedHashMap<>();

    @PostConstruct
    private void init() {
        map.putIfAbsent(ListenerTypeEnum.LOG, (connectorType) -> LogExtractorEnum.getExtractor(connectorType));
        map.putIfAbsent(ListenerTypeEnum.TIMING, (connectorType) -> TimingExtractorEnum.getExtractor(connectorType));
    }

    @Override
    public <T> T getExtractor(ListenerTypeEnum listenerTypeEnum, String connectorType, Class<T> valueType) throws IllegalAccessException, InstantiationException {
        Function function = map.get(listenerTypeEnum);
        if (null == function) {
            throw new ListenerException(String.format("Unsupported type \"%s\" for extractor \"%s\".", listenerTypeEnum, connectorType));
        }

        Class<T> clazz = (Class<T>) function.apply(connectorType);
        return clazz.newInstance();
    }

}