package com.turkcell.ticket.service;

import com.turkcell.ticket.dto.request.SlaCreateRequest;
import com.turkcell.ticket.dto.response.SlaResponse;
import com.turkcell.ticket.entity.Sla;
import com.turkcell.ticket.exception.DuplicateSlaException;
import com.turkcell.ticket.exception.SlaNotFoundException;
import com.turkcell.ticket.mapper.SlaMapper;
import com.turkcell.ticket.repository.SlaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlaServiceTest {

    private SlaRepository slaRepository;
    private SlaService slaService;

    @BeforeEach
    void setUp() {
        slaRepository = mock(SlaRepository.class);
        SlaMapper slaMapper = Mappers.getMapper(SlaMapper.class);
        slaService = new SlaService(slaRepository, slaMapper);

        when(slaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createSla_withNewCombination_succeeds() {
        when(slaRepository.existsByCategoryAndPriority("TECHNICAL", "HIGH")).thenReturn(false);

        SlaCreateRequest request = new SlaCreateRequest();
        request.setCategory("TECHNICAL");
        request.setPriority("HIGH");
        request.setResponseTime(15);
        request.setResolutionTime(240);

        SlaResponse response = slaService.createSla(request);

        assertThat(response.getCategory()).isEqualTo("TECHNICAL");
        assertThat(response.getResolutionTime()).isEqualTo(240);
    }

    @Test
    void createSla_withExistingCombination_throwsDuplicateSlaException() {
        when(slaRepository.existsByCategoryAndPriority("TECHNICAL", "HIGH")).thenReturn(true);

        SlaCreateRequest request = new SlaCreateRequest();
        request.setCategory("TECHNICAL");
        request.setPriority("HIGH");
        request.setResponseTime(15);
        request.setResolutionTime(240);

        assertThatThrownBy(() -> slaService.createSla(request)).isInstanceOf(DuplicateSlaException.class);
        verify(slaRepository, never()).save(any());
    }

    @Test
    void getSlaByCategoryAndPriority_whenMissing_throwsSlaNotFoundException() {
        when(slaRepository.findByCategoryAndPriority("GHOST", "LOW")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slaService.getSlaByCategoryAndPriority("GHOST", "LOW"))
                .isInstanceOf(SlaNotFoundException.class);
    }

    @Test
    void getSlaByCategoryAndPriority_whenExists_returnsSla() {
        Sla sla = new Sla();
        sla.setCategory("BILLING");
        sla.setPriority("MEDIUM");
        when(slaRepository.findByCategoryAndPriority("BILLING", "MEDIUM")).thenReturn(Optional.of(sla));

        Sla result = slaService.getSlaByCategoryAndPriority("BILLING", "MEDIUM");

        assertThat(result.getCategory()).isEqualTo("BILLING");
    }
}
