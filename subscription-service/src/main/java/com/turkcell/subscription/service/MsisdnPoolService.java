package com.turkcell.subscription.service;

import com.turkcell.subscription.dto.request.MsisdnPoolAddRequest;
import com.turkcell.subscription.dto.response.MsisdnPoolResponse;
import com.turkcell.subscription.entity.MsisdnPool;
import com.turkcell.subscription.exception.NoAvailableMsisdnException;
import com.turkcell.subscription.mapper.MsisdnPoolMapper;
import com.turkcell.subscription.repository.MsisdnPoolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MsisdnPoolService {

    private final MsisdnPoolRepository msisdnPoolRepository;
    private final MsisdnPoolMapper msisdnPoolMapper;

    public MsisdnPoolService(MsisdnPoolRepository msisdnPoolRepository, MsisdnPoolMapper msisdnPoolMapper) {
        this.msisdnPoolRepository = msisdnPoolRepository;
        this.msisdnPoolMapper = msisdnPoolMapper;
    }

    @Transactional
    public MsisdnPoolResponse addToPool(MsisdnPoolAddRequest request) {
        MsisdnPool pool = new MsisdnPool(request.getMsisdn(), "FREE", null);
        return msisdnPoolMapper.toResponse(msisdnPoolRepository.save(pool));
    }

    public Page<MsisdnPoolResponse> list(String status, Pageable pageable) {
        Page<MsisdnPool> page = status != null
                ? msisdnPoolRepository.findAllByStatus(status, pageable)
                : msisdnPoolRepository.findAll(pageable);
        return page.map(msisdnPoolMapper::toResponse);
    }

    @Transactional
    public String allocateNext() {
        MsisdnPool pool = msisdnPoolRepository.lockNextFree()
                .orElseThrow(() -> new NoAvailableMsisdnException("MSISDN pool is exhausted, no FREE number available"));
        pool.setStatus("ALLOCATED");
        pool.setReservedUntil(null);
        msisdnPoolRepository.save(pool);
        return pool.getMsisdn();
    }

    @Transactional
    public String allocateSpecific(String msisdn) {
        MsisdnPool pool = msisdnPoolRepository.findById(msisdn)
                .orElseThrow(() -> new NoAvailableMsisdnException("MSISDN not found in pool: " + msisdn));
        if (!"FREE".equals(pool.getStatus())) {
            throw new NoAvailableMsisdnException("MSISDN is not FREE: " + msisdn);
        }
        pool.setStatus("ALLOCATED");
        msisdnPoolRepository.save(pool);
        return pool.getMsisdn();
    }

    @Transactional
    public void release(String msisdn) {
        msisdnPoolRepository.findById(msisdn).ifPresent(pool -> {
            pool.setStatus("FREE");
            pool.setReservedUntil(null);
            msisdnPoolRepository.save(pool);
        });
    }
}
