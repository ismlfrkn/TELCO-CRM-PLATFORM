package com.turkcell.notification.exception;

public class DuplicateNotificationTemplateException extends RuntimeException {
    public DuplicateNotificationTemplateException(String message) {
        super(message);
    }
}
