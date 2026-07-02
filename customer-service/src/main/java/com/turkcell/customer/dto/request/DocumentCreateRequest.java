package com.turkcell.customer.dto.request;

import jakarta.validation.constraints.NotBlank;

public class DocumentCreateRequest {

    @NotBlank(message = "Type cannot be blank")
    private String type;

    @NotBlank(message = "File reference cannot be blank")
    private String fileRef;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFileRef() { return fileRef; }
    public void setFileRef(String fileRef) { this.fileRef = fileRef; }
}
