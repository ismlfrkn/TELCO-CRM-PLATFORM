package com.turkcell.payment.service;

import com.turkcell.payment.dto.request.WalletCreateRequest;
import com.turkcell.payment.dto.request.WalletTopUpRequest;
import com.turkcell.payment.dto.response.WalletResponse;
import com.turkcell.payment.entity.Wallet;
import com.turkcell.payment.exception.InsufficientWalletBalanceException;
import com.turkcell.payment.exception.WalletNotFoundException;
import com.turkcell.payment.mapper.WalletMapper;
import com.turkcell.payment.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    public WalletService(WalletRepository walletRepository, WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletMapper = walletMapper;
    }

    @Transactional
    public WalletResponse createWallet(WalletCreateRequest request) {
        Wallet wallet = new Wallet();
        wallet.setCustomerId(request.getCustomerId());
        wallet.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        wallet.setCurrency(request.getCurrency());
        wallet.setStatus("ACTIVE");
        return walletMapper.toResponse(walletRepository.save(wallet));
    }

    public WalletResponse getWalletResponse(UUID id) {
        return walletMapper.toResponse(getWallet(id));
    }

    public Wallet getWallet(UUID id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + id));
    }

    @Transactional
    public WalletResponse topUp(UUID id, WalletTopUpRequest request) {
        Wallet wallet = walletRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + id));
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        wallet.setUpdatedAt(Instant.now());
        return walletMapper.toResponse(walletRepository.save(wallet));
    }

    @Transactional
    public void debit(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientWalletBalanceException(
                    "Wallet " + walletId + " has insufficient balance for amount " + amount);
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
    }

    @Transactional
    public void credit(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
    }
}
