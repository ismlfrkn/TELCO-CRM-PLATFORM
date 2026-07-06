package com.turkcell.subscription.service;

import com.turkcell.subscription.entity.MsisdnPool;
import com.turkcell.subscription.exception.NoAvailableMsisdnException;
import com.turkcell.subscription.mapper.MsisdnPoolMapper;
import com.turkcell.subscription.repository.MsisdnPoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MsisdnPoolServiceTest {

    private MsisdnPoolRepository msisdnPoolRepository;
    private MsisdnPoolService msisdnPoolService;

    @BeforeEach
    void setUp() {
        msisdnPoolRepository = mock(MsisdnPoolRepository.class);
        MsisdnPoolMapper msisdnPoolMapper = Mappers.getMapper(MsisdnPoolMapper.class);
        msisdnPoolService = new MsisdnPoolService(msisdnPoolRepository, msisdnPoolMapper);
    }

    @Test
    void allocateNext_whenFreeNumberExists_marksAllocatedAndReturnsIt() {
        MsisdnPool free = new MsisdnPool("905550000001", "FREE", null);
        when(msisdnPoolRepository.lockNextFree()).thenReturn(Optional.of(free));
        when(msisdnPoolRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String allocated = msisdnPoolService.allocateNext();

        assertThat(allocated).isEqualTo("905550000001");

        ArgumentCaptor<MsisdnPool> captor = ArgumentCaptor.forClass(MsisdnPool.class);
        verify(msisdnPoolRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ALLOCATED");
    }

    @Test
    void allocateNext_whenPoolExhausted_throwsNoAvailableMsisdnException() {
        when(msisdnPoolRepository.lockNextFree()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> msisdnPoolService.allocateNext())
                .isInstanceOf(NoAvailableMsisdnException.class);

        verify(msisdnPoolRepository, Mockito.never()).save(any());
    }

    @Test
    void allocateSpecific_whenNumberIsFree_marksAllocated() {
        MsisdnPool free = new MsisdnPool("905550000099", "FREE", null);
        when(msisdnPoolRepository.findById("905550000099")).thenReturn(Optional.of(free));
        when(msisdnPoolRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String allocated = msisdnPoolService.allocateSpecific("905550000099");

        assertThat(allocated).isEqualTo("905550000099");
        assertThat(free.getStatus()).isEqualTo("ALLOCATED");
    }

    @Test
    void allocateSpecific_whenNumberAlreadyAllocated_throws() {
        MsisdnPool notFree = new MsisdnPool("905550000099", "ALLOCATED", null);
        when(msisdnPoolRepository.findById("905550000099")).thenReturn(Optional.of(notFree));

        assertThatThrownBy(() -> msisdnPoolService.allocateSpecific("905550000099"))
                .isInstanceOf(NoAvailableMsisdnException.class);
    }

    @Test
    void allocateSpecific_whenNumberNotInPool_throws() {
        when(msisdnPoolRepository.findById("905550009999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> msisdnPoolService.allocateSpecific("905550009999"))
                .isInstanceOf(NoAvailableMsisdnException.class);
    }

    @Test
    void release_setsStatusBackToFreeAndClearsReservation() {
        MsisdnPool allocated = new MsisdnPool("905550000001", "ALLOCATED", null);
        when(msisdnPoolRepository.findById("905550000001")).thenReturn(Optional.of(allocated));
        when(msisdnPoolRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        msisdnPoolService.release("905550000001");

        assertThat(allocated.getStatus()).isEqualTo("FREE");
        assertThat(allocated.getReservedUntil()).isNull();
    }

    @Test
    void release_whenMsisdnUnknown_doesNothingSilently() {
        when(msisdnPoolRepository.findById("905550000001")).thenReturn(Optional.empty());

        msisdnPoolService.release("905550000001");

        verify(msisdnPoolRepository, Mockito.never()).save(any());
    }
}
