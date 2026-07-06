-- order-service saga entegrasyonu: siparis anindaki odemelerin (aktivasyon ucreti gibi) henuz bir
-- faturasi olmaz, fatura aylik bill-run ile sonradan kesilir.
ALTER TABLE payments ALTER COLUMN invoice_id DROP NOT NULL;
