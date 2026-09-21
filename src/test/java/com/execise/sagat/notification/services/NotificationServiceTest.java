package com.execise.sagat.notification.services;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;
import com.execise.sagat.notification.enums.NotificationStatus;
import com.execise.sagat.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    NotificationService service;

    @MockitoBean
    NotificationRepository repository;

    @MockitoBean
    NotificationDispatcher dispatcher;

    @Value("${notification.retry.max-attempts:2}")
    int configuredMaxAttempts;

    @BeforeEach
    void resetMocks() {
        reset(repository, dispatcher);
    }

    @Test
    void processesLogNotificationSuccessfully() {
        Notification notification = notification(NotificationChannel.LOG);
        repositoryReturning(notification);

        service.processQueue();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        verify(dispatcher).dispatch(notification);
        verify(repository, times(2)).save(notification);
    }

    @Test
    void processesServiceNotificationSuccessfully() {
        Notification notification = notification(NotificationChannel.SERVICE);
        repositoryReturning(notification);

        service.processQueue();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        verify(dispatcher).dispatch(notification);
    }

    @Test
    void marksLogNotificationAsFailedWhenDispatchThrows() {
        assertFailedDispatch(NotificationChannel.LOG);
    }

    @Test
    void marksServiceNotificationAsFailedWhenDispatchThrows() {
        assertFailedDispatch(NotificationChannel.SERVICE);
    }

    @Test
    void getsAnExistingNotification() {
        Notification notification = notification(NotificationChannel.LOG);
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertEquals(notification, service.get(notification.getId()));
    }

    @Test
    void throwsWhenGettingAnUnknownNotification() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.get(id));
    }

    private void assertFailedDispatch(NotificationChannel channel) {
        Notification notification = notification(channel);
        repositoryReturning(notification);
        doThrow(new IllegalStateException("dispatch unavailable"))
                .when(dispatcher).dispatch(notification);

        service.processQueue();

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        assertEquals("dispatch unavailable", notification.getLastError());
    }

    private void repositoryReturning(Notification notification) {
        int maxAttempts = Math.max(2, configuredMaxAttempts);
        when(repository.findTop50ByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), maxAttempts))
                .thenReturn(List.of(notification));
    }

    private Notification notification(NotificationChannel channel) {
        return new Notification("recipient", channel, "subject", "body",
                NotificationPriority.HIGH, Map.of());
    }
}
