-- V4__add_overage_rates_to_tariffs.sql
-- FR-20/FR-22 icin: kota asimi ucretlendirmesinde kullanilacak birim fiyatlar.
-- NUMERIC(19,4): diger para alanlari (19,2) iken burada 4 ondalik seciliyor cunku bunlar birim
-- fiyat (orn. 0.05 TL/MB) - 2 ondalikta anlamli hassasiyet kaybi olabilir. Fatura satiri toplami
-- yine 2 ondaliga yuvarlanir (InvoiceService).
-- DEFAULT 0: mevcut tarifeler migration sonrasi kirilmadan asim ucreti almadan devam eder.
ALTER TABLE tariffs
    ADD COLUMN overage_rate_per_minute NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN overage_rate_per_sms    NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN overage_rate_per_mb     NUMERIC(19,4) NOT NULL DEFAULT 0;
