-- Migration: Add street, streetNumber, addressComplement fields to clients and providers tables
-- Date: 2025-01-29

-- Add new columns to clients table
ALTER TABLE clients 
ADD COLUMN IF NOT EXISTS street VARCHAR(200),
ADD COLUMN IF NOT EXISTS street_number VARCHAR(20),
ADD COLUMN IF NOT EXISTS address_complement VARCHAR(100);

-- Add new columns to providers table
ALTER TABLE providers 
ADD COLUMN IF NOT EXISTS street VARCHAR(200),
ADD COLUMN IF NOT EXISTS street_number VARCHAR(20),
ADD COLUMN IF NOT EXISTS address_complement VARCHAR(100);

-- Note: The address field remains for backwards compatibility
-- New records will populate both address (constructed) and the separate fields
-- Old records will have NULL in the new fields, which is handled gracefully


