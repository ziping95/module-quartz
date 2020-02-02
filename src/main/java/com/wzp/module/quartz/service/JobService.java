package com.wzp.module.quartz.service;

import com.wzp.module.quartz.bean.QuartzJob;

import java.util.List;
import java.util.Map;

public interface JobService {

    List<QuartzJob> list();

    /**
     * 启动定时任务
     * @param jobId
     * @throws Exception
     */
    void runJob(String jobId) throws Exception;

    /**
     * 批量启动任务
     * @param jobs
     * @throws Exception
     */
    void runJobFromList(List<QuartzJob> jobs) throws Exception;

    /**
     * 停止定时任务
     * @param jobId
     * @throws Exception
     */
    void stopJob(String jobId) throws Exception;

    /**
     * 更新定时任务
     * @param quartzJob
     * @return
     * @throws Exception
     */
    Integer updateJob(QuartzJob quartzJob) throws Exception;

    /**
     *手动触发一次任务
     * @param jobId
     * @throws Exception
     */
    void runJobNow(String jobId) throws Exception;

    /**
     * 自定义条件查询
     * @param map
     * @return
     */
    List<QuartzJob> detailsByCondition(Map<String,Object> map);
}
