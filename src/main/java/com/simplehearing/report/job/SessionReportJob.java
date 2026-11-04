package com.simplehearing.report.job;

import com.simplehearing.report.service.SessionReportService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Fires the session reports once each: the day end report at 10:30 AM for the previous day and the day report at 10:35 AM ({@code app.timezone}, default Asia/Kolkata). */
@Component
public class SessionReportJob {

    private final SessionReportService sessionReportService;

    public SessionReportJob(SessionReportService sessionReportService) {
        this.sessionReportService = sessionReportService;
    }

    @Scheduled(cron = "0 35 10 * * *", zone = "${app.timezone:Asia/Kolkata}")
    public void sendDayReport() {
        sessionReportService.sendDayReport();
    }

    @Scheduled(cron = "0 30 10 * * *", zone = "${app.timezone:Asia/Kolkata}")
    public void sendNightReport() {
        sessionReportService.sendNightReport();
    }
}
