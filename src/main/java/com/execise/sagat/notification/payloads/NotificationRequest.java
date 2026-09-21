package com.execise.sagat.notification.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;

public record NotificationRequest(
        @NotBlank(message = "recipient es obligatorio") String recipient,
        @NotNull(message = "channel es obligatorio") NotificationChannel channel,
        @NotBlank(message = "subject es obligatorio") @Size(max = 255) String subject,
        @NotBlank(message = "body es obligatorio") String body,
        @NotNull(message = "priority es obligatorio") NotificationPriority priority,
        Map<String, String> metadata) { }
