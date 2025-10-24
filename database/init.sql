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

-- Clients table
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(80) NOT NULL,
    province VARCHAR(80) NOT NULL,
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
    address VARCHAR(255) NOT NULL,
    city VARCHAR(80),
    province VARCHAR(80),
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
    address_override VARCHAR(255),
    city VARCHAR(100),
    province VARCHAR(100),
    contact VARCHAR(100),
    courier_id UUID REFERENCES couriers(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'PENDING_CONFIRMATION', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('NORMAL', 'URGENT')),
    scheduled_date VARCHAR(10),
    notes TEXT,
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

-- Insert demo clients
INSERT INTO clients (id, organization_id, name, address, city, province, phone_number, email, is_active) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Cliente Demo S.A.', 'Av. Corrientes 1234', 'Buenos Aires', 'CABA', '+54911234567', 'cliente@demo.com', true),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Empresa Logística ABC', 'Av. Santa Fe 5678', 'Buenos Aires', 'CABA', '+54911234568', 'contacto@empresaabc.com', true)
ON CONFLICT (id) DO NOTHING;

-- Insert demo providers
INSERT INTO providers (id, organization_id, name, address, city, province, contact_name, contact_phone, is_active) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Proveedor Demo S.A.', 'Av. Santa Fe 5678', 'Buenos Aires', 'CABA', 'Juan Pérez', '+54911234568', true),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Transportes XYZ', 'Av. Rivadavia 9012', 'Buenos Aires', 'CABA', 'María González', '+54911234569', true)
ON CONFLICT (id) DO NOTHING;

-- Insert demo courier (linked to user)
INSERT INTO couriers (id, user_id, organization_id, name, phone_number, address, vehicle_type, is_active) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Carlos López', '+54911234569', 'Av. Rivadavia 9012', 'Moto', true)
ON CONFLICT (id) DO NOTHING;

-- Insert demo tasks
INSERT INTO tasks (id, organization_id, type, reference_number, client_id, provider_id, address_override, city, province, contact, courier_id, status, priority, scheduled_date, notes, mbl, hbl, freight_cert, fo_cert, bunker_cert, photo_required) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-001', '00000000-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, NULL, '00000000-0000-0000-0000-000000000001', 'PENDING', 'URGENT', '2024-01-15', 'Recoger documentos urgentes', 'MBL001', 'HBL001', true, false, true, true),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'DELIVER', 'DEL-002', NULL, '00000000-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, '00000000-0000-0000-0000-000000000001', 'CONFIRMED', 'NORMAL', '2024-01-16', 'Entrega de mercadería', 'MBL002', 'HBL002', false, true, false, true),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-003', '00000000-0000-0000-0000-000000000001', NULL, 'Av. 9 de Julio 999', 'Buenos Aires', 'CABA', 'Carlos López', '00000000-0000-0000-0000-000000000001', 'COMPLETED', 'NORMAL', '2024-01-14', 'Devolución de productos', 'MBL003', 'HBL003', true, true, true, false),
    ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'DELIVER', 'DEL-004', NULL, '00000000-0000-0000-0000-000000000001', 'Av. Rivadavia 1111', 'Buenos Aires', 'CABA', 'Ana Martínez', '00000000-0000-0000-0000-000000000001', 'PENDING_CONFIRMATION', 'URGENT', '2024-01-17', 'Inspección de contenedor', 'MBL004', 'HBL004', false, false, false, true),
    ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-005', '00000000-0000-0000-0000-000000000001', NULL, 'Av. Callao 2222', 'Buenos Aires', 'CABA', 'Roberto Silva', '00000000-0000-0000-0000-000000000001', 'CANCELLED', 'NORMAL', '2024-01-13', 'Tarea cancelada por cliente', 'MBL005', 'HBL005', true, false, false, false)
ON CONFLICT (id) DO NOTHING;

-- Insert demo task photos
INSERT INTO task_photos (id, task_id, photo_url, photo_type) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '/photos/receipt-001.jpg', 'RECEIPT'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', '/photos/additional-001.jpg', 'ADDITIONAL')
ON CONFLICT (id) DO NOTHING;