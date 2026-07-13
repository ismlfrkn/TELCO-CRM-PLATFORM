-- V4__seed_domain_event_templates.sql
-- FR-29: DomainEventConsumerConfig'in Kafka'dan tukettigi event'ler icin gercek (jenerik olmayan)
-- sablon icerigi. Yer tutucular ({{alan}}) event zarfinin payload'undaki JSON alan adlariyla
-- birebir eslesir (ör. SubscriptionActivated payload'unda "msisdn"/"tariffCode" var).
INSERT INTO notification_templates (id, code, channel_id, locale, subject, body_template)
VALUES
    (gen_random_uuid(), 'CUSTOMER_WELCOME_SMS', (SELECT id FROM channels WHERE code = 'SMS'), 'tr-TR',
     NULL, 'Merhaba {{firstName}}, TelcoX''e hosgeldiniz! Basvurunuz alindi, kimlik dogrulamasi sonrasi bilgilendirileceksiniz.'),
    (gen_random_uuid(), 'WELCOME_SMS', (SELECT id FROM channels WHERE code = 'SMS'), 'tr-TR',
     NULL, 'Merhaba, {{msisdn}} numarali hattiniz {{tariffCode}} tarifesiyle aktif edildi. Iyi kullanimlar!'),
    (gen_random_uuid(), 'INVOICE_GENERATED_EMAIL', (SELECT id FROM channels WHERE code = 'EMAIL'), 'tr-TR',
     'Faturaniz Hazir', 'Sayin musterimiz, {{periodStart}} - {{periodEnd}} donemine ait faturaniz hazirlandi. Tutar: {{grandTotal}} {{currency}}. Son odeme tarihi: {{dueDate}}.'),
    (gen_random_uuid(), 'ORDER_CANCELLED_SMS', (SELECT id FROM channels WHERE code = 'SMS'), 'tr-TR',
     NULL, 'Siparisiniz iptal edildi. Tutar: {{totalAmount}} {{currency}}. Sorulariniz icin musteri hizmetlerini arayabilirsiniz.'),
    (gen_random_uuid(), 'PAYMENT_FAILED_SMS', (SELECT id FROM channels WHERE code = 'SMS'), 'tr-TR',
     NULL, 'Odemeniz gerceklestirilemedi. Tutar: {{amount}} {{currency}}. Lutfen odeme yonteminizi kontrol edip tekrar deneyin.')
ON CONFLICT DO NOTHING;
