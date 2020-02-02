package com.wzp.module.quartz.config;

import org.quartz.Scheduler;
import org.quartz.simpl.SimpleJobFactory;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;

/**
 * Quartz核心配置类
 */
@Configuration
public class QuartzConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public JobFactory jobFactory() {
        return new SimpleJobFactory();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        //可选,QuartzScheduler启动时更新己存在的Job,这样就不用每次修改targetObject后删除qrtz_job_details表对应记录
        factoryBean.setOverwriteExistingJobs(true); // 暂时无用
        factoryBean.setAutoStartup(true); // 自动启动
        factoryBean.setJobFactory(jobFactory());
        return factoryBean;
    }

    @Bean(name = "scheduler")
    public Scheduler scheduler() throws IOException {
        return schedulerFactoryBean().getScheduler();
    }

}
