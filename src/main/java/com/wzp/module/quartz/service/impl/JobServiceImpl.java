package com.wzp.module.quartz.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
            runJob(job);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;


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
        runJob(job);

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

    private void runJob (QuartzJob job) throws Exception {
        MethodInvokingJobDetailFactoryBean jobDetail = new MethodInvokingJobDetailFactoryBean();
        // 禁止并发
        jobDetail.setConcurrent(false);
        Class clazz = Class.forName(job.getInvokeClass());
        jobDetail.setName(job.getJobName());
        jobDetail.setGroup(job.getJobGroup());
        jobDetail.setTargetClass(clazz);
        jobDetail.setTargetMethod(job.getMethodName());
        String beanName = job.getSpringBeanName();

        // 如果beanName为空则默认类名小写
        if (StringUtils.isEmpty(beanName)) {
            beanName = handleBeanName(job.getInvokeClass());
        }
        Object targetObject = applicationContext.getBean(beanName);

        // 对cglib方式代理的bean做处理
        targetObject = handleProxyObject(targetObject);

        if (targetObject == null) {
            throw new Exception("获取定时任务bean失败");
        }

        jobDetail.setTargetObject(targetObject);
        String paramJson = job.getMethodParam();

        if (StringUtils.isNotEmpty(paramJson)) {
            jobDetail.setArguments(handleParam(paramJson));
        }

        jobDetail.afterPropertiesSet();

        // 设置定时器
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(job.getJobName(), job.getJobGroup())
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCron())).startNow().build();

        // 设置调度器
        scheduler.scheduleJob(jobDetail.getObject(), trigger);
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    /**
     * 类名首字母小写
     *
     * @param rawName
     * @return
     */
    private String handleBeanName(String rawName) {
        String clazzName = rawName.substring(rawName.lastIndexOf(".") + 1);
        return clazzName.substring(0, 1).toLowerCase() + clazzName.substring(1);
    }

    /**
     * 解析json获得定时任务参数
     *
     * @param rawJson
     * @return
     * @throws ClassNotFoundException
     */
    private Object[] handleParam(String rawJson) throws ClassNotFoundException {
        JSONArray paramArray = JSONArray.parseArray(rawJson);
        Object[] param = new Object[paramArray.size()];
        for (int i = 0; i < paramArray.size(); i++) {
            JSONObject temp = paramArray.getJSONObject(i);
            String clazz = temp.getString("clazz");
            param[i] = temp.getObject("param", Class.forName(clazz));
        }
        return param;
    }

    /**
     * 处理cglib方式代理的对象
     * @param beanInstance
     * @return
     * @throws Exception
     */
    private Object handleProxyObject(Object beanInstance) throws Exception {

        // 判断是否为cglib方式代理
        if (AopUtils.isCglibProxy(beanInstance)) {

            Field h = beanInstance.getClass().getDeclaredField("CGLIB$CALLBACK_0");
            h.setAccessible(true);
            Object dynamicAdvisedInterceptor = h.get(beanInstance);

            Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
            advised.setAccessible(true);
            return  (((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource()).getTarget();
        }
        return beanInstance;
    }
}
