package com.turkcell.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class TicketCommentCreateRequest {

    @NotNull(message = "Author id cannot be null")
    private UUID authorId;

    @NotBlank(message = "Body cannot be blank")
    private String body;

    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
