package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Component;

/** 入口二：定时任务。同样是「只认识 OrderService」。 */
@Component
public class ReportJob {

    private final OrderService service;

    public ReportJob(OrderService service) {
        this.service = service;
        Trace.constructed("ReportJob", null);
    }

    public String run() {
        return "JOB 汇总完成 ← " + service.place(1001L);
    }
}
