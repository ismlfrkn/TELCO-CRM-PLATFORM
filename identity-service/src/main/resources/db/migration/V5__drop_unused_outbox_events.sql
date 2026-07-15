-- V5__drop_unused_outbox_events.sql
-- identity-service, MVP spec'inde (Bolum 2.2 event listesi, Bolum 8 servis-bazli Publish/Consume
-- tablosu) hicbir domain event'i yayinlamiyor/tuketmiyor - kimlik/yetki disinda bir bounded
-- context'i yok. Bu tablo diger 9 servisin ortak scaffolding'inden kopyalanmis ama hicbir zaman
-- bir OutboxEventService/Publisher tarafindan yazilmadi (bkz. CLAUDE.md Bolum 16). Olu kod.
DROP TABLE IF EXISTS outbox_events;
