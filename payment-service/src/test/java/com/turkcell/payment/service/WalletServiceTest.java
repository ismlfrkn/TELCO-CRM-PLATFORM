package com.turkcell.payment.service;

import com.turkcell.payment.dto.request.WalletCreateRequest;
import com.turkcell.payment.dto.request.WalletTopUpRequest;
import com.turkcell.payment.dto.response.WalletResponse;
import com.turkcell.payment.entity.Wallet;
import com.turkcell.payment.exception.InsufficientWalletBalanceException;
import com.turkcell.payment.exception.WalletNotFoundException;
import com.turkcell.payment.mapper.WalletMapper;
import com.turkcell.payment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private WalletRepository walletRepository;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        WalletMapper walletMapper = Mappers.getMapper(WalletMapper.class);
        walletService = new WalletService(walletRepository, walletMapper);

        when(walletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createWallet_withoutInitialBalance_defaultsToZero() {
        WalletCreateRequest request = new WalletCreateRequest();
        request.setCustomerId(UUID.randomUUID());

        WalletResponse response = walletService.createWallet(request);

        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void topUp_addsAmountToBalance() {
        Wallet wallet = walletWithBalance(new BigDecimal("100.00"));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));

        WalletTopUpRequest request = new WalletTopUpRequest();
        request.setAmount(new BigDecimal("50.00"));

        WalletResponse response = walletService.topUp(wallet.getId(), request);

        assertThat(response.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void debit_whenSufficientBalance_subtractsAmount() {
        Wallet wallet = walletWithBalance(new BigDecimal("100.00"));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));

        walletService.debit(wallet.getId(), new BigDecimal("40.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    void debit_whenInsufficientBalance_throwsAndLeavesBalanceUnchanged() {
        Wallet wallet = walletWithBalance(new BigDecimal("10.00"));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.debit(wallet.getId(), new BigDecimal("40.00")))
                .isInstanceOf(InsufficientWalletBalanceException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void credit_addsAmountToBalance() {
        Wallet wallet = walletWithBalance(new BigDecimal("10.00"));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));

        walletService.credit(wallet.getId(), new BigDecimal("25.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("35.00");
    }

    @Test
    void getWallet_whenMissing_throwsWalletNotFoundException() {
        UUID id = UUID.randomUUID();
        when(walletRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(id))
                .isInstanceOf(WalletNotFoundException.class);
    }

    private Wallet walletWithBalance(BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setCustomerId(UUID.randomUUID());
        wallet.setBalance(balance);
        wallet.setCurrency("TRY");
        wallet.setStatus("ACTIVE");
        return wallet;
    }
}
