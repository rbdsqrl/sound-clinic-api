package com.simplehearing.enrollment.job;

import com.simplehearing.enrollment.service.RenewalReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Checks hourly whether any program's renewal reminder is due (morning and evening, in each organisation's timezone). */
@Component
public class RenewalReminderJob {

    private final RenewalReminderService renewalReminderService;

    public RenewalReminderJob(RenewalReminderService renewalReminderService) {
        this.renewalReminderService = renewalReminderService;
    }

    @Scheduled(cron = "0 5 * * * *")
    public void sendDueReminders() {
        renewalReminderService.sendDueReminders();
    }
}
