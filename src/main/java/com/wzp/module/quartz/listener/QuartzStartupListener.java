package com.wzp.module.quartz.listener;

import com.wzp.module.core.utils.CollectionUtil;
import com.wzp.module.quartz.QuartzJobConstant;
import com.wzp.module.quartz.bean.QuartzJob;
import com.wzp.module.quartz.mapper.JobMapper;
import com.wzp.module.quartz.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.SchedulerContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class QuartzStartupListener implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private JobService jobService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            Map<String,Object> map = new HashMap<>();
            map.put("status", QuartzJobConstant.JOB_RUN);
            List<QuartzJob> jobs = jobService.detailsByCondition(map);

            if (CollectionUtil.isNotEmpty(jobs)) {
                this.jobService.runJobFromList(jobs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("初始化定时任务失败，错误信息为：{}",e.getMessage());
        }

    }
}
