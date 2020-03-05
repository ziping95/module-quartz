package com.wzp.module.quartz.listener;

import com.wzp.module.quartz.bean.JobLog;
import com.wzp.module.quartz.event.JobLogEvent;
import com.wzp.module.quartz.mapper.JobLogMapper;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
public class JobLogListener implements ApplicationListener<JobLogEvent> {

    @Resource
    private JobLogMapper jobLogMapper;

    /**
     * 记录定时任务日志
     * @param jobLogEvent
     */
    @Override
    public void onApplicationEvent(JobLogEvent jobLogEvent) {
        JobLog jobLog = (JobLog) jobLogEvent.getSource();
        jobLogMapper.create(jobLog);
    }
}