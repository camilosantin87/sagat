package com.execise.sagat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createsPendingNotificationWithValidApiKey() throws Exception {
        mvc.perform(post("/api/notifications")
                        .header("X-API-Key", "change-me")
                        .contentType("application/json")
                        .content("{\"recipient\":\"log-recipient\",\"channel\":\"LOG\",\"subject\":\"hello\",\"body\":\"world\",\"priority\":\"LOW\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsNotificationRequestWithoutApiKey() throws Exception {
        mvc.perform(post("/api/notifications")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
