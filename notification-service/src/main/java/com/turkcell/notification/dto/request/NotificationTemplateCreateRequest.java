package com.turkcell.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public class NotificationTemplateCreateRequest {

    @NotBlank(message = "Code cannot be blank")
    private String code;

    @NotBlank(message = "Channel code cannot be blank")
    private String channelCode;

    @NotBlank(message = "Locale cannot be blank")
    private String locale;

    private String subject;

    @NotBlank(message = "Body template cannot be blank")
    private String bodyTemplate;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
}
