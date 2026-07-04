-- FR-28: SMS, e-posta ve push notification kanallari desteklenir.
INSERT INTO channels (id, code, description) VALUES
    (gen_random_uuid(), 'SMS', 'Short Message Service (mock)'),
    (gen_random_uuid(), 'EMAIL', 'Electronic mail (mock)'),
    (gen_random_uuid(), 'PUSH', 'Mobile push notification (mock)');
