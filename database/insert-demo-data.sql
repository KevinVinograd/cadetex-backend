-- Insert initial data
INSERT INTO organizations (id, name, created_at, updated_at) VALUES 
    ('00000000-0000-0000-0000-000000000001', 'Cadetex Demo Organization', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert demo users (with hashed passwords)
-- Password for all demo users: "password123"
INSERT INTO users (id, organization_id, name, email, password_hash, role, created_at, updated_at) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Admin Demo', 'admin@cadetex.com', '$2a$10$gYdF8HSMLRzGXRtEnxttZuj0L/lVKEAGKz2OwosuvQdq8T4MliWJu', 'SUPERADMIN', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Org Admin Demo', 'orgadmin@cadetex.com', '$2a$10$gYdF8HSMLRzGXRtEnxttZuj0L/lVKEAGKz2OwosuvQdq8T4MliWJu', 'ORGADMIN', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Carlos López', 'carlos@cadetex.com', '$2a$10$gYdF8HSMLRzGXRtEnxttZuj0L/lVKEAGKz2OwosuvQdq8T4MliWJu', 'COURIER', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert demo clients
INSERT INTO clients (id, organization_id, name, address, city, province, phone_number, email, is_active, created_at, updated_at) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Cliente Demo S.A.', 'Av. Corrientes 1234', 'Buenos Aires', 'CABA', '+54911234567', 'cliente@demo.com', true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Empresa Logística ABC', 'Av. Santa Fe 5678', 'Buenos Aires', 'CABA', '+54911234568', 'contacto@empresaabc.com', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert demo providers
INSERT INTO providers (id, organization_id, name, address, city, province, contact_name, contact_phone, is_active, created_at, updated_at) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Proveedor Demo S.A.', 'Av. Santa Fe 5678', 'Buenos Aires', 'CABA', 'Juan Pérez', '+54911234568', true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Transportes XYZ', 'Av. Rivadavia 9012', 'Buenos Aires', 'CABA', 'María González', '+54911234569', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert demo courier (linked to user)
INSERT INTO couriers (id, user_id, organization_id, name, phone_number, address, vehicle_type, is_active, created_at, updated_at) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Carlos López', '+54911234569', 'Av. Rivadavia 9012', 'Moto', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert demo tasks
INSERT INTO tasks (id, organization_id, type, reference_number, client_id, provider_id, address_override, city, province, contact, courier_id, status, priority, scheduled_date, notes, mbl, hbl, freight_cert, fo_cert, bunker_cert, photo_required, created_at, updated_at) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-001', '00000000-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, NULL, '00000000-0000-0000-0000-000000000001', 'PENDING', 'URGENT', '2024-01-15', 'Recoger documentos urgentes', 'MBL001', 'HBL001', true, false, true, true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'DELIVER', 'DEL-002', NULL, '00000000-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, '00000000-0000-0000-0000-000000000001', 'CONFIRMED', 'NORMAL', '2024-01-16', 'Entrega de mercadería', 'MBL002', 'HBL002', false, true, false, true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-003', '00000000-0000-0000-0000-000000000001', NULL, 'Av. 9 de Julio 999', 'Buenos Aires', 'CABA', 'Carlos López', '00000000-0000-0000-0000-000000000001', 'COMPLETED', 'NORMAL', '2024-01-14', 'Devolución de productos', 'MBL003', 'HBL003', true, true, true, false, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'DELIVER', 'DEL-004', NULL, '00000000-0000-0000-0000-000000000001', 'Av. Rivadavia 1111', 'Buenos Aires', 'CABA', 'Ana Martínez', '00000000-0000-0000-0000-000000000001', 'PENDING_CONFIRMATION', 'URGENT', '2024-01-17', 'Inspección de contenedor', 'MBL004', 'HBL004', false, false, false, true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'RETIRE', 'RET-005', '00000000-0000-0000-0000-000000000001', NULL, 'Av. Callao 2222', 'Buenos Aires', 'CABA', 'Roberto Silva', '00000000-0000-0000-0000-000000000001', 'CANCELLED', 'NORMAL', '2024-01-13', 'Tarea cancelada por cliente', 'MBL005', 'HBL005', true, false, false, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert demo task photos
INSERT INTO task_photos (id, task_id, photo_url, photo_type) VALUES 
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '/photos/receipt-001.jpg', 'RECEIPT'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', '/photos/additional-001.jpg', 'ADDITIONAL')
ON CONFLICT (id) DO NOTHING;

