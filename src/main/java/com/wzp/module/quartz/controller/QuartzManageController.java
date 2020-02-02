package com.wzp.module.quartz.controller;

import com.wzp.module.core.dto.ResultDataModel;
import com.wzp.module.quartz.bean.QuartzJob;
import com.wzp.module.quartz.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/manage/quartz")
public class QuartzManageController {

    @Resource
    private JobService jobService;

    /**
     * 启动定时任务
     * @param jobId
     * @return
     */
    @GetMapping("/run")
    public ResultDataModel runJob(@RequestParam("jobId") String jobId) {
        try {
            jobService.runJob(jobId);
        } catch (Exception e) {
            log.error("启动ID为{}定时任务失败，失败原因为：{}",jobId,e.getMessage());
            e.printStackTrace();
            return ResultDataModel.handleFailureResult("启动失败");
        }
        return ResultDataModel.handleSuccessResult();
    }

    /**
     * 停止定时任务
     * @param jobId
     * @return
     */
    @GetMapping("/stop")
    public ResultDataModel stopJob(@RequestParam("jobId") String jobId) {
        try {
            jobService.stopJob(jobId);
        } catch (Exception e) {
            log.error("启动ID为{}定时任务失败，失败原因为：{}",jobId,e.getMessage());
            e.printStackTrace();
            return ResultDataModel.handleFailureResult("停止失败");
        }
        return ResultDataModel.handleSuccessResult();
    }

    /**
     * 更新定时任务
     * @param quartzJob
     * @return
     */
    @PutMapping("/update")
    public ResultDataModel updateJob(@RequestBody QuartzJob quartzJob) {
        if(StringUtils.isEmpty(quartzJob.getId())) {
            return ResultDataModel.handleFailureResult("ID不能为空");
        }

        try {
            jobService.updateJob(quartzJob);
            return ResultDataModel.handleSuccessResult();
        } catch (Exception e) {
            log.error("更新ID为{}定时任务失败，错误原因为：{}",quartzJob.getId(),e.getMessage());
            e.printStackTrace();
        }
        return ResultDataModel.handleFailureResult("更新定时任务失败");
    }

    /**
     * 立即触发一次定时任务
     * @param jobId
     * @return
     */
    @GetMapping("/runNow")
    public ResultDataModel runNow(@RequestParam("jobId") String jobId) {
        try {
            jobService.runJobNow(jobId);
            return ResultDataModel.handleSuccessResult();
        } catch (Exception e) {
            log.error("触发ID为{}的任务失败，失败原因为：{}",jobId,e.getMessage());
            e.printStackTrace();
        }
        return ResultDataModel.handleFailureResult("触发任务失败");
    }

}
