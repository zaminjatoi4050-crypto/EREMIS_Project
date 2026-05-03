-- ============================================================
--  EREMIS — Sample Data
--  Run AFTER schema.sql
--  IMPORTANT: Default passwords are using legacy SHA-256 format for compatibility.
--  These will be transparently upgraded to BCrypt on first login.
--  
--  Default admin:  username=admin@EREMIS.com  password=EREMIS_@#$786
--  Default seller: username=seller1@example.com  password=Seller@123
--  Default buyer:  username=john   password=User@123
-- ============================================================

USE eremis_db;

-- SHA-256 of "EREMIS_@#$786" = 22908fe609918a823db716c0c72004040f416dd37b8582578547ec144a9280f1
-- SHA-256 of "User@123"  = 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4

INSERT INTO users (full_name, email, username, password_hash, role) VALUES
('System Administrator', 'admin@EREMIS.com', 'admin@EREMIS.com', '22908fe609918a823db716c0c72004040f416dd37b8582578547ec144a9280f1', 'ADMIN'),
('John Doe',             'john@example.com',  'john',  '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'USER'),
('Sarah Smith',          'sarah@example.com', 'sarah', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'USER'),
('Mike Johnson',         'mike@example.com',  'mike',  '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'USER'),
('Sara Seller',          'seller1@example.com','seller1@example.com', 'bd28c94800c2be055b3329f8dd63a3d5a4137c0def2517bf4fce85eb11e62853', 'SELLER');

INSERT INTO properties (title, description, location, city, price, area_sqft, bedrooms, bathrooms, type, status, owner_name, owner_contact, listed_by) VALUES
('Luxury Villa in DHA Phase 5',       'A stunning 7-marla villa with modern architecture, large garden, and 24/7 security.',     'DHA Phase 5, Block C', 'Lahore',    25000000.00, 3000, 5, 4, 'VILLA',       'AVAILABLE', 'Ahmed Khan',    '0300-1234567', 1),
('Studio Apartment in Gulberg III',   'Well-furnished studio apartment on 4th floor, gated community, near MM Alam Road.',       'Gulberg III',          'Lahore',     8500000.00, 700,  1, 1, 'APARTMENT',   'AVAILABLE', 'Fatima Malik',  '0321-9876543', 1),
('Commercial Plaza F-7',              'Prime location plaza with 4 shops on ground floor, ideal for retail or office use.',      'F-7 Markaz',           'Islamabad', 45000000.00, 4500, 0, 2, 'COMMERCIAL',  'AVAILABLE', 'Bilal Tariq',   '0333-5551234', 1),
('3-Bed House in Bahria Town',        'Beautiful double-story house in Bahria Town with parking for 2 cars and a garden.',       'Bahria Town, Sector C','Karachi',   18000000.00, 2200, 3, 3, 'HOUSE',       'RESERVED',  'Zara Hussain',  '0312-7778899', 1),
('Agricultural Land — 5 Acres',      '5-acre fertile agricultural land near motorway, ideal for farming or investment.',        'Motorway Road',        'Multan',     6000000.00, 217800,0, 0, 'LAND',        'AVAILABLE', 'Nasir Iqbal',   '0346-1122334', 1),
('Modern Condo Clifton Block 9',      '2-bed luxury condo with sea view, fully equipped kitchen, and rooftop access.',           'Clifton Block 9',      'Karachi',   22000000.00, 1500, 2, 2, 'CONDO',       'AVAILABLE', 'Hina Rauf',     '0300-9998877', 1),
('Penthouse in E-11',                 '4-bed penthouse with wraparound terrace and panoramic Margalla Hills views.',             'E-11/2',               'Islamabad', 55000000.00, 5000, 4, 4, 'APARTMENT',   'AVAILABLE', 'Omar Farooq',   '0322-4446655', 1),
('Townhouse in DHA Phase 6',          'Corner townhouse in DHA with extra land, solar panels installed, excellent condition.',   'DHA Phase 6',          'Lahore',    32000000.00, 2800, 4, 3, 'HOUSE',       'SOLD',      'Asma Chaudhry', '0301-3335566', 1),
('Warehouse near Port Qasim',         'Industrial warehouse 10,000 sqft with loading bay, 3-phase electricity, 24hr security.', 'Port Qasim Road',      'Karachi',   38000000.00, 10000,0, 2, 'COMMERCIAL',  'AVAILABLE', 'Raza Sheikh',   '0340-2224455', 1),
('Affordable Apartment in Johar Town','First floor 2-bed apartment in Johar Town, quiet street, near schools and market.',      'Johar Town',           'Lahore',     7200000.00, 900,  2, 1, 'APARTMENT',   'AVAILABLE', 'Sadia Noor',    '0315-6667788', 1);

INSERT INTO property_images (property_id, file_path, is_primary) VALUES
(1, '/images/properties/villa_dha_1.jpg',       1),
(1, '/images/properties/villa_dha_2.jpg',       0),
(2, '/images/properties/studio_gulberg_1.jpg',  1),
(3, '/images/properties/plaza_f7_1.jpg',        1),
(4, '/images/properties/house_bahria_1.jpg',    1),
(5, '/images/properties/land_multan_1.jpg',     1),
(6, '/images/properties/condo_clifton_1.jpg',   1),
(7, '/images/properties/penthouse_e11_1.jpg',   1);

INSERT INTO inquiries (property_id, user_id, subject, message, status, notes) VALUES
(1, 2, 'Interested in DHA Villa', 'I am interested in the villa. Can we arrange a visit this weekend?', 'PENDING',   NULL),
(2, 3, 'Studio Apartment Query',  'Is the rent negotiable? Are pets allowed?', 'CONTACTED', 'Called on 2024-01-10, second visit scheduled.'),
(3, 4, 'Commercial Plaza Inquiry','What is the occupancy status of the shops? Any tenants currently?', 'PENDING',   NULL),
(6, 2, 'Condo Sea View Query',    'Does the condo come with parking? What floor is it on?', 'CLOSED', 'Client decided not to proceed.'),
(7, 3, 'Penthouse Islamabad',     'We are a family of 6. Is the layout suitable? Can you share floor plan?', 'PENDING', NULL);

INSERT INTO search_history (user_id, keyword, city, min_price, max_price, property_type) VALUES
(2, 'villa',      'Lahore',    20000000, 30000000, 'VILLA'),
(2, 'apartment',  'Karachi',    5000000, 15000000, 'APARTMENT'),
(3, 'condo',      'Karachi',   15000000, 30000000, 'CONDO'),
(3, 'house',      'Lahore',    10000000, 35000000, 'HOUSE'),
(4, 'commercial', 'Islamabad', 30000000, 60000000, 'COMMERCIAL'),
(2, 'land',       'Multan',     4000000,  8000000, 'LAND');

INSERT INTO logs (user_id, action, entity_type, entity_id, details) VALUES
(1, 'USER_LOGIN',      'USER',     1, 'Admin logged in'),
(1, 'PROPERTY_CREATE', 'PROPERTY', 1, 'Created: Luxury Villa in DHA Phase 5'),
(1, 'PROPERTY_CREATE', 'PROPERTY', 2, 'Created: Studio Apartment in Gulberg III'),
(2, 'USER_LOGIN',      'USER',     2, 'John logged in'),
(2, 'INQUIRY_CREATE',  'INQUIRY',  1, 'Inquiry on Property #1'),
(1, 'STATUS_CHANGE',   'PROPERTY', 4, 'Status changed: AVAILABLE → RESERVED');

INSERT INTO notifications (user_id, title, message, type, is_read) VALUES
(1, 'New Inquiry Received',   'John Doe sent an inquiry on "Luxury Villa in DHA Phase 5".',   'INFO',    0),
(1, 'New Inquiry Received',   'Sarah Smith sent an inquiry on "Studio Apartment in Gulberg".', 'INFO',    0),
(2, 'Inquiry Status Updated', 'Your inquiry on "Studio Apartment" has been updated.',          'SUCCESS', 0),
(1, 'Property Sold',          'Townhouse in DHA Phase 6 has been marked as SOLD.',             'SUCCESS', 1);
