package com.wzp.module.quartz.event;

import org.springframework.context.ApplicationEvent;

public class JobLogEvent extends ApplicationEvent {


    public JobLogEvent(Object source) {
        super(source);
    }
}
