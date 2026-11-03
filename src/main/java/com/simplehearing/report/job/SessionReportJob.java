package com.simplehearing.report.job;

import com.simplehearing.report.service.SessionReportService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Fires the session reports once each: the day report at 11:40 AM and the day end report at 1:00 PM for the previous day ({@code app.timezone}, default Asia/Kolkata). */
@Component
public class SessionReportJob {

    private final SessionReportService sessionReportService;

    public SessionReportJob(SessionReportService sessionReportService) {
        this.sessionReportService = sessionReportService;
    }

    @Scheduled(cron = "0 40 11 * * *", zone = "${app.timezone:Asia/Kolkata}")
    public void sendDayReport() {
        sessionReportService.sendDayReport();
    }

    @Scheduled(cron = "0 0 13 * * *", zone = "${app.timezone:Asia/Kolkata}")
    public void sendNightReport() {
        sessionReportService.sendNightReport();
    }
}
