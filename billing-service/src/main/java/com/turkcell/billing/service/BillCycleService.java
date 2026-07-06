package com.turkcell.billing.service;

import com.turkcell.billing.dto.request.BillCycleCreateRequest;
import com.turkcell.billing.dto.response.BillCycleResponse;
import com.turkcell.billing.entity.BillCycle;
import com.turkcell.billing.exception.BillCycleNotFoundException;
import com.turkcell.billing.exception.DuplicateBillCycleException;
import com.turkcell.billing.mapper.BillCycleMapper;
import com.turkcell.billing.repository.BillCycleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class BillCycleService {

    private final BillCycleRepository billCycleRepository;
    private final BillCycleMapper billCycleMapper;

    public BillCycleService(BillCycleRepository billCycleRepository, BillCycleMapper billCycleMapper) {
        this.billCycleRepository = billCycleRepository;
        this.billCycleMapper = billCycleMapper;
    }

    @Transactional
    public BillCycleResponse createBillCycle(BillCycleCreateRequest request) {
        if (billCycleRepository.findByCustomerId(request.getCustomerId()).isPresent()) {
            throw new DuplicateBillCycleException(
                    "Bill cycle already exists for customer: " + request.getCustomerId());
        }

        BillCycle billCycle = new BillCycle();
        billCycle.setCustomerId(request.getCustomerId());
        billCycle.setDayOfMonth(request.getDayOfMonth());
        billCycle.setNextRunDate(nextRunDateFrom(LocalDate.now(), request.getDayOfMonth()));

        return billCycleMapper.toResponse(billCycleRepository.save(billCycle));
    }

    public BillCycleResponse getBillCycleResponse(UUID customerId) {
        return billCycleMapper.toResponse(getBillCycleByCustomerId(customerId));
    }

    public BillCycle getBillCycleByCustomerId(UUID customerId) {
        return billCycleRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BillCycleNotFoundException("Bill cycle not found for customer: " + customerId));
    }

    /**
     * Bir faturalama donemi basariyla islendiginde bir sonraki calisma tarihini bir ay ileri tasir.
     * BillCycle yoksa (musteri henuz kayitli degilse) sessizce atlanir - bill-run'in devam etmesi
     * bu abonelige BillCycle atanmis olmasina bagli degildir.
     */
    @Transactional
    public void advanceIfExists(UUID customerId) {
        billCycleRepository.findByCustomerId(customerId).ifPresent(billCycle -> {
            billCycle.setNextRunDate(nextRunDateFrom(billCycle.getNextRunDate(), billCycle.getDayOfMonth()));
            billCycleRepository.save(billCycle);
        });
    }

    private LocalDate nextRunDateFrom(LocalDate from, int dayOfMonth) {
        LocalDate candidate = from.plusMonths(1).withDayOfMonth(dayOfMonth);
        return candidate;
    }
}
