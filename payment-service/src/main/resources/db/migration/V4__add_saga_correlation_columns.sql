-- Order->Payment bacagi artik OrderCreated event'ini tuketerek tetikleniyor (senkron REST'in yerine
-- gecti). Bu 3 alan, PaymentCompleted/PaymentFailed event'inde subscription-service ve order-service'in
-- ihtiyac duydugu baglami (hangi siparis, hangi musteri, hangi tarife) senkron geri sorgu yapmadan
-- tasimak icin var - Payment'in kendi domain'i degil, saga korelasyon verisi.
ALTER TABLE payments ADD COLUMN order_id UUID;
ALTER TABLE payments ADD COLUMN customer_id UUID;
ALTER TABLE payments ADD COLUMN tariff_code VARCHAR(255);
