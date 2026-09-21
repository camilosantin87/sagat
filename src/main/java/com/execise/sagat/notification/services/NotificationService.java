package com.execise.sagat.notification.services;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationStatus;
import com.execise.sagat.notification.exception.NotificationNotFoundException;
import com.execise.sagat.notification.payloads.NotificationRequest;
import com.execise.sagat.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    @Autowired 
    private NotificationRepository repository;
    @Autowired 
    private NotificationDispatcher dispatcher;
    @Value("${notification.retry.max-attempts:2}")
    private int maxQueueAttempts;

    @Transactional
    public Notification create(NotificationRequest request) {
        return repository.save(new Notification(request.recipient(), request.channel(), request.subject(),
                request.body(), request.priority(), request.metadata()));
    }

    @Transactional(readOnly = true)
    public Notification get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    @Scheduled(
            initialDelayString = "${notification.worker.initial-delay-ms:30000}",
            fixedDelayString = "${notification.worker.fixed-delay-ms:30000}")
    @Transactional
    public void processQueue() {
        int maxAttempts = Math.max(2, maxQueueAttempts);
        List<Notification> pending = repository.findTop50ByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), maxAttempts);
        for (Notification n : pending) {
            n.markProcessing(); repository.save(n);
            try {
                dispatcher.dispatch(n);
                n.markSent();
                log.atInfo().addKeyValue("event", "notification.completed").addKeyValue("notificationId", n.getId()).log("Notification completed");
            } catch (RuntimeException ex) {
                n.markFailed(ex.getMessage());
                log.atError().addKeyValue("event", "notification.failed").addKeyValue("notificationId", n.getId()).setCause(ex).log("Notification failed");
            }
            repository.save(n);
        }
    }
}
