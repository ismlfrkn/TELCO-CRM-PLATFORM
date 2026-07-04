package com.turkcell.customer.service;

import com.turkcell.customer.dto.request.DocumentCreateRequest;
import com.turkcell.customer.dto.response.DocumentResponse;
import com.turkcell.customer.entity.Customer;
import com.turkcell.customer.entity.Document;
import com.turkcell.customer.exception.CustomerNotFoundException;
import com.turkcell.customer.mapper.DocumentMapper;
import com.turkcell.customer.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private CustomerService customerService;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        customerService = mock(CustomerService.class);
        DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);
        documentService = new DocumentService(documentRepository, customerService, documentMapper);

        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addDocument_whenCustomerExists_savesDocumentLinkedToCustomer() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        when(customerService.getCustomerById(customer.getId())).thenReturn(customer);

        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setType("ID_CARD");
        request.setFileRef("s3://bucket/id-card.pdf");

        DocumentResponse response = documentService.addDocument(customer.getId(), request);

        assertThat(response.getType()).isEqualTo("ID_CARD");
        assertThat(response.getFileRef()).isEqualTo("s3://bucket/id-card.pdf");
    }

    @Test
    void addDocument_whenCustomerMissing_throwsCustomerNotFoundException() {
        UUID customerId = UUID.randomUUID();
        when(customerService.getCustomerById(customerId))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: " + customerId));

        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setType("ID_CARD");
        request.setFileRef("s3://bucket/id-card.pdf");

        assertThatThrownBy(() -> documentService.addDocument(customerId, request))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void getDocuments_returnsMappedResponsesForCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerService.getCustomerById(customerId)).thenReturn(customer);

        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setCustomer(customer);
        document.setType("PASSPORT");
        document.setFileRef("s3://bucket/passport.pdf");
        when(documentRepository.findByCustomerId(customerId)).thenReturn(List.of(document));

        List<DocumentResponse> responses = documentService.getDocuments(customerId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getType()).isEqualTo("PASSPORT");
    }
}
