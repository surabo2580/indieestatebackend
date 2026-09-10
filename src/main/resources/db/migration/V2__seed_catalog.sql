INSERT INTO categories (id, slug, name, icon, sort_order, is_active) VALUES
    ('0a1c0001-0000-4000-8000-000000000001', 'jobs', 'Jobs', 'work', 1, true),
    ('0a1c0001-0000-4000-8000-000000000002', 'cars', 'Cars', 'directions_car', 2, true),
    ('0a1c0001-0000-4000-8000-000000000003', 'bikes', 'Bikes', 'two_wheeler', 3, true),
    ('0a1c0001-0000-4000-8000-000000000004', 'properties', 'Properties', 'home', 4, true);

INSERT INTO services (id, slug, name, icon, sort_order, is_active) VALUES
    ('0a1c0002-0000-4000-8000-000000000001', 'home_cleaning', 'Home cleaning', 'cleaning_services', 1, true),
    ('0a1c0002-0000-4000-8000-000000000002', 'salon', 'Salon', 'content_cut', 2, true),
    ('0a1c0002-0000-4000-8000-000000000003', 'water_services', 'Water services', 'water_drop', 3, true),
    ('0a1c0002-0000-4000-8000-000000000004', 'plumbing', 'Plumbing', 'plumbing', 4, true);

INSERT INTO forms (id, code, type, category_id, service_id, title, version, is_active, form_schema) VALUES
(
    '0a1c0003-0000-4000-8000-000000000001',
    'JOB_POST',
    'JOB',
    '0a1c0001-0000-4000-8000-000000000001',
    NULL,
    'Post a job',
    1,
    true,
    '{
      "fields": [
        {"key": "jobTitle", "label": "Job title", "type": "text", "required": true, "maxLength": 80},
        {"key": "company", "label": "Company", "type": "text", "required": true, "maxLength": 80},
        {"key": "jobType", "label": "Job type", "type": "select", "required": true, "options": ["Full-time", "Part-time", "Contract", "Internship"]},
        {"key": "salary", "label": "Salary", "type": "number", "required": false, "min": 0},
        {"key": "location", "label": "Location", "type": "text", "required": true, "maxLength": 120},
        {"key": "description", "label": "Description", "type": "textarea", "required": true, "maxLength": 2000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000002',
    'CAR_SELL',
    'SELL',
    '0a1c0001-0000-4000-8000-000000000002',
    NULL,
    'Sell a car',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Ad title", "type": "text", "required": true, "maxLength": 80},
        {"key": "price", "label": "Price", "type": "number", "required": true, "min": 0},
        {"key": "brand", "label": "Brand", "type": "select", "required": true, "options": ["Maruti", "Hyundai", "Honda", "Tata", "Mahindra", "Toyota", "Other"]},
        {"key": "model", "label": "Model", "type": "text", "required": true, "maxLength": 80},
        {"key": "year", "label": "Year", "type": "number", "required": true, "min": 1990, "max": 2030},
        {"key": "kmDriven", "label": "KM driven", "type": "number", "required": true, "min": 0},
        {"key": "fuel", "label": "Fuel", "type": "select", "required": true, "options": ["Petrol", "Diesel", "CNG", "Electric", "Hybrid"]},
        {"key": "description", "label": "Description", "type": "textarea", "required": false, "maxLength": 2000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000003',
    'BIKE_SELL',
    'SELL',
    '0a1c0001-0000-4000-8000-000000000003',
    NULL,
    'Sell a bike',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Ad title", "type": "text", "required": true, "maxLength": 80},
        {"key": "price", "label": "Price", "type": "number", "required": true, "min": 0},
        {"key": "brand", "label": "Brand", "type": "select", "required": true, "options": ["Hero", "Honda", "Bajaj", "TVS", "Royal Enfield", "Yamaha", "Other"]},
        {"key": "year", "label": "Year", "type": "number", "required": true, "min": 1990, "max": 2030},
        {"key": "kmDriven", "label": "KM driven", "type": "number", "required": true, "min": 0},
        {"key": "description", "label": "Description", "type": "textarea", "required": false, "maxLength": 2000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000004',
    'PROPERTY_SELL',
    'SELL',
    '0a1c0001-0000-4000-8000-000000000004',
    NULL,
    'Sell a property',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Ad title", "type": "text", "required": true, "maxLength": 80},
        {"key": "price", "label": "Price", "type": "number", "required": true, "min": 0},
        {"key": "propertyType", "label": "Property type", "type": "select", "required": true, "options": ["Apartment", "Independent house", "Villa", "Plot", "Commercial"]},
        {"key": "bedrooms", "label": "Bedrooms", "type": "number", "required": false, "min": 0},
        {"key": "bathrooms", "label": "Bathrooms", "type": "number", "required": false, "min": 0},
        {"key": "areaSqft", "label": "Area (sqft)", "type": "number", "required": true, "min": 1},
        {"key": "furnished", "label": "Furnished", "type": "boolean", "required": false},
        {"key": "location", "label": "Location", "type": "text", "required": true, "maxLength": 160},
        {"key": "description", "label": "Description", "type": "textarea", "required": false, "maxLength": 2000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000005',
    'HOME_CLEANING',
    'SERVICE_REQUEST',
    NULL,
    '0a1c0002-0000-4000-8000-000000000001',
    'Request home cleaning',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Request title", "type": "text", "required": true, "maxLength": 80},
        {"key": "propertyType", "label": "Property type", "type": "select", "required": true, "options": ["Apartment", "Independent house", "Office"]},
        {"key": "rooms", "label": "Number of rooms", "type": "number", "required": true, "min": 1},
        {"key": "preferredDate", "label": "Preferred date", "type": "date", "required": true},
        {"key": "address", "label": "Address", "type": "textarea", "required": true, "maxLength": 300},
        {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "maxLength": 1000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000006',
    'SALON',
    'SERVICE_REQUEST',
    NULL,
    '0a1c0002-0000-4000-8000-000000000002',
    'Book a salon service',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Request title", "type": "text", "required": true, "maxLength": 80},
        {"key": "serviceType", "label": "Service type", "type": "select", "required": true, "options": ["Haircut", "Hair color", "Facial", "Spa", "Other"]},
        {"key": "preferredDate", "label": "Preferred date", "type": "date", "required": true},
        {"key": "address", "label": "Address", "type": "textarea", "required": true, "maxLength": 300},
        {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "maxLength": 1000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000007',
    'WATER_SERVICES',
    'SERVICE_REQUEST',
    NULL,
    '0a1c0002-0000-4000-8000-000000000003',
    'Request water service',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Request title", "type": "text", "required": true, "maxLength": 80},
        {"key": "issueType", "label": "Issue type", "type": "select", "required": true, "options": ["Tanker", "Purifier install", "Purifier repair", "Leakage", "Other"]},
        {"key": "preferredDate", "label": "Preferred date", "type": "date", "required": true},
        {"key": "address", "label": "Address", "type": "textarea", "required": true, "maxLength": 300},
        {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "maxLength": 1000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000008',
    'PLUMBING',
    'SERVICE_REQUEST',
    NULL,
    '0a1c0002-0000-4000-8000-000000000004',
    'Request plumbing',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Request title", "type": "text", "required": true, "maxLength": 80},
        {"key": "issueType", "label": "Issue type", "type": "multiselect", "required": true, "options": ["Tap leak", "Pipe burst", "Clogged drain", "Toilet", "Water heater", "Other"]},
        {"key": "preferredDate", "label": "Preferred date", "type": "date", "required": true},
        {"key": "address", "label": "Address", "type": "textarea", "required": true, "maxLength": 300},
        {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "maxLength": 1000}
      ]
    }'::jsonb
);
