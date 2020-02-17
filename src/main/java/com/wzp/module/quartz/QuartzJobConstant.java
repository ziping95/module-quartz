package com.wzp.module.quartz;

import com.wzp.module.quartz.test.CglibProxy;
import com.wzp.module.quartz.test.TestCglib;
import org.springframework.cglib.core.DebuggingClassWriter;

import javax.print.DocFlavor;

public class QuartzJobConstant {

    public final static String JOB_RUN = "1";
    public final static String JOB_STOP = "0";

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        CglibProxy cglibProxy = new CglibProxy();
        TestCglib testCglib = (TestCglib) cglibProxy.CreatProxyedObj(TestCglib.class);
        testCglib.sayHello("cglib代理");

        long end = System.currentTimeMillis();
        System.out.println("运行时间为：" + String.valueOf(end - start));

    }
}
