package com.wzp.module.quartz.bean;

import com.wzp.module.core.base.bean.AbstractBaseBean;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuartzJob extends AbstractBaseBean {

    private String jobName;

    private String jobGroup;

    private String cron;

    private String invokeClass;

    private String methodName;

    private String methodParam;

    private String springBeanName;

}
