-- Order->Subscription aktivasyonu artik PaymentCompleted event'ini tuketerek tetikleniyor. UNIQUE
-- kisit, ayni siparis icin PaymentCompleted iki kez gelse bile (en-az-bir-kez teslim) iki MSISDN
-- tahsis edilip iki abonelik acilmasini DB seviyesinde engelliyor (payment-service'teki
-- idempotency_key ile ayni mantik).
ALTER TABLE subscriptions ADD COLUMN order_id UUID UNIQUE;
