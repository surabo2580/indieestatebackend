ALTER TABLE categories
    ADD COLUMN parent_id UUID REFERENCES categories (id);

ALTER TABLE services
    ADD COLUMN parent_id UUID REFERENCES services (id);

CREATE INDEX idx_categories_parent_id ON categories (parent_id);
CREATE INDEX idx_services_parent_id ON services (parent_id);

-- Category subcategories
INSERT INTO categories (id, slug, name, icon, sort_order, is_active, parent_id) VALUES
    ('0a1c0001-0000-4000-8000-000000000011', 'data-entry', 'Data entry', 'keyboard', 1, true, '0a1c0001-0000-4000-8000-000000000001'),
    ('0a1c0001-0000-4000-8000-000000000012', 'teacher', 'Teacher', 'school', 2, true, '0a1c0001-0000-4000-8000-000000000001'),
    ('0a1c0001-0000-4000-8000-000000000013', 'it', 'IT', 'computer', 3, true, '0a1c0001-0000-4000-8000-000000000001'),
    ('0a1c0001-0000-4000-8000-000000000014', 'salesman', 'Salesman', 'handshake', 4, true, '0a1c0001-0000-4000-8000-000000000001'),

    ('0a1c0001-0000-4000-8000-000000000021', 'sedan', 'Sedan', 'directions_car', 1, true, '0a1c0001-0000-4000-8000-000000000002'),
    ('0a1c0001-0000-4000-8000-000000000022', 'suv', 'SUV', 'directions_car', 2, true, '0a1c0001-0000-4000-8000-000000000002'),
    ('0a1c0001-0000-4000-8000-000000000023', 'hatchback', 'Hatchback', 'directions_car', 3, true, '0a1c0001-0000-4000-8000-000000000002'),
    ('0a1c0001-0000-4000-8000-000000000024', 'luxury-car', 'Luxury', 'directions_car', 4, true, '0a1c0001-0000-4000-8000-000000000002'),

    ('0a1c0001-0000-4000-8000-000000000031', 'motorcycle', 'Motorcycle', 'two_wheeler', 1, true, '0a1c0001-0000-4000-8000-000000000003'),
    ('0a1c0001-0000-4000-8000-000000000032', 'scooter', 'Scooter', 'two_wheeler', 2, true, '0a1c0001-0000-4000-8000-000000000003'),
    ('0a1c0001-0000-4000-8000-000000000033', 'electric-bike', 'Electric', 'two_wheeler', 3, true, '0a1c0001-0000-4000-8000-000000000003'),
    ('0a1c0001-0000-4000-8000-000000000034', 'sports-bike', 'Sports', 'two_wheeler', 4, true, '0a1c0001-0000-4000-8000-000000000003'),

    ('0a1c0001-0000-4000-8000-000000000041', 'apartment', 'Apartment', 'apartment', 1, true, '0a1c0001-0000-4000-8000-000000000004'),
    ('0a1c0001-0000-4000-8000-000000000042', 'villa', 'Villa', 'villa', 2, true, '0a1c0001-0000-4000-8000-000000000004'),
    ('0a1c0001-0000-4000-8000-000000000043', 'plot', 'Plot', 'map', 3, true, '0a1c0001-0000-4000-8000-000000000004'),
    ('0a1c0001-0000-4000-8000-000000000044', 'commercial', 'Commercial', 'store', 4, true, '0a1c0001-0000-4000-8000-000000000004');

-- Replace the old service catalog with the requested top-level services
UPDATE services SET is_active = false
WHERE slug IN ('home_cleaning', 'water_services', 'plumbing');

UPDATE services SET sort_order = 1 WHERE slug = 'salon';

INSERT INTO services (id, slug, name, icon, sort_order, is_active, parent_id) VALUES
    ('0a1c0002-0000-4000-8000-000000000005', 'toilet', 'Toilet', 'wc', 2, true, NULL),
    ('0a1c0002-0000-4000-8000-000000000006', 'pets', 'Pets', 'pets', 3, true, NULL),
    ('0a1c0002-0000-4000-8000-000000000007', 'car-wash', 'Car wash', 'local_car_wash', 4, true, NULL),

    ('0a1c0002-0000-4000-8000-000000000011', 'salon-men', 'Men', 'content_cut', 1, true, '0a1c0002-0000-4000-8000-000000000002'),
    ('0a1c0002-0000-4000-8000-000000000012', 'salon-women', 'Women', 'content_cut', 2, true, '0a1c0002-0000-4000-8000-000000000002'),
    ('0a1c0002-0000-4000-8000-000000000013', 'salon-unisex', 'Unisex', 'content_cut', 3, true, '0a1c0002-0000-4000-8000-000000000002'),
    ('0a1c0002-0000-4000-8000-000000000014', 'salon-kids', 'Kids', 'content_cut', 4, true, '0a1c0002-0000-4000-8000-000000000002'),

    ('0a1c0002-0000-4000-8000-000000000021', 'toilet-cleaning', 'Cleaning', 'wc', 1, true, '0a1c0002-0000-4000-8000-000000000005'),
    ('0a1c0002-0000-4000-8000-000000000022', 'toilet-repair', 'Repair', 'build', 2, true, '0a1c0002-0000-4000-8000-000000000005'),
    ('0a1c0002-0000-4000-8000-000000000023', 'toilet-installation', 'Installation', 'plumbing', 3, true, '0a1c0002-0000-4000-8000-000000000005'),

    ('0a1c0002-0000-4000-8000-000000000031', 'pets-grooming', 'Grooming', 'pets', 1, true, '0a1c0002-0000-4000-8000-000000000006'),
    ('0a1c0002-0000-4000-8000-000000000032', 'pets-walking', 'Walking', 'pets', 2, true, '0a1c0002-0000-4000-8000-000000000006'),
    ('0a1c0002-0000-4000-8000-000000000033', 'pets-boarding', 'Boarding', 'pets', 3, true, '0a1c0002-0000-4000-8000-000000000006'),
    ('0a1c0002-0000-4000-8000-000000000034', 'pets-vet', 'Vet', 'local_hospital', 4, true, '0a1c0002-0000-4000-8000-000000000006'),

    ('0a1c0002-0000-4000-8000-000000000041', 'car-wash-hatchback', 'Hatchback', 'local_car_wash', 1, true, '0a1c0002-0000-4000-8000-000000000007'),
    ('0a1c0002-0000-4000-8000-000000000042', 'car-wash-sedan', 'Sedan', 'local_car_wash', 2, true, '0a1c0002-0000-4000-8000-000000000007'),
    ('0a1c0002-0000-4000-8000-000000000043', 'car-wash-suv', 'SUV', 'local_car_wash', 3, true, '0a1c0002-0000-4000-8000-000000000007'),
    ('0a1c0002-0000-4000-8000-000000000044', 'car-wash-bike', 'Bike wash', 'two_wheeler', 4, true, '0a1c0002-0000-4000-8000-000000000007');
