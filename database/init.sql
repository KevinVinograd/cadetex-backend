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

-- Users table
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
    city VARCHAR(80) NOT NULL,
    province VARCHAR(80) NOT NULL,
    phone_number VARCHAR(50),
    email VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Couriers table
CREATE TABLE IF NOT EXISTS couriers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    email VARCHAR(150),
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
    type VARCHAR(30) NOT NULL CHECK (type IN ('PICKUP', 'DELIVERY', 'RETURN', 'INSPECTION')),
    reference_number VARCHAR(50),
    client_id UUID REFERENCES clients(id),
    provider_id UUID REFERENCES providers(id),
    courier_id UUID REFERENCES couriers(id),
    address_override VARCHAR(255),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    scheduled_date DATE,
    notes TEXT,
    mbl VARCHAR(50),
    hbl VARCHAR(50),
    freight_cert BOOLEAN DEFAULT FALSE,
    fo_cert BOOLEAN DEFAULT FALSE,
    bunker_cert BOOLEAN DEFAULT FALSE,
    linked_task_id UUID REFERENCES tasks(id),
    receipt_photo_url VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Task Photos table
CREATE TABLE IF NOT EXISTS task_photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Task History table
CREATE TABLE IF NOT EXISTS task_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_clients_organization_id ON clients(organization_id);
CREATE INDEX IF NOT EXISTS idx_providers_organization_id ON providers(organization_id);
CREATE INDEX IF NOT EXISTS idx_couriers_organization_id ON couriers(organization_id);
CREATE INDEX IF NOT EXISTS idx_tasks_organization_id ON tasks(organization_id);
CREATE INDEX IF NOT EXISTS idx_tasks_courier_id ON tasks(courier_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_task_photos_task_id ON task_photos(task_id);
CREATE INDEX IF NOT EXISTS idx_task_history_task_id ON task_history(task_id);

-- Insert initial data
INSERT INTO organizations (id, name) VALUES 
    ('00000000-0000-0000-0000-000000000001', 'Cadetex Demo Organization')
ON CONFLICT (id) DO NOTHING;

-- Insert demo superadmin user (password: admin123)
INSERT INTO users (id, organization_id, name, email, password_hash, role) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Super Admin', 'admin@cadetex.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SUPERADMIN')
ON CONFLICT (id) DO NOTHING;

-- Insert demo organization admin user (password: orgadmin123)
INSERT INTO users (id, organization_id, name, email, password_hash, role) VALUES 
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Org Admin', 'orgadmin@cadetex.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ORGADMIN')
ON CONFLICT (id) DO NOTHING;

-- Insert demo courier user (password: courier123)
INSERT INTO users (id, organization_id, name, email, password_hash, role) VALUES 
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Courier Demo', 'courier@cadetex.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'COURIER')
ON CONFLICT (id) DO NOTHING;

-- Insert demo client
INSERT INTO clients (id, organization_id, name, address, city, province, phone_number, email) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Cliente Demo S.A.', 'Av. Corrientes 1234', 'Buenos Aires', 'CABA', '+54911234567', 'cliente@demo.com')
ON CONFLICT (id) DO NOTHING;

-- Insert demo provider
INSERT INTO providers (id, organization_id, name, address, city, province, contact_name, contact_phone) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Proveedor Demo S.A.', 'Av. Santa Fe 5678', 'Buenos Aires', 'CABA', 'Juan Pérez', '+54911234568')
ON CONFLICT (id) DO NOTHING;

-- Insert demo courier
INSERT INTO couriers (id, organization_id, name, phone_number, email, address, vehicle_type) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Carlos López', '+54911234569', 'carlos@cadetex.com', 'Av. Rivadavia 9012', 'Moto')
ON CONFLICT (id) DO NOTHING;

-- Insert demo tasks
INSERT INTO tasks (id, organization_id, type, reference_number, client_id, provider_id, courier_id, status, priority, scheduled_date, notes, mbl, hbl, freight_cert, fo_cert, bunker_cert) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'PICKUP', 'PICK-001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'PENDING', 'HIGH', '2024-01-15', 'Recoger documentos urgentes', 'MBL001', 'HBL001', true, false, true),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'DELIVERY', 'DEL-002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'IN_PROGRESS', 'NORMAL', '2024-01-16', 'Entrega de mercadería', 'MBL002', 'HBL002', false, true, false),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'RETURN', 'RET-003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'COMPLETED', 'LOW', '2024-01-14', 'Devolución de productos', 'MBL003', 'HBL003', true, true, true),
    ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'INSPECTION', 'INS-004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'PENDING', 'URGENT', '2024-01-17', 'Inspección de contenedor', 'MBL004', 'HBL004', false, false, false),
    ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'PICKUP', 'PICK-005', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'CANCELLED', 'NORMAL', '2024-01-13', 'Tarea cancelada por cliente', 'MBL005', 'HBL005', true, false, false)
ON CONFLICT (id) DO NOTHING;
