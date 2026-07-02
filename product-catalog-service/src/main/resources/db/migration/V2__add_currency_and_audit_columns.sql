-- V2__add_currency_and_audit_columns.sql

-- Add currency to tariffs and addons
ALTER TABLE tariffs ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'TRY';
ALTER TABLE addons ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'TRY';

-- Add status to addons for soft-delete support
ALTER TABLE addons ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Add createdAt to support sorting and pagination
ALTER TABLE tariffs ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE addons ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE product_offerings ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
