-- V3__seed_customer_role.sql
-- CUSTOMER rolu: aboneler/musteriler icin ayrilmis rol. Bu rolun User.customerId (customer-service'teki
-- Customer'a capraz-servis referans) alani ile birlikte nasil kullanilacagi (self-servis abone login'i
-- identity-service uzerinden mi yoksa ayri bir akiskan mi olacak) henuz netlesmedi; gercek baglanti
-- ve olasi self-servis kayit akisi ilerleyen bir fazda tasarlanacaktir. Simdilik sadece rol tanimi seed edilir.

INSERT INTO roles (id, name, description) VALUES
    ('66666666-6666-6666-6666-666666666666', 'CUSTOMER', 'Abone/musteri - self-servis erisim (baglanti Faz 3''te netlesecek)');
