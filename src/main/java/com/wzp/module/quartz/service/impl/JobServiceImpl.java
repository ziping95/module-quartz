package com.wzp.module.quartz.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wzp.module.core.utils.SpringUtil;
import com.wzp.module.core.utils.StringUtil;
import com.wzp.module.quartz.QuartzJobConstant;
import com.wzp.module.quartz.bean.QuartzJob;
import com.wzp.module.quartz.mapper.JobMapper;
import com.wzp.module.quartz.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("jobService")
public class JobServiceImpl implements JobService {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private Scheduler scheduler;

    @Override
    public void runJobFromList(List<QuartzJob> jobs) throws Exception {

        for (QuartzJob job : jobs) {
            job.runJob();
        }
    }

    @Override
    public List<QuartzJob> list() {
        return jobMapper.list();
    }

    @Override
    public void runJob(String jobId) throws Exception {
        QuartzJob job = jobMapper.details(jobId);
        if (job == null) {
            throw new Exception("该任务不存在");
        }

        if (!QuartzJobConstant.JOB_STOP.equals(job.getStatus())) {
            throw new Exception("该任务已经开启，请勿重复开启");
        }

        // 启动任务
        job.runJob();

        job.setStatus("1");
        jobMapper.update(job);
    }

    @Override
    public void stopJob(String jobId) throws Exception {
        QuartzJob job = jobMapper.details(jobId);
        if (job == null) {
            throw new Exception("该任务不存在");
        }
        JobKey jobKey = new JobKey(job.getJobName(), job.getJobGroup());

        if (!scheduler.deleteJob(jobKey)) {
            throw new Exception("停止任务失败");
        }

        job.setStatus("0");
        jobMapper.update(job);
    }

    @Override
    public Integer updateJob(QuartzJob paramBean) throws Exception {
        QuartzJob quartzJob = this.jobMapper.details(paramBean.getId());
        if (quartzJob == null) {
            throw new Exception("该任务不存在");
        }

        if (!CronExpression.isValidExpression(paramBean.getCron())) {
            throw new Exception("cron表达式格式错误");
        }

        quartzJob.setCron(paramBean.getCron());

        Integer count = jobMapper.update(quartzJob);

        if (count == 0) {
            throw new Exception("更新失败");
        }

        // 如果任务已经开启,则更新定时器
        if(QuartzJobConstant.JOB_RUN.equals(quartzJob.getStatus())) {
            TriggerKey triggerKey = TriggerKey.triggerKey(quartzJob.getJobName(),quartzJob.getJobGroup());
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(quartzJob.getCron());
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
            scheduler.rescheduleJob(triggerKey,trigger);
        }

        return count;
    }

    @Override
    public List<QuartzJob> detailsByCondition(Map<String, Object> map) {
        return this.jobMapper.detailsByCondition(map);
    }

    @Override
    public void runJobNow(String jobId) throws Exception {
        QuartzJob quartzJob = this.jobMapper.details(jobId);
        if(quartzJob == null) {
            throw new Exception("该任务不存在");
        }

        if(QuartzJobConstant.JOB_STOP.equals(quartzJob.getStatus())) {
            throw new Exception("该任务未启动");
        }
        JobKey jobKey = JobKey.jobKey(quartzJob.getJobName(),quartzJob.getJobGroup());
        scheduler.triggerJob(jobKey);
    }





}
