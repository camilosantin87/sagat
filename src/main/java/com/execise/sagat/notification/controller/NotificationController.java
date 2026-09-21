package com.execise.sagat.notification.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.payloads.NotificationRequest;
import com.execise.sagat.notification.payloads.NotificationResponse;
import com.execise.sagat.notification.services.NotificationService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired 
    private NotificationService service;

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        Notification n = service.create(request);
        return ResponseEntity.accepted().location(URI.create("/api/notifications/" + n.getId()))
                .body(NotificationResponse.from(n));
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) { return NotificationResponse.from(service.get(id)); }
}
