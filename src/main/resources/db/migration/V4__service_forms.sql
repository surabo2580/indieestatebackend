-- Service posting forms (Salon + V3 top-level services). Upsert by code.

INSERT INTO forms (id, code, type, category_id, service_id, title, version, is_active, form_schema) VALUES
(
    '0a1c0003-0000-4000-8000-000000000101',
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
    '0a1c0003-0000-4000-8000-000000000102',
    'TOILET_SERVICE',
    'SERVICE_REQUEST',
    NULL,
    '0a1c0002-0000-4000-8000-000000000005',
    'Toilet service request',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Request title", "type": "text", "required": true, "maxLength": 80},
        {"key": "serviceType", "label": "Service type", "type": "select", "required": true, "options": ["Cleaning", "Repair", "Installation", "Other"]},
        {"key": "preferredDate", "label": "Preferred date", "type": "date", "required": true},
        {"key": "address", "label": "Address", "type": "textarea", "required": true, "maxLength": 300},
        {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "maxLength": 1000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000103',
    'PETS_SERVICE',
    'SERVICE_REQUEST',
    NULL,
    '0a1c0002-0000-4000-8000-000000000006',
    'Pet service request',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Request title", "type": "text", "required": true, "maxLength": 80},
        {"key": "serviceType", "label": "Service type", "type": "select", "required": true, "options": ["Grooming", "Walking", "Boarding", "Vet", "Other"]},
        {"key": "petType", "label": "Pet type", "type": "select", "required": true, "options": ["Dog", "Cat", "Bird", "Other"]},
        {"key": "preferredDate", "label": "Preferred date", "type": "date", "required": true},
        {"key": "address", "label": "Address", "type": "textarea", "required": true, "maxLength": 300},
        {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "maxLength": 1000}
      ]
    }'::jsonb
),
(
    '0a1c0003-0000-4000-8000-000000000104',
    'CAR_WASH_SERVICE',
    'SERVICE_REQUEST',
    NULL,
    '0a1c0002-0000-4000-8000-000000000007',
    'Car wash request',
    1,
    true,
    '{
      "fields": [
        {"key": "title", "label": "Request title", "type": "text", "required": true, "maxLength": 80},
        {"key": "vehicleType", "label": "Vehicle type", "type": "select", "required": true, "options": ["Hatchback", "Sedan", "SUV", "Bike", "Other"]},
        {"key": "preferredDate", "label": "Preferred date", "type": "date", "required": true},
        {"key": "address", "label": "Address", "type": "textarea", "required": true, "maxLength": 300},
        {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "maxLength": 1000}
      ]
    }'::jsonb
)
ON CONFLICT (code) DO UPDATE SET
    service_id = EXCLUDED.service_id,
    title = EXCLUDED.title,
    form_schema = EXCLUDED.form_schema,
    is_active = true;
