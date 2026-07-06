package com.turkcell.ticket.service;

import com.turkcell.ticket.dto.request.SlaCreateRequest;
import com.turkcell.ticket.dto.response.SlaResponse;
import com.turkcell.ticket.entity.Sla;
import com.turkcell.ticket.exception.DuplicateSlaException;
import com.turkcell.ticket.exception.SlaNotFoundException;
import com.turkcell.ticket.mapper.SlaMapper;
import com.turkcell.ticket.repository.SlaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaService {

    private final SlaRepository slaRepository;
    private final SlaMapper slaMapper;

    public SlaService(SlaRepository slaRepository, SlaMapper slaMapper) {
        this.slaRepository = slaRepository;
        this.slaMapper = slaMapper;
    }

    @Transactional
    public SlaResponse createSla(SlaCreateRequest request) {
        if (slaRepository.existsByCategoryAndPriority(request.getCategory(), request.getPriority())) {
            throw new DuplicateSlaException(
                    "SLA already exists for category=" + request.getCategory() + ", priority=" + request.getPriority());
        }

        Sla sla = new Sla();
        sla.setCategory(request.getCategory());
        sla.setPriority(request.getPriority());
        sla.setResponseTime(request.getResponseTime());
        sla.setResolutionTime(request.getResolutionTime());

        return slaMapper.toResponse(slaRepository.save(sla));
    }

    public Sla getSlaByCategoryAndPriority(String category, String priority) {
        return slaRepository.findByCategoryAndPriority(category, priority)
                .orElseThrow(() -> new SlaNotFoundException(
                        "SLA not found for category=" + category + ", priority=" + priority));
    }
}
