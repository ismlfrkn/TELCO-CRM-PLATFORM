package com.turkcell.customer.mapper;

import com.turkcell.customer.dto.response.DocumentResponse;
import com.turkcell.customer.entity.Document;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-03T12:49:34+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class DocumentMapperImpl implements DocumentMapper {

    @Override
    public DocumentResponse toResponse(Document document) {
        if ( document == null ) {
            return null;
        }

        DocumentResponse documentResponse = new DocumentResponse();

        documentResponse.setId( document.getId() );
        documentResponse.setType( document.getType() );
        documentResponse.setFileRef( document.getFileRef() );
        documentResponse.setVerifiedAt( document.getVerifiedAt() );

        return documentResponse;
    }
}
