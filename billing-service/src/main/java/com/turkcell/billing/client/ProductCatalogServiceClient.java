package com.turkcell.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Asim ucreti bir fiyat oldugu icin (CLAUDE.md Bolum 8.1 "Order->Catalog fiyat: Senkron (REST+cache) -
 * Fiyat snapshot alinmali" ile ayni gerekce) fatura kesim aninda guncel oran senkron cekilir; event
 * ile tasinan (potansiyel olarak bayat) bir oran kullanilmaz.
 */
@FeignClient(name = "product-catalog-service")
public interface ProductCatalogServiceClient {

    @GetMapping("/api/v1/tariffs/{code}")
    TariffClientDto getTariff(@PathVariable("code") String code);

    /**
     * FR-08: aboneligin pinlendigi tarife versiyonunun aylik ucret/paket icerigini doner (guncel
     * versiyonsa tariffs tablosundan, eskiyse tariff_versions arsivinden - bkz. product-catalog-service
     * TariffService.getTariffVersion). Asim oranlari tariff_versions'ta arsivlenmedigi icin
     * (yalnizca guncel Tariff'ta var) asim hesaplamasi hala getTariff(code) ile GUNCEL orani kullanir -
     * bilincli bir sadelestirme, sadece aylik ucret gecmis versiyona pinlenir.
     */
    @GetMapping("/api/v1/tariffs/{code}/versions/{version}")
    TariffVersionClientDto getTariffVersion(@PathVariable("code") String code, @PathVariable("version") int version);
}
