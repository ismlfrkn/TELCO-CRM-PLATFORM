package com.turkcell.customer.dto.response;

import java.time.Instant;
import java.util.UUID;

public class DocumentResponse {

    private UUID id;
    private String type;
    private String fileRef;
    private Instant verifiedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFileRef() { return fileRef; }
    public void setFileRef(String fileRef) { this.fileRef = fileRef; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
