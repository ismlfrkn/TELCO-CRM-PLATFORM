-- identity_number artik AES-256-GCM ile sifreli (Base64: 12 byte IV + ciphertext + 16 byte tag)
-- saklaniyor; duz metin TCKN/VKN'den (max 11 karakter) cok daha uzun oldugu icin kolon genisletilir.
ALTER TABLE customers ALTER COLUMN identity_number TYPE VARCHAR(255);
