package com.wzp.module.quartz.bean;

import lombok.Data;

import java.util.Date;

@Data
public class JobLog {

    private String id;

    private String name;

    private String status;

    private Date startTime;

    private Date endTime;

    private Date nextTime;

    private String exception;

    private String param;

    private String address;
}
