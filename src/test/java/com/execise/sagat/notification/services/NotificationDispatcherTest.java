package com.execise.sagat.notification.services;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@TestPropertySource(properties = "notification.dispatch.max-attempts=2")
class NotificationDispatcherTest {

    @Autowired
    NotificationDispatcher dispatcher;

    @Autowired
    RestClient.Builder restClientBuilder;

    @Value("${notification.dispatch.max-attempts}")
    int configuredMaxAttempts;

    private MockRestServiceServer server;

    @BeforeEach
    void setUpServer() {
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void dispatchesLogNotificationWithoutCallingAnHttpEndpoint() {
        assertEquals(2, configuredMaxAttempts);
        assertDoesNotThrow(() -> dispatcher.dispatch(notification(NotificationChannel.LOG, "log-recipient")));
    }

    @Test
    void dispatchesServiceNotificationWhenTheEndpointSucceeds() {
        server.expect(requestTo("http://service.test/notifications"))
                .andRespond(withSuccess());

        assertDoesNotThrow(() -> dispatcher.dispatch(
                notification(NotificationChannel.SERVICE, "http://service.test/notifications")));
    }

    @Test
    void retriesServiceDispatchAfterATemporaryFailure() {
        server.expect(requestTo("http://service.test/notifications"))
                .andRespond(withServerError());
        server.expect(requestTo("http://service.test/notifications"))
                .andRespond(withSuccess());

        assertDoesNotThrow(() -> dispatcher.dispatch(
                notification(NotificationChannel.SERVICE, "http://service.test/notifications")));
    }

    @Test
    void throwsAfterAllServiceDispatchAttemptsFail() {
        server.expect(requestTo("http://service.test/notifications"))
                .andRespond(withServerError());
        server.expect(requestTo("http://service.test/notifications"))
                .andRespond(withServerError());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(
                notification(NotificationChannel.SERVICE, "http://service.test/notifications")));
    }

    private Notification notification(NotificationChannel channel, String recipient) {
        return new Notification(recipient, channel, "subject", "body",
                NotificationPriority.HIGH, Map.of("traceId", "unit-test"));
    }
}
