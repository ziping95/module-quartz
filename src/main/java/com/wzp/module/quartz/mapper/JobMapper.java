package com.wzp.module.quartz.mapper;

import com.wzp.module.quartz.bean.QuartzJob;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface JobMapper {

    List<QuartzJob> list();

    QuartzJob details(@Param("jobId") String id);

    Integer update(QuartzJob job);

    /**
     * 自定义条件查询
     * @param map
     * @return
     */
    List<QuartzJob> detailsByCondition(Map<String,Object> map);
}
