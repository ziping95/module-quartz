package com.wzp.module.quartz.proxy;

import com.wzp.module.core.utils.SpringUtil;
import com.wzp.module.quartz.QuartzJobConstant;
import com.wzp.module.quartz.bean.JobLog;
import com.wzp.module.quartz.bean.QuartzJob;
import com.wzp.module.quartz.event.JobLogEvent;
import com.wzp.module.quartz.service.JobService;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.support.CronSequenceGenerator;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CglibProxy implements MethodInterceptor {

    private final QuartzJob quartzJob;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private JobService jobService;

    private final Object target;

    private final Scheduler scheduler;

    // 该处代码可以将动态代理生成的class文件，存放在你指定的路径下，方便反编译查看内容
    static {
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\class");
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

        // 在调用前先判断当前任务是否已经有过修改
        Map<String,Object> param = new HashMap<>();
        param.put("jobGroup",this.quartzJob.getJobGroup());
        param.put("jobName",this.quartzJob.getJobName());
        QuartzJob quartzJobFromDB = this.jobService.detailsByCondition(param).get(0);
        long newVersions = quartzJobFromDB.getUpdateDate().getTime();
        long oldVersions = this.quartzJob.getUpdateDate().getTime();

        // todo:更新定时任务也应该加入到日志中
        // 如果不等于则更新当前任务
        if (newVersions != oldVersions) {
            new Thread(() -> {
                JobKey jobKey = new JobKey(this.quartzJob.getJobName(),this.quartzJob.getJobGroup());
                try {
                    // 先停止任务
                    this.scheduler.deleteJob(jobKey);
                    // 重新启动任务
                    quartzJobFromDB.runJob();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            // 构建一个分布式锁
            String lock = String.format(QuartzJobConstant.JOB_LOCK,this.quartzJob.getJobName(),this.quartzJob.getJobGroup(),this.quartzJob.getUpdateDate().getTime());
            boolean isLock = redisTemplate.opsForValue().setIfAbsent(lock,lock,5, TimeUnit.SECONDS);
            Object result = null;
            if (isLock) {

                // 构建日志记录对象
                JobLog jobLog = new JobLog();
                jobLog.setName(this.quartzJob.getAlias());
                jobLog.setStartTime(new Date());
                CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(this.quartzJob.getCron());
                jobLog.setNextTime(cronSequenceGenerator.next(jobLog.getStartTime()));
                jobLog.setParam(this.quartzJob.getMethodParam());

                // 执行内容
                try {
                    result = method.invoke(this.target,objects);
                    boolean s = false;
                    jobLog.setStatus("true");
                    return result;
                }catch (Throwable e) {
                    System.out.println("记录异常：" + e.getMessage());
                    jobLog.setStatus("false");
                    if (e instanceof InvocationTargetException) {
                        jobLog.setException(((InvocationTargetException) e ).getTargetException().toString());
                    }
                } finally {
                    jobLog.setAddress(QuartzJobConstant.getLocalIp(true,false).getHostAddress());
                    jobLog.setEndTime(new Date());

                    // 发布记录日志事件
                    SpringUtil.publishEvent(new JobLogEvent(jobLog));
                    // 释放锁
                    redisTemplate.delete(lock);
                }
            }
        }
        return null;
    }

    public CglibProxy(Object target,QuartzJob quartzJob) {
        this.scheduler = (Scheduler) SpringUtil.getBean("scheduler");
        this.jobService = (JobService) SpringUtil.getBean("jobService");
        this.redisTemplate = (RedisTemplate) SpringUtil.getBean("redisTemplate");
        this.target = target;
        this.quartzJob = quartzJob;
    }
}
