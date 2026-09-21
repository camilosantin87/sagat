package com.execise.sagat.notification.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.execise.sagat.notification.enums.NotificationChannel;
import com.execise.sagat.notification.enums.NotificationPriority;
import com.execise.sagat.notification.enums.NotificationStatus;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notification_queue", columnList = "status,created_at"))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String recipient;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationChannel channel;
    @Column(nullable = false) private String subject;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationStatus status = NotificationStatus.PENDING;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", columnDefinition = "text") private String lastError;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @ElementCollection @CollectionTable(name = "notification_metadata", joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "metadata_key") @Column(name = "metadata_value")
    private Map<String, String> metadata = new LinkedHashMap<>();

    protected Notification() { }
    public Notification(String recipient, NotificationChannel channel, String subject, String body,
                        NotificationPriority priority, Map<String, String> metadata) {
        this.recipient = recipient; this.channel = channel; this.subject = subject; this.body = body;
        this.priority = priority; if (metadata != null) this.metadata.putAll(metadata);
    }
    public void markProcessing() { status = NotificationStatus.PROCESSING; attempts++; touch(); }
    public void markSent() { status = NotificationStatus.SENT; lastError = null; touch(); }
    public void markFailed(String error) { status = NotificationStatus.FAILED; lastError = error; touch(); }
    private void touch() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getRecipient() { return recipient; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationPriority getPriority() { return priority; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Map<String, String> getMetadata() { return metadata; }
}
