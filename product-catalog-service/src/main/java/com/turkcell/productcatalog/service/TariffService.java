package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.TariffCreateRequest;
import com.turkcell.productcatalog.dto.request.TariffPatchRequest;
import com.turkcell.productcatalog.dto.request.TariffUpdateRequest;
import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.dto.response.TariffVersionResponse;
import com.turkcell.productcatalog.entity.Addon;
import com.turkcell.productcatalog.entity.Tariff;
import com.turkcell.productcatalog.entity.TariffVersion;
import com.turkcell.productcatalog.exception.TariffNotFoundException;
import com.turkcell.productcatalog.exception.TariffVersionNotFoundException;
import com.turkcell.productcatalog.mapper.TariffMapper;
import com.turkcell.productcatalog.mapper.TariffVersionMapper;
import com.turkcell.productcatalog.repository.TariffRepository;
import com.turkcell.productcatalog.repository.TariffVersionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TariffService {

    private static final String AGGREGATE_TYPE = "Tariff";

    private final TariffRepository tariffRepository;
    private final TariffVersionRepository tariffVersionRepository;
    private final AddonService addonService;
    private final TariffMapper tariffMapper;
    private final TariffVersionMapper tariffVersionMapper;
    private final OutboxEventService outboxEventService;

    public TariffService(TariffRepository tariffRepository, TariffVersionRepository tariffVersionRepository,
                          AddonService addonService, TariffMapper tariffMapper,
                          TariffVersionMapper tariffVersionMapper, OutboxEventService outboxEventService) {
        this.tariffRepository = tariffRepository;
        this.tariffVersionRepository = tariffVersionRepository;
        this.addonService = addonService;
        this.tariffMapper = tariffMapper;
        this.tariffVersionMapper = tariffVersionMapper;
        this.outboxEventService = outboxEventService;
    }

    public Page<TariffResponse> getAllTariffs(Pageable pageable) {
        return tariffRepository.findAllByStatusNot("INACTIVE", pageable)
                .map(tariffMapper::toResponse);
    }

    @Cacheable(cacheNames = "tariffs", key = "#code")
    public TariffResponse getTariffResponseByCode(String code) {
        return tariffMapper.toResponse(getTariffByCode(code));
    }

    public Tariff getTariffByCode(String code) {
        return tariffRepository.findByCodeAndStatusNot(code, "INACTIVE")
                .orElseThrow(() -> new TariffNotFoundException("Tariff not found with code: " + code));
    }

    @Transactional
    public TariffResponse createTariff(TariffCreateRequest request) {
        Tariff tariff = new Tariff();
        tariff.setCode(request.getCode());
        tariff.setName(request.getName());
        tariff.setType(request.getType());
        tariff.setMonthlyFee(request.getMonthlyFee());
        tariff.setMinutesIncluded(request.getMinutesIncluded());
        tariff.setSmsIncluded(request.getSmsIncluded());
        tariff.setDataMbIncluded(request.getDataMbIncluded());
        tariff.setOverageRatePerMinute(request.getOverageRatePerMinute());
        tariff.setOverageRateSms(request.getOverageRateSms());
        tariff.setOverageRatePerMb(request.getOverageRatePerMb());
        tariff.setStatus(request.getStatus());
        tariff.setCurrency(request.getCurrency());
        tariff.setEffectiveFrom(request.getEffectiveFrom());
        tariff.setEffectiveTo(request.getEffectiveTo());

        tariff = tariffRepository.save(tariff);
        TariffResponse response = tariffMapper.toResponse(tariff);
        outboxEventService.publish(AGGREGATE_TYPE, tariff.getId(), "TariffCreated", response);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "tariffs", key = "#code")
    public TariffResponse updateTariff(String code, TariffUpdateRequest request) {
        Tariff tariff = getTariffByCode(code);
        archiveCurrentVersion(tariff);
        BigDecimal oldMonthlyFee = tariff.getMonthlyFee();

        tariff.setName(request.getName());
        tariff.setType(request.getType());
        tariff.setMonthlyFee(request.getMonthlyFee());
        tariff.setMinutesIncluded(request.getMinutesIncluded());
        tariff.setSmsIncluded(request.getSmsIncluded());
        tariff.setDataMbIncluded(request.getDataMbIncluded());
        tariff.setOverageRatePerMinute(request.getOverageRatePerMinute());
        tariff.setOverageRateSms(request.getOverageRateSms());
        tariff.setOverageRatePerMb(request.getOverageRatePerMb());
        tariff.setStatus(request.getStatus());
        tariff.setCurrency(request.getCurrency());
        tariff.setEffectiveFrom(request.getEffectiveFrom());
        tariff.setEffectiveTo(request.getEffectiveTo());
        tariff.setVersion(tariff.getVersion() + 1);

        tariff = tariffRepository.save(tariff);
        TariffResponse response = tariffMapper.toResponse(tariff);
        outboxEventService.publish(AGGREGATE_TYPE, tariff.getId(), updateEventTypeFor(oldMonthlyFee, tariff.getMonthlyFee()), response);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "tariffs", key = "#code")
    public TariffResponse patchTariff(String code, TariffPatchRequest request) {
        Tariff tariff = getTariffByCode(code);
        archiveCurrentVersion(tariff);
        BigDecimal oldMonthlyFee = tariff.getMonthlyFee();

        if (request.getName() != null) tariff.setName(request.getName());
        if (request.getType() != null) tariff.setType(request.getType());
        if (request.getMonthlyFee() != null) tariff.setMonthlyFee(request.getMonthlyFee());
        if (request.getMinutesIncluded() != null) tariff.setMinutesIncluded(request.getMinutesIncluded());
        if (request.getSmsIncluded() != null) tariff.setSmsIncluded(request.getSmsIncluded());
        if (request.getDataMbIncluded() != null) tariff.setDataMbIncluded(request.getDataMbIncluded());
        if (request.getOverageRatePerMinute() != null) tariff.setOverageRatePerMinute(request.getOverageRatePerMinute());
        if (request.getOverageRateSms() != null) tariff.setOverageRateSms(request.getOverageRateSms());
        if (request.getOverageRatePerMb() != null) tariff.setOverageRatePerMb(request.getOverageRatePerMb());
        if (request.getStatus() != null) tariff.setStatus(request.getStatus());
        if (request.getCurrency() != null) tariff.setCurrency(request.getCurrency());
        if (request.getEffectiveFrom() != null) tariff.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) tariff.setEffectiveTo(request.getEffectiveTo());
        tariff.setVersion(tariff.getVersion() + 1);

        tariff = tariffRepository.save(tariff);
        TariffResponse response = tariffMapper.toResponse(tariff);
        outboxEventService.publish(AGGREGATE_TYPE, tariff.getId(), updateEventTypeFor(oldMonthlyFee, tariff.getMonthlyFee()), response);
        return response;
    }

    /**
     * FR-08: bir onceki versiyonu (henuz yeni degerler uygulanmadan once) immutable bir
     * anlik goruntu olarak tariff_versions'a kopyalar. subscription-service gibi tuketiciler
     * bu satirlar sayesinde "eski abonemin baglandigi versiyonun sartlari neydi" sorusunu
     * yanitlayabilir (bkz. GET /tariffs/{code}/versions/{version}).
     */
    private void archiveCurrentVersion(Tariff tariff) {
        TariffVersion snapshot = new TariffVersion();
        snapshot.setTariffId(tariff.getId());
        snapshot.setCode(tariff.getCode());
        snapshot.setVersion(tariff.getVersion());
        snapshot.setName(tariff.getName());
        snapshot.setType(tariff.getType());
        snapshot.setMonthlyFee(tariff.getMonthlyFee());
        snapshot.setMinutesIncluded(tariff.getMinutesIncluded());
        snapshot.setSmsIncluded(tariff.getSmsIncluded());
        snapshot.setDataMbIncluded(tariff.getDataMbIncluded());
        snapshot.setStatus(tariff.getStatus());
        snapshot.setEffectiveFrom(tariff.getEffectiveFrom());
        snapshot.setEffectiveTo(tariff.getEffectiveTo());
        snapshot.setCurrency(tariff.getCurrency());
        tariffVersionRepository.save(snapshot);
    }

    /**
     * Belirli bir versiyonun sartlarini dondurur: istenen versiyon guncel (henuz arsivlenmemis)
     * versiyonsa canli satirdan, degilse tariff_versions'daki immutable kayittan okunur.
     */
    public TariffVersionResponse getTariffVersion(String code, int version) {
        Tariff tariff = findTariffEntityByCode(code);

        if (tariff.getVersion() == version) {
            return tariffVersionMapper.fromCurrent(tariff);
        }

        return tariffVersionRepository.findByCodeAndVersion(code, version)
                .map(tariffVersionMapper::fromHistory)
                .orElseThrow(() -> new TariffVersionNotFoundException(
                        "Tariff " + code + " has no version " + version));
    }

    /**
     * Gecmis (arsivlenmis) versiyonlarin listesi, en yeniden en eskiye. Guncel versiyon zaten
     * GET /tariffs/{code} ile alinabildigi icin bu listeye dahil edilmez.
     */
    public List<TariffVersionResponse> getTariffVersionHistory(String code) {
        findTariffEntityByCode(code);
        return tariffVersionRepository.findAllByCodeOrderByVersionDesc(code).stream()
                .map(tariffVersionMapper::fromHistory)
                .toList();
    }

    /**
     * getTariffByCode'dan farkli olarak status'u (ACTIVE/INACTIVE) filtrelemez - deaktive
     * edilmis bir tarifenin gecmis versiyonlari da sorgulanabilir olmali.
     */
    private Tariff findTariffEntityByCode(String code) {
        return tariffRepository.findByCode(code)
                .orElseThrow(() -> new TariffNotFoundException("Tariff not found with code: " + code));
    }

    /**
     * monthlyFee gercekten degistiyse (fiyat degisikligi diger alan degisikliklerinden ayri bir
     * semantige sahip - tuketen servisler icin onemli olabilir) TariffPriceChanged, aksi halde
     * (fiyat ayni kalip sadece isim/aciklama/kota gibi alanlar degistiyse) mevcut TariffUpdated
     * yayinlanir. Ayni cagrida hem fiyat hem baska alanlar birden degisirse de TariffPriceChanged
     * yayinlanir - fiyat degisikligi tuketiciler icin daha kritik/spesifik bir sinyal sayilir.
     */
    private String updateEventTypeFor(BigDecimal oldMonthlyFee, BigDecimal newMonthlyFee) {
        boolean priceChanged = oldMonthlyFee == null
                ? newMonthlyFee != null
                : oldMonthlyFee.compareTo(newMonthlyFee) != 0;
        return priceChanged ? "TariffPriceChanged" : "TariffUpdated";
    }

    @Transactional
    @CacheEvict(cacheNames = "tariffs", key = "#code")
    public void deleteTariff(String code) {
        Tariff tariff = getTariffByCode(code);
        tariff.setStatus("INACTIVE");
        tariff = tariffRepository.save(tariff);
        outboxEventService.publish(AGGREGATE_TYPE, tariff.getId(), "TariffDeactivated", tariffMapper.toResponse(tariff));
    }

    @Transactional
    @CacheEvict(cacheNames = "tariffs", key = "#tariffCode")
    public void linkAddonToTariff(String tariffCode, String addonCode) {
        Tariff tariff = getTariffByCode(tariffCode);
        Addon addon = addonService.getAddonByCode(addonCode);
        
        tariff.getAddons().add(addon);
        tariffRepository.save(tariff);
    }
}
