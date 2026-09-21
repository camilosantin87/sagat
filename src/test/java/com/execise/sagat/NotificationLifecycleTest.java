package com.execise.sagat;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;
import com.execise.sagat.notification.enums.NotificationStatus;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationLifecycleTest {

    @Test
    void recordsAnAttemptWhenDeliveryFails() {
        Notification notification = new Notification(
                "recipient",
                NotificationChannel.LOG,
                "subject",
                "body",
                NotificationPriority.HIGH,
                Map.of("traceId", "abc"));

        notification.markProcessing();
        notification.markFailed("destination unavailable");

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        assertEquals("destination unavailable", notification.getLastError());
    }
}
