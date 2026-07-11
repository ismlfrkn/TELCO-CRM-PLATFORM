-- FR-08: tarife degisiklikleri versiyonlanmali, eski abonelerin tarifesi korunmalidir.
-- tariffs.version her updateTariff/patchTariff'te 1 artar; onceki durum tariff_versions'a
-- degismez (immutable) bir anlik goruntu olarak kopyalanir.
ALTER TABLE tariffs ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

CREATE TABLE tariff_versions (
    id UUID PRIMARY KEY,
    tariff_id UUID NOT NULL,
    code VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    monthly_fee DECIMAL(19, 2) NOT NULL,
    minutes_included INT NOT NULL,
    sms_included INT NOT NULL,
    data_mb_included INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    currency VARCHAR(3) NOT NULL,
    superseded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tariff_versions_tariff FOREIGN KEY (tariff_id) REFERENCES tariffs (id) ON DELETE CASCADE,
    CONSTRAINT uq_tariff_versions_tariff_version UNIQUE (tariff_id, version)
);

CREATE INDEX idx_tariff_versions_code ON tariff_versions (code);
