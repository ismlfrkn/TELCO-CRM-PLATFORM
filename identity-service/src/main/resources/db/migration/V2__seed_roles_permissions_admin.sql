-- V2__seed_roles_permissions_admin.sql
-- Seed/dev data: persona tablosuna uygun roller, temel yetkiler ve test icin bir admin kullanicisi.
-- Admin kullanicisi: username=admin, password=Admin123! (yalnizca gelistirme/test ortami icindir)

INSERT INTO roles (id, name, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ADMIN', 'Sistem yoneticisi - tam yetki'),
    ('22222222-2222-2222-2222-222222222222', 'CALL_CENTER_AGENT', 'Cagri merkezi temsilcisi'),
    ('33333333-3333-3333-3333-333333333333', 'DEALER', 'Saha bayisi'),
    ('44444444-4444-4444-4444-444444444444', 'MARKETING_MANAGER', 'Pazarlama yoneticisi'),
    ('55555555-5555-5555-5555-555555555555', 'BILLING_OPERATOR', 'Fatura operatoru');

INSERT INTO permissions (id, code, description) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'USER_MANAGE', 'Kullanici olusturma/guncelleme/silme'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ROLE_MANAGE', 'Rol olusturma/guncelleme/silme'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'PERMISSION_MANAGE', 'Yetki olusturma/guncelleme/silme');

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('11111111-1111-1111-1111-111111111111', 'cccccccc-cccc-cccc-cccc-cccccccccccc');

INSERT INTO users (id, username, email, phone_number, password_hash, status, customer_id, last_login_at, created_at, updated_at) VALUES
    ('99999999-9999-9999-9999-999999999999', 'admin', 'admin@telco.example', '+900000000000',
     '$2a$10$Iq4UyACO3zH3KJ5NEHMFRumIqZMri5RrrXM2dIBMUoI75A1tq3aSy', 'ACTIVE', NULL, NULL, now(), now());

INSERT INTO user_roles (user_id, role_id) VALUES
    ('99999999-9999-9999-9999-999999999999', '11111111-1111-1111-1111-111111111111');
