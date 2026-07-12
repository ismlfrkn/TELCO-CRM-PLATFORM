package com.turkcell.usage.service;

import com.turkcell.usage.dto.request.QuotaCreateRequest;
import com.turkcell.usage.dto.response.QuotaResponse;
import com.turkcell.usage.entity.Quota;
import com.turkcell.usage.exception.DuplicateQuotaException;
import com.turkcell.usage.exception.QuotaNotFoundException;
import com.turkcell.usage.exception.UnsupportedCdrTypeException;
import com.turkcell.usage.mapper.QuotaMapper;
import com.turkcell.usage.repository.QuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class QuotaService {

    private static final String TYPE_VOICE = "VOICE";
    private static final String TYPE_SMS = "SMS";
    private static final String TYPE_DATA = "DATA";

    private static final String EVENT_QUOTA_EXCEEDED = "QuotaExceeded";
    private static final String EVENT_QUOTA_THRESHOLD_REACHED = "QuotaThresholdReached";
    private static final double THRESHOLD_RATIO = 0.8; // FR-19: %80 kullanimda uyari

    private final QuotaRepository quotaRepository;
    private final QuotaMapper quotaMapper;

    public QuotaService(QuotaRepository quotaRepository, QuotaMapper quotaMapper) {
        this.quotaRepository = quotaRepository;
        this.quotaMapper = quotaMapper;
    }

    @Transactional
    public QuotaResponse createQuota(QuotaCreateRequest request) {
        if (quotaRepository.findBySubscriptionId(request.getSubscriptionId()).isPresent()) {
            throw new DuplicateQuotaException(
                    "Quota already exists for subscription: " + request.getSubscriptionId());
        }

        Quota quota = new Quota();
        quota.setSubscriptionId(request.getSubscriptionId());
        quota.setPeriodStart(request.getPeriodStart());
        quota.setPeriodEnd(request.getPeriodEnd());
        quota.setMinutesIncluded(request.getMinutesIncluded());
        quota.setSmsIncluded(request.getSmsIncluded());
        quota.setMbIncluded(request.getMbIncluded());
        quota.setMinutesRemaining(request.getMinutesIncluded());
        quota.setSmsRemaining(request.getSmsIncluded());
        quota.setMbRemaining(request.getMbIncluded());

        return quotaMapper.toResponse(quotaRepository.save(quota));
    }

    public QuotaResponse getQuotaResponse(UUID subscriptionId) {
        return quotaMapper.toResponse(getQuotaBySubscriptionId(subscriptionId));
    }

    public Quota getQuotaBySubscriptionId(UUID subscriptionId) {
        return quotaRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new QuotaNotFoundException("Quota not found for subscription: " + subscriptionId));
    }

    /**
     * FOR UPDATE ile kilitlenen tek satiri gunceller: ayni aboneligin kotasina eszamanli birden
     * fazla CDR dusme istegi gelirse (ornegin biri bitmeden digeri baslayan iki cagri), her istek
     * sirayla islenir - hicbiri "eski" remaining degeri uzerinden yazip digerinin dususunu kaybetmez.
     */
    @Transactional
    public QuotaDeductionResult deduct(UUID subscriptionId, String cdrType, BigDecimal quantity) {
        Quota quota = quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)
                .orElseThrow(() -> new QuotaNotFoundException("Quota not found for subscription: " + subscriptionId));

        int amount = quantity.setScale(0, RoundingMode.CEILING).intValueExact();
        int included;
        int remainingBefore;
        int remainingAfter;

        switch (cdrType) {
            case TYPE_VOICE -> {
                included = quota.getMinutesIncluded();
                remainingBefore = quota.getMinutesRemaining();
                remainingAfter = remainingBefore - amount;
                quota.setMinutesRemaining(remainingAfter);
            }
            case TYPE_SMS -> {
                included = quota.getSmsIncluded();
                remainingBefore = quota.getSmsRemaining();
                remainingAfter = remainingBefore - amount;
                quota.setSmsRemaining(remainingAfter);
            }
            case TYPE_DATA -> {
                included = quota.getMbIncluded();
                remainingBefore = quota.getMbRemaining();
                remainingAfter = remainingBefore - amount;
                quota.setMbRemaining(remainingAfter);
            }
            default -> throw new UnsupportedCdrTypeException("Unsupported cdr type: " + cdrType);
        }

        quotaRepository.save(quota);
        String thresholdEvent = detectThresholdEvent(included, remainingBefore, remainingAfter);
        BigDecimal overageQuantity = computeOverageQuantity(amount, remainingBefore, remainingAfter);
        return new QuotaDeductionResult(quotaMapper.toResponse(quota), thresholdEvent, overageQuantity);
    }

    /**
     * FR-20: asim kullanimlari billing'e agregate edilir. Bu CDR dususu kotayi negatife
     * dusurduyse (ya da zaten negatifken devam ettiyse), dususun asima denk gelen kismini
     * dondurur - null donerse bu CDR tamamen kota icinde kalmis demektir, UsageAggregated
     * yayinlanmaz.
     */
    private BigDecimal computeOverageQuantity(int amount, int remainingBefore, int remainingAfter) {
        if (remainingAfter >= 0) {
            return null;
        }
        int overage = remainingBefore > 0 ? -remainingAfter : amount;
        return overage > 0 ? BigDecimal.valueOf(overage) : null;
    }

    /**
     * Kenar tetiklemeli (edge-triggered): esik zaten asilmisken gelen sonraki CDR'larda event
     * TEKRAR uretilmez, sadece esigin "yeni gecildigi" CDR'da uretilir.
     */
    private String detectThresholdEvent(int included, int remainingBefore, int remainingAfter) {
        if (included <= 0) {
            return null;
        }
        if (remainingBefore > 0 && remainingAfter <= 0) {
            return EVENT_QUOTA_EXCEEDED;
        }
        int thresholdRemaining = (int) Math.ceil(included * (1 - THRESHOLD_RATIO));
        if (remainingBefore > thresholdRemaining && remainingAfter <= thresholdRemaining) {
            return EVENT_QUOTA_THRESHOLD_REACHED;
        }
        return null;
    }

    public record QuotaDeductionResult(QuotaResponse quota, String thresholdEvent, BigDecimal overageQuantity) {
    }
}
