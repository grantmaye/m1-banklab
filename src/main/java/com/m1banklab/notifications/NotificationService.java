package com.m1banklab.notifications;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSystemNotification(String subject, String body) {
        notificationRepository.save(new Notification(null, NotificationChannel.SYSTEM, subject, body));
    }
}
