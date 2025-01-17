package org.dbsyncer.biz;

import org.dbsyncer.biz.vo.MappingVo;

import java.util.List;
import java.util.Map;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2019/10/17 23:14
 */
public interface MappingService {

    /**
     * 新增驱动
     *
     * @param params
     */
    String add(Map<String, String> params);

    /**
     * 复制驱动
     *
     * @param id
     */
    String copy(String id);

    /**
     * 修改驱动
     *
     * @param params
     */
    String edit(Map<String, String> params);

    /**
     * 删除驱动
     *
     * @param id
     */
    String remove(String id);

    /**
     * 获取驱动
     *
     * @param id
     * @return
     */
    MappingVo getMapping(String id);

    /**
     * 获取所有驱动
     *
     * @return
     */
    List<MappingVo> getMappingAll();

    /**
     * 启动驱动
     *
     * @param id
     */
    String start(String id);

    /**
     * 停止驱动
     *
     * @param id
     */
    String stop(String id);

}