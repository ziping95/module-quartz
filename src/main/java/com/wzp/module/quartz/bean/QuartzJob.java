package com.wzp.module.quartz.bean;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wzp.module.core.base.bean.AbstractBaseBean;
import com.wzp.module.core.utils.SpringUtil;
import com.wzp.module.quartz.proxy.CglibProxy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

// 该注解作用为在equals方法中也要判断父类中的属性，完全相同才会返回true
@EqualsAndHashCode(callSuper = true)
@Data
public class QuartzJob extends AbstractBaseBean {

    private String alias;

    private String jobName;

    private String jobGroup;

    private String cron;

    private String invokeClass;

    private String methodName;

    private String methodParam;

    private String springBeanName;

    /**
     * 开启任务
     * @throws Exception
     */
    public void runJob () throws Exception {
        MethodInvokingJobDetailFactoryBean jobDetail = new MethodInvokingJobDetailFactoryBean();
        // 禁止并发
        jobDetail.setConcurrent(false);
        Class clazz = Class.forName(this.getInvokeClass());
        jobDetail.setName(this.getJobName());
        jobDetail.setGroup(this.getJobGroup());
        jobDetail.setTargetClass(clazz);
        jobDetail.setTargetMethod(this.getMethodName());
        String beanName = this.getSpringBeanName();

        // 如果beanName为空则默认类名小写
        if (StringUtils.isEmpty(beanName)) {
            beanName = handleBeanName(this.getInvokeClass());
        }
        Object targetObject = SpringUtil.getBean(beanName);

        // 使用cglib代理方式做方法增强
        Object cglibTarget = creatProxyedObj(targetObject);

        if (targetObject == null) {
            throw new Exception("获取定时任务bean失败");
        }

        jobDetail.setTargetObject(cglibTarget);
        String paramJson = this.getMethodParam();

        // 支持全类型参数
        if (StringUtils.isNotEmpty(paramJson)) {
            jobDetail.setArguments(handleParam(paramJson));
        }

        jobDetail.afterPropertiesSet();

        // 设置定时器
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(this.getJobName(), this.getJobGroup())
                .withSchedule(CronScheduleBuilder.cronSchedule(this.getCron())).startNow().build();

        // 设置调度器
        Scheduler scheduler = (Scheduler) SpringUtil.getBean("scheduler");
        scheduler.scheduleJob(jobDetail.getObject(), trigger);
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
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
            // 判断是不是对象
            if (JSONObject.isValidObject(paramArray.get(0).toString())) {
                JSONObject temp = paramArray.getJSONObject(i);
                String clazz = temp.getString("clazz");
                param[i] = temp.getObject("param", Class.forName(clazz));
            } else {
                param[i] = paramArray.get(i);
            }

        }
        return param;
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

    // 根据一个类型产生代理类
    private Object creatProxyedObj(Object target) {
        Class clazz;
        boolean isCglib = AopUtils.isCglibProxy(target);
        // 如果是cglib方式代理的对象
        if (isCglib) {
            clazz = target.getClass().getSuperclass();
        } else {
            clazz = target.getClass();
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new CglibProxy(target,this));
        return enhancer.create();
    }


    public static void main(String[] args) throws InterruptedException {
        String cron = "5/5 * * * * ?";
        CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(cron);
        System.out.println(CronSequenceGenerator.isValidExpression(cron));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < 10; i++) {
            System.out.println(sdf.format(new Date()) + "------->" + sdf.format(cronSequenceGenerator.next(new Date())));
            Thread.sleep(1000);
        }
    }

    public static String getCronToDate(String cron) {
        String format = null;
        StringBuffer stringBuffer = new StringBuffer(cron);
        int lastIndexOf = stringBuffer.lastIndexOf("/");
        stringBuffer.deleteCharAt(lastIndexOf);
        stringBuffer.deleteCharAt(lastIndexOf);
        String dateFormat = "ss mm HH dd MM ? yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        try {
            Date date = sdf.parse(stringBuffer.toString());
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format = sdf.format(date);
        } catch (Exception e) {
            return null;
        }
        return format;
    }

}
