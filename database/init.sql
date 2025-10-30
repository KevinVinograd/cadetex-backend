-- Cadetex Database Initialization Script
-- PostgreSQL Database Schema

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Organizations table
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users table (authentication and authorization)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPERADMIN', 'ORGADMIN', 'COURIER')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Addresses table (normalized addresses for clients and providers)
CREATE TABLE IF NOT EXISTS addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    street VARCHAR(200),
    street_number VARCHAR(20),
    address_complement VARCHAR(100),
    city VARCHAR(80),
    province VARCHAR(80),
    postal_code VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Clients table
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    address_id UUID REFERENCES addresses(id) ON DELETE SET NULL,
    phone_number VARCHAR(50),
    email VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Providers table
CREATE TABLE IF NOT EXISTS providers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    address_id UUID REFERENCES addresses(id) ON DELETE SET NULL,
    contact_name VARCHAR(100),
    contact_phone VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Couriers table (business data with user relationship)
CREATE TABLE IF NOT EXISTS couriers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    address VARCHAR(255),
    vehicle_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('RETIRE', 'DELIVER')),
    reference_number VARCHAR(50),
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    provider_id UUID REFERENCES providers(id) ON DELETE SET NULL,
    address_override_id UUID REFERENCES addresses(id) ON DELETE SET NULL,
    contact VARCHAR(100),
    courier_id UUID REFERENCES couriers(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'PENDING_CONFIRMATION', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('NORMAL', 'URGENT')),
    scheduled_date VARCHAR(10),
    notes TEXT,
    courier_notes TEXT,
    mbl VARCHAR(50),
    hbl VARCHAR(50),
    freight_cert BOOLEAN DEFAULT FALSE,
    fo_cert BOOLEAN DEFAULT FALSE,
    bunker_cert BOOLEAN DEFAULT FALSE,
    linked_task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    receipt_photo_url TEXT,
    photo_required BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_contact_exclusivity CHECK (
        (client_id IS NOT NULL AND provider_id IS NULL) OR 
        (client_id IS NULL AND provider_id IS NOT NULL)
    )
);

-- Task Photos table
CREATE TABLE IF NOT EXISTS task_photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    photo_type VARCHAR(20) NOT NULL CHECK (photo_type IN ('RECEIPT', 'ADDITIONAL')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial data
INSERT INTO organizations (id, name) VALUES 
    ('00000000-0000-0000-0000-000000000001', 'Cadetex Demo Organization')
ON CONFLICT (id) DO NOTHING;

-- Insert demo users (with hashed passwords)
-- Password for all demo users: "password123"
INSERT INTO users (id, organization_id, name, email, password_hash, role) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Admin Demo', 'admin@cadetex.com', '$2a$10$gYdF8HSMLRzGXRtEnxttZuj0L/lVKEAGKz2OwosuvQdq8T4MliWJu', 'SUPERADMIN'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Org Admin Demo', 'orgadmin@cadetex.com', '$2a$10$gYdF8HSMLRzGXRtEnxttZuj0L/lVKEAGKz2OwosuvQdq8T4MliWJu', 'ORGADMIN'),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Carlos López', 'carlos@cadetex.com', '$2a$10$gYdF8HSMLRzGXRtEnxttZuj0L/lVKEAGKz2OwosuvQdq8T4MliWJu', 'COURIER')
ON CONFLICT (id) DO NOTHING;

-- Insert demo addresses
INSERT INTO addresses (id, street, street_number, city, province) VALUES 
    ('00000000-0000-0000-0000-000000000001', 'Av. Corrientes', '1234', 'Buenos Aires', 'CABA'),
    ('00000000-0000-0000-0000-000000000002', 'Av. Santa Fe', '5678', 'Buenos Aires', 'CABA'),
    ('00000000-0000-0000-0000-000000000003', 'Av. Rivadavia', '9012', 'Buenos Aires', 'CABA')
ON CONFLICT (id) DO NOTHING;

-- Insert demo clients
INSERT INTO clients (id, organization_id, name, address_id, phone_number, email, is_active) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Cliente Demo S.A.', '00000000-0000-0000-0000-000000000001', '+54911234567', 'cliente@demo.com', true),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Empresa Logística ABC', '00000000-0000-0000-0000-000000000002', '+54911234568', 'contacto@empresaabc.com', true)
ON CONFLICT (id) DO NOTHING;

-- Insert demo providers
INSERT INTO providers (id, organization_id, name, address_id, contact_name, contact_phone, is_active) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Proveedor Demo S.A.', '00000000-0000-0000-0000-000000000002', 'Juan Pérez', '+54911234568', true),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Transportes XYZ', '00000000-0000-0000-0000-000000000003', 'María González', '+54911234569', true)
ON CONFLICT (id) DO NOTHING;

-- Insert demo courier (linked to user)
INSERT INTO couriers (id, user_id, organization_id, name, phone_number, address, vehicle_type, is_active) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Carlos López', '+54911234569', 'Av. Rivadavia 9012', 'Moto', true)
ON CONFLICT (id) DO NOTHING;

-- Insert additional demo addresses for task overrides
INSERT INTO addresses (id, street, street_number, city, province) VALUES 
    ('00000000-0000-0000-0000-000000000004', 'Av. 9 de Julio', '999', 'Buenos Aires', 'CABA'),
    ('00000000-0000-0000-0000-000000000005', 'Av. Rivadavia', '1111', 'Buenos Aires', 'CABA'),
    ('00000000-0000-0000-0000-000000000006', 'Av. Callao', '2222', 'Buenos Aires', 'CABA')
ON CONFLICT (id) DO NOTHING;

-- Insert demo tasks
INSERT INTO tasks (id, organization_id, type, reference_number, client_id, provider_id, address_override_id, contact, courier_id, status, priority, scheduled_date, notes, mbl, hbl, freight_cert, fo_cert, bunker_cert, photo_required) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-001', '00000000-0000-0000-0000-000000000001', NULL, NULL, NULL, '00000000-0000-0000-0000-000000000001', 'PENDING', 'URGENT', '2024-01-15', 'Recoger documentos urgentes', 'MBL001', 'HBL001', true, false, true, true),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'DELIVER', 'DEL-002', NULL, '00000000-0000-0000-0000-000000000001', NULL, NULL, '00000000-0000-0000-0000-000000000001', 'CONFIRMED', 'NORMAL', '2024-01-16', 'Entrega de mercadería', 'MBL002', 'HBL002', false, true, false, true),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-003', '00000000-0000-0000-0000-000000000001', NULL, '00000000-0000-0000-0000-000000000004', 'Carlos López', '00000000-0000-0000-0000-000000000001', 'COMPLETED', 'NORMAL', '2024-01-14', 'Devolución de productos', 'MBL003', 'HBL003', true, true, true, false),
    ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'DELIVER', 'DEL-004', NULL, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000005', 'Ana Martínez', '00000000-0000-0000-0000-000000000001', 'PENDING_CONFIRMATION', 'URGENT', '2024-01-17', 'Inspección de contenedor', 'MBL004', 'HBL004', false, false, false, true),
    ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-005', '00000000-0000-0000-0000-000000000001', NULL, '00000000-0000-0000-0000-000000000006', 'Roberto Silva', '00000000-0000-0000-0000-000000000001', 'CANCELLED', 'NORMAL', '2024-01-13', 'Tarea cancelada por cliente', 'MBL005', 'HBL005', true, false, false, false)
ON CONFLICT (id) DO NOTHING;

-- Insert demo task photos
INSERT INTO task_photos (id, task_id, photo_url, photo_type) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '/photos/receipt-001.jpg', 'RECEIPT'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', '/photos/additional-001.jpg', 'ADDITIONAL')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- DATABASE INDEXES FOR PERFORMANCE OPTIMIZATION
-- ============================================================================
-- These indexes are based on the actual JOIN and WHERE conditions used
-- in the repository queries to optimize query performance

-- Tasks table indexes
-- organization_id is the most common filter
CREATE INDEX IF NOT EXISTS idx_tasks_organization_id ON tasks(organization_id);
-- For sorting by creation date
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at DESC);
-- Composite index for common query: organization + status
CREATE INDEX IF NOT EXISTS idx_tasks_org_status ON tasks(organization_id, status);
-- Composite index for common query: organization + courier (for filtering)
CREATE INDEX IF NOT EXISTS idx_tasks_org_courier ON tasks(organization_id, courier_id);
-- For tasksByStatus queries
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
-- For tasksByCourier queries
CREATE INDEX IF NOT EXISTS idx_tasks_courier_id ON tasks(courier_id);
-- For reference number searches (unique per organization)
CREATE INDEX IF NOT EXISTS idx_tasks_org_reference ON tasks(organization_id, reference_number);
-- Foreign keys already indexed by PostgreSQL, but adding explicit ones for clarity:
-- client_id, provider_id, address_override_id, courier_id are already indexed as FKs

-- Clients table indexes
-- organization_id is the most common filter
CREATE INDEX IF NOT EXISTS idx_clients_organization_id ON clients(organization_id);
-- For joins with Addresses
CREATE INDEX IF NOT EXISTS idx_clients_address_id ON clients(address_id);
-- Composite index for searchByName: organization + name
CREATE INDEX IF NOT EXISTS idx_clients_org_name ON clients(organization_id, name);
-- Composite index for filtering active clients
CREATE INDEX IF NOT EXISTS idx_clients_org_active ON clients(organization_id, is_active);

-- Providers table indexes
-- organization_id is the most common filter
CREATE INDEX IF NOT EXISTS idx_providers_organization_id ON providers(organization_id);
-- For joins with Addresses
CREATE INDEX IF NOT EXISTS idx_providers_address_id ON providers(address_id);
-- Composite index for searchByName: organization + name
CREATE INDEX IF NOT EXISTS idx_providers_org_name ON providers(organization_id, name);
-- Composite index for filtering active providers
CREATE INDEX IF NOT EXISTS idx_providers_org_active ON providers(organization_id, is_active);

-- Addresses table indexes
-- For searchByCity queries (LIKE queries on city)
CREATE INDEX IF NOT EXISTS idx_addresses_city ON addresses(city);

-- Couriers table indexes
-- organization_id is the most common filter
CREATE INDEX IF NOT EXISTS idx_couriers_organization_id ON couriers(organization_id);
-- user_id is UNIQUE so already indexed
-- For filtering active couriers
CREATE INDEX IF NOT EXISTS idx_couriers_org_active ON couriers(organization_id, is_active);

-- Users table indexes
-- organization_id is the most common filter
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users(organization_id);
-- email is UNIQUE so already indexed
-- Composite index for filtering active users
CREATE INDEX IF NOT EXISTS idx_users_org_active ON users(organization_id, is_active);

-- Task Photos table indexes
-- For queries by task_id (most common)
CREATE INDEX IF NOT EXISTS idx_task_photos_task_id ON task_photos(task_id);