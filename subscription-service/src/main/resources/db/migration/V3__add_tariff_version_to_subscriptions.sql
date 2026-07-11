-- FR-08: abonenin hangi tarife versiyonuna baglandigini tutar (product-catalog-service
-- Tariff.version). Nullable - caller doldurmazsa (ör. eski/manuel akislar) bilinmiyor kabul edilir.
ALTER TABLE subscriptions ADD COLUMN tariff_version INTEGER;
