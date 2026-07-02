package com.turkcell.customer.service;

import com.turkcell.customer.dto.request.DocumentCreateRequest;
import com.turkcell.customer.dto.response.DocumentResponse;
import com.turkcell.customer.entity.Customer;
import com.turkcell.customer.entity.Document;
import com.turkcell.customer.mapper.DocumentMapper;
import com.turkcell.customer.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CustomerService customerService;
    private final DocumentMapper documentMapper;

    public DocumentService(DocumentRepository documentRepository, CustomerService customerService, DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.customerService = customerService;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public DocumentResponse addDocument(UUID customerId, DocumentCreateRequest request) {
        Customer customer = customerService.getCustomerById(customerId);

        Document document = new Document();
        document.setCustomer(customer);
        document.setType(request.getType());
        document.setFileRef(request.getFileRef());

        return documentMapper.toResponse(documentRepository.save(document));
    }

    public List<DocumentResponse> getDocuments(UUID customerId) {
        customerService.getCustomerById(customerId);
        return documentRepository.findByCustomerId(customerId).stream()
                .map(documentMapper::toResponse)
                .toList();
    }
}
