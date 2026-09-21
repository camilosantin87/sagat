package com.execise.sagat.notification.payloads;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationStatus;

public record NotificationResponse(UUID id, NotificationStatus status, int attempts, String lastError,
                                   Instant createdAt, Instant updatedAt, Map<String, String> metadata) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getStatus(), n.getAttempts(), n.getLastError(),
                n.getCreatedAt(), n.getUpdatedAt(), n.getMetadata());
    }
}
