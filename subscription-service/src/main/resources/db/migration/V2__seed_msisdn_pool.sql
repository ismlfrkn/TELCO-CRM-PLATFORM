-- Yerel gelistirme/test icin 200 adet musait (FREE) ornek MSISDN uretir: 905550000001 .. 905550000200
INSERT INTO msisdn_pool (msisdn, status)
SELECT '90555' || LPAD(n::text, 7, '0'), 'FREE'
FROM generate_series(1, 200) AS n;
