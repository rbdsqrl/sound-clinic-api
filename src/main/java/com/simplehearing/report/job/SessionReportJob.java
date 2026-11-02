package com.simplehearing.report.job;

import com.simplehearing.report.service.SessionReportService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every minute, checks whether any organisation's local clock is at a session-report time (11:40 AM day report, 11:00 PM night report). */
@Component
public class SessionReportJob {

    private final SessionReportService sessionReportService;

    public SessionReportJob(SessionReportService sessionReportService) {
        this.sessionReportService = sessionReportService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendDueReports() {
        sessionReportService.sendDueReports();
    }
}
