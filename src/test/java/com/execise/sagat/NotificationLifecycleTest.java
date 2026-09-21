package com.execise.sagat;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;
import com.execise.sagat.notification.enums.NotificationStatus;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationLifecycleTest {

    @Test
    void startsPendingWithoutAttemptsAndCopiesMetadata() {
        Map<String, String> metadata = Map.of("traceId", "abc");
        Notification notification = new Notification(
                "recipient", NotificationChannel.LOG, "subject", "body",
                NotificationPriority.HIGH, metadata);

        assertAll(
                () -> assertEquals(NotificationStatus.PENDING, notification.getStatus()),
                () -> assertEquals(0, notification.getAttempts()),
                () -> assertEquals(metadata, notification.getMetadata()),
                () -> assertNotNull(notification.getCreatedAt()),
                () -> assertNotNull(notification.getUpdatedAt()));
    }

    @Test
    void marksNotificationAsProcessingAndCountsEachAttempt() {
        Notification notification = notification();
        var initialUpdatedAt = notification.getUpdatedAt();

        notification.markProcessing();
        notification.markProcessing();

        assertEquals(NotificationStatus.PROCESSING, notification.getStatus());
        assertEquals(2, notification.getAttempts());
        assertTrue(!notification.getUpdatedAt().isBefore(initialUpdatedAt));
    }

    @Test
    void marksNotificationAsSentAndClearsPreviousError() {
        Notification notification = notification();
        notification.markProcessing();
        notification.markFailed("temporary failure");

        notification.markSent();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        assertTrue(notification.getLastError() == null);
    }

    @Test
    void recordsAnAttemptWhenDeliveryFails() {
        Notification notification = notification();

        notification.markProcessing();
        notification.markFailed("destination unavailable");

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        assertEquals("destination unavailable", notification.getLastError());
    }

    private Notification notification() {
        return new Notification("recipient", NotificationChannel.LOG, "subject", "body",
                NotificationPriority.HIGH, Map.of("traceId", "abc"));
    }
}
