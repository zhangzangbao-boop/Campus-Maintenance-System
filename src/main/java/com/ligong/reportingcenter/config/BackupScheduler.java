package com.ligong.reportingcenter.config;

import com.ligong.reportingcenter.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;

    /**
     * 每天凌晨2点执行一次自动备份
     * cron表达式: 秒 分 时 日 月 周
     * 0 0 2 * * ? 表示每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        try {
            log.info("开始执行定时备份任务...");
            backupService.performBackup();
            log.info("定时备份任务执行成功");
        } catch (Exception e) {
            log.error("定时备份任务执行失败", e);
        }
    }
}


