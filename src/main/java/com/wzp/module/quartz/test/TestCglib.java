package com.wzp.module.quartz.test;

import org.springframework.cglib.core.DebuggingClassWriter;

public class TestCglib {

    public void sayHello(String s) {
        System.out.println("hello world, hello " + s);
    }

    private void privateTest () {
        System.out.println("23424");
    }
}
