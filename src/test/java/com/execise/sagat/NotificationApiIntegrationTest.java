package com.execise.sagat;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;
import com.execise.sagat.notification.enums.NotificationStatus;
import com.execise.sagat.notification.repository.NotificationRepository;
import com.execise.sagat.notification.services.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    NotificationRepository repository;

    @Autowired
    NotificationService service;

    @Test
    void createsAndPersistsPendingNotificationWithValidApiKey() throws Exception {
        MvcResult result = mvc.perform(post("/api/notifications")
                        .header("X-API-Key", "change-me")
                        .contentType("application/json")
                        .content("""
                                {
                                  "recipient": "log-recipient",
                                  "channel": "LOG",
                                  "subject": "hello",
                                  "body": "world",
                                  "priority": "LOW",
                                  "metadata": {"traceId": "abc-123"}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location);
        assertTrue(location.startsWith("/api/notifications/"));

        UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        Notification notification = repository.findById(id).orElseThrow();

        assertEquals("log-recipient", notification.getRecipient());
        assertEquals(NotificationChannel.LOG, notification.getChannel());
        assertEquals("hello", notification.getSubject());
        assertEquals("world", notification.getBody());
        assertEquals(NotificationPriority.LOW, notification.getPriority());
        assertEquals("abc-123", notification.getMetadata().get("traceId"));
    }

    @Test
    void rejectsNotificationRequestWithoutApiKey() throws Exception {
        mvc.perform(post("/api/notifications")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidApiKey() throws Exception {
        mvc.perform(post("/api/notifications")
                        .header("X-API-Key", "invalid")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidNotificationPayload() throws Exception {
        mvc.perform(post("/api/notifications")
                        .header("X-API-Key", "change-me")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"))
                .andExpect(jsonPath("$.fields.recipient").exists())
                .andExpect(jsonPath("$.fields.channel").exists())
                .andExpect(jsonPath("$.fields.subject").exists())
                .andExpect(jsonPath("$.fields.body").exists())
                .andExpect(jsonPath("$.fields.priority").exists());
    }

    @Test
    void returnsCreatedNotificationWhenItIsRequested() throws Exception {
        MvcResult result = mvc.perform(post("/api/notifications")
                        .header("X-API-Key", "change-me")
                        .contentType("application/json")
                        .content("{\"recipient\":\"recipient\",\"channel\":\"LOG\",\"subject\":\"subject\",\"body\":\"body\",\"priority\":\"MEDIUM\"}"))
                .andExpect(status().isAccepted())
                .andReturn();

        String id = result.getResponse().getHeader(HttpHeaders.LOCATION).substring(
                result.getResponse().getHeader(HttpHeaders.LOCATION).lastIndexOf('/') + 1);

        mvc.perform(get("/api/notifications/{id}", id)
                        .header("X-API-Key", "change-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returnsNotFoundForUnknownNotification() throws Exception {
        mvc.perform(get("/api/notifications/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("X-API-Key", "change-me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void retriesFailedNotificationAndKeepsItRegistered() {
        Notification notification = repository.save(new Notification(
                "not a valid uri",
                NotificationChannel.SERVICE,
                "subject",
                "body",
                NotificationPriority.HIGH,
                Map.of()));

        service.processQueue();
        Notification afterFirstAttempt = repository.findById(notification.getId()).orElseThrow();
        assertEquals(NotificationStatus.FAILED, afterFirstAttempt.getStatus());
        assertEquals(1, afterFirstAttempt.getAttempts());
        assertNotNull(afterFirstAttempt.getLastError());

        service.processQueue();
        Notification afterRetry = repository.findById(notification.getId()).orElseThrow();
        assertEquals(NotificationStatus.FAILED, afterRetry.getStatus());
        assertEquals(2, afterRetry.getAttempts());
        assertNotNull(afterRetry.getLastError());
    }
}
