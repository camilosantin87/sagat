package com.execise.sagat.notification.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.enums.NotificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @EntityGraph(attributePaths = "metadata")
    Optional<Notification> findById(UUID id);

    List<Notification> findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}
