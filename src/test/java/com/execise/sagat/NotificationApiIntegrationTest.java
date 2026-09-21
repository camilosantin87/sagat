package com.execise.sagat;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;
import com.execise.sagat.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
