package com.execise.sagat.notification.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.payloads.NotificationDispatchRequest;

@Service
public class NotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private final RestClient restClient = RestClient.builder().build();
    private final int maxAttempts;

    public NotificationDispatcher(@Value("${notification.dispatch.max-attempts:3}") int maxAttempts) {
        this.maxAttempts = Math.max(2, maxAttempts);
    }

    public void dispatch(Notification n) {
        NotificationDispatchRequest request = new NotificationDispatchRequest(
                n.getId(),
                n.getRecipient(),
                n.getChannel(),
                n.getSubject(),
                n.getBody(),
                n.getPriority(),
                n.getMetadata());

        if (request.channel() == NotificationChannel.LOG) {
                log.atInfo()
                    .addKeyValue("event", "notification.sent")
                    .addKeyValue("notificationId", request.id())
                    .addKeyValue("channel", request.channel())
                    .addKeyValue("attempt", 1)
                    .addKeyValue("status", "DISPATCHED")
                    .addKeyValue("recipient", request.recipient())
                    .addKeyValue("subject", request.subject())
                    .addKeyValue("body", request.body())
                    .log("Notification dispatched");
            return;
        }
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.atInfo()
                    .addKeyValue("event", "notification.dispatch_attempt")
                    .addKeyValue("notificationId", request.id())
                    .addKeyValue("channel", request.channel())
                    .addKeyValue("attempt", attempt)
                    .addKeyValue("status", "PROCESSING")
                    .log("Notification dispatch attempt");
            try {
                restClient.post().uri(request.recipient()).contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve().toBodilessEntity();
                log.atInfo()
                        .addKeyValue("event", "notification.sent")
                        .addKeyValue("notificationId", request.id())
                        .addKeyValue("channel", request.channel())
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("status", "DISPATCHED")
                        .log("Notification dispatched");
                return;
            } catch (RuntimeException ex) {
                last = ex;
                log.atWarn()
                        .addKeyValue("event", "notification.dispatch_attempt_failed")
                        .addKeyValue("notificationId", request.id())
                        .addKeyValue("channel", request.channel())
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("status", "PROCESSING")
                        .setCause(ex)
                        .log("Notification dispatch attempt failed");
            }
        }
            
        throw new IllegalStateException("No se pudo entregar la notificacion despues de " + maxAttempts + " intentos", last);
    }
}
