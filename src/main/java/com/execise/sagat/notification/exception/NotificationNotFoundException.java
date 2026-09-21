package com.execise.sagat.notification.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(UUID id) { super("Notificacion no encontrada: " + id); }
}
