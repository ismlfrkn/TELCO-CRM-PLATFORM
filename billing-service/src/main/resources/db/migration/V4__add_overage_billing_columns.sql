-- V4__add_overage_billing_columns.sql
-- tariff_code: fatura hangi tarifeye gore kesildi (audit + asim oranini catalog'dan cekmek icin
-- InvoiceCreateRequest uzerinden gelir, InvoiceService'in kendisi tahmin etmez).
ALTER TABLE invoices ADD COLUMN tariff_code VARCHAR(255);

-- invoice_id: bir asim kaydinin hangi faturaya "islendigini" isaretler - NULL ise henuz
-- faturalanmamis (unclaimed) demektir. Bir bill-run bu satiri bir faturaya bagladiktan sonra
-- bir sonraki bill-run'da tekrar secilip cift faturalanmamasi icin gereklidir.
ALTER TABLE usage_aggregates ADD COLUMN invoice_id UUID;

CREATE INDEX idx_usage_aggregates_unclaimed
    ON usage_aggregates (subscription_id, period_start, period_end)
    WHERE invoice_id IS NULL;
