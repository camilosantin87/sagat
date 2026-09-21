package com.execise.sagat.notification.payloads;

import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;

import java.util.Map;
import java.util.UUID;

public record NotificationDispatchRequest(
        UUID id,
        String recipient,
        NotificationChannel channel,
        String subject,
        String body,
        NotificationPriority priority,
        Map<String, String> metadata) {
}
