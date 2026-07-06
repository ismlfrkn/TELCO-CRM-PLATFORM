package com.turkcell.notification.exception;

public class NotificationTemplateNotFoundException extends RuntimeException {
    public NotificationTemplateNotFoundException(String message) {
        super(message);
    }
}
