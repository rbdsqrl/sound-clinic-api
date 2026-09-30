package com.simplehearing.subscription.job;

import com.simplehearing.subscription.service.PaymentReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Emails parents a reminder every 2 days while a subscription's payment is still pending. */
@Component
public class PaymentReminderJob {

    private final PaymentReminderService paymentReminderService;

    public PaymentReminderJob(PaymentReminderService paymentReminderService) {
        this.paymentReminderService = paymentReminderService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDueReminders() {
        paymentReminderService.sendDueReminders();
    }
}
