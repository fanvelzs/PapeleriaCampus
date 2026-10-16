-- SOLO base local de pruebas: restaura exactamente los 12 registros del laboratorio.
BEGIN;
TRUNCATE TABLE producto RESTART IDENTITY;
INSERT INTO producto (codigo, nombre, categoria, precio, stock) VALUES
('P001', 'Cuaderno universitario', 'Papelería', 65.00, 25),
('P002', 'Cuaderno cuadriculado', 'Papelería', 72.50, 12),
('P003', 'Lapicero azul', 'Papelería', 12.00, 80),
('P004', 'Resma de papel carta', 'Papelería', 185.00, 0),
('T001', 'Memoria USB 32 GB', 'Tecnología', 210.00, 8),
('T002', 'Mouse inalámbrico', 'Tecnología', 320.00, 6),
('T003', 'Teclado USB', 'Tecnología', 450.00, 4),
('T004', 'Cable HDMI', 'Tecnología', 180.00, 0),
('A001', 'Mochila escolar', 'Accesorios', 850.00, 5),
('A002', 'Estuche de lápices', 'Accesorios', 95.00, 18),
('A003', 'Botella reutilizable', 'Accesorios', 240.00, 0),
('A004', 'Regla metálica', 'Accesorios', 45.00, 20);
COMMIT;
