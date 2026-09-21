package com.execise.sagat.notification.controller;

import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.payloads.ServiceNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-notifications")
public class ServiceNotificationController {

    private static final Logger log = LoggerFactory.getLogger(ServiceNotificationController.class);

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody ServiceNotificationRequest request) {
        if (request.channel() != NotificationChannel.SERVICE) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Service notification received: {}", request.id());
        return ResponseEntity.accepted().build();
    }
}
