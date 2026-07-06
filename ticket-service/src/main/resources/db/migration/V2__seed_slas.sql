-- Yerel gelistirme/test icin ornek SLA politikalari (dakika cinsinden).
INSERT INTO slas (id, category, priority, response_time, resolution_time) VALUES
    (gen_random_uuid(), 'TECHNICAL', 'HIGH', 15, 240),
    (gen_random_uuid(), 'TECHNICAL', 'MEDIUM', 30, 480),
    (gen_random_uuid(), 'TECHNICAL', 'LOW', 60, 1440),
    (gen_random_uuid(), 'BILLING', 'HIGH', 15, 240),
    (gen_random_uuid(), 'BILLING', 'MEDIUM', 30, 720),
    (gen_random_uuid(), 'BILLING', 'LOW', 60, 1440),
    (gen_random_uuid(), 'COMPLAINT', 'HIGH', 15, 480),
    (gen_random_uuid(), 'COMPLAINT', 'MEDIUM', 60, 1440),
    (gen_random_uuid(), 'GENERAL', 'LOW', 120, 2880);
