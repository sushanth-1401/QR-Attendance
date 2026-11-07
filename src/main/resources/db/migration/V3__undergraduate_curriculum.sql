-- ============================================
-- SCSVMV University - Undergraduate Curriculum & Syllabus
-- Comprehensive B.Tech / BE Academic Course Offerings
-- ============================================

-- 1. Additional Academic Sections for Engineering Departments
INSERT INTO class_sections (department_id, academic_year, year_of_study, section_name, is_active) VALUES
(4, '2026-2027', 3, 'EEE-A', TRUE),
(5, '2026-2027', 3, 'MECH-A', TRUE),
(6, '2026-2027', 3, 'CIVIL-A', TRUE);

-- 2. Computer Science & Engineering (CSE) - B.Tech Undergraduate Syllabus
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('CS101', 'Programming in C & Problem Solving', 1, 3, TRUE),
('CS201', 'Data Structures & Algorithms', 1, 4, TRUE),
('CS202', 'Object Oriented Programming with Java', 1, 4, TRUE),
('CS203', 'Discrete Mathematical Structures', 1, 3, TRUE),
('CS204', 'Digital Logic & Computer Organization', 1, 3, TRUE),
('CS303', 'Computer Networks & Internet Protocols', 1, 4, TRUE),
('CS304', 'Design and Analysis of Algorithms', 1, 4, TRUE),
('CS305', 'Theory of Computation & Automata', 1, 3, TRUE),
('CS401', 'Artificial Intelligence & Machine Learning', 1, 4, TRUE),
('CS402', 'Compiler Design & Construction', 1, 3, TRUE),
('CS403', 'Cryptography & Cyber Security', 1, 3, TRUE),
('CS404', 'Cloud Computing & DevOps Architecture', 1, 3, TRUE);

-- 3. Information Technology (IT) - B.Tech Undergraduate Syllabus
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('IT201', 'Python Programming & Rapid Development', 2, 3, TRUE),
('IT202', 'Data Structures using C++', 2, 4, TRUE),
('IT302', 'Software Engineering & Agile Methodologies', 2, 3, TRUE),
('IT303', 'Mobile Application Development (Android/iOS)', 2, 3, TRUE),
('IT401', 'Big Data Technologies & Analytics', 2, 3, TRUE),
('IT402', 'Internet of Things & Embedded Sensors', 2, 3, TRUE);

-- 4. Electronics & Communication Engineering (ECE) - B.Tech Undergraduate Syllabus
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('EC201', 'Semiconductor Devices & Analog Circuits', 3, 4, TRUE),
('EC202', 'Signals & Systems Analysis', 3, 3, TRUE),
('EC301', 'Digital Signal Processing', 3, 4, TRUE),
('EC302', 'Microprocessors & Microcontrollers (ARM/8051)', 3, 4, TRUE),
('EC303', 'VLSI Design & Hardware Description', 3, 3, TRUE),
('EC401', 'Wireless & Mobile Cellular Communications', 3, 3, TRUE);

-- 5. Electrical & Electronics Engineering (EEE) - B.Tech Undergraduate Syllabus
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('EE201', 'Electric Circuit Theory & Network Analysis', 4, 4, TRUE),
('EE202', 'Electrical Machines - DC & AC', 4, 4, TRUE),
('EE301', 'Power Systems Analysis & Protection', 4, 4, TRUE),
('EE302', 'Control Systems Engineering', 4, 3, TRUE),
('EE401', 'Power Electronics & Industrial Motor Drives', 4, 3, TRUE);

-- 6. Mechanical Engineering (MECH) - B.Tech Undergraduate Syllabus
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('ME201', 'Engineering Mechanics & Strength of Materials', 5, 4, TRUE),
('ME202', 'Applied Engineering Thermodynamics', 5, 4, TRUE),
('ME301', 'Fluid Mechanics & Hydraulic Machinery', 5, 4, TRUE),
('ME302', 'Manufacturing Technology & CNC Machining', 5, 3, TRUE),
('ME401', 'Automobile Engineering & Electric Vehicles', 5, 3, TRUE);

-- 7. Civil Engineering (CIVIL) - B.Tech Undergraduate Syllabus
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('CE201', 'Surveying & Advanced Geomatics', 6, 3, TRUE),
('CE202', 'Structural Analysis & Reinforced Concrete', 6, 4, TRUE),
('CE301', 'Fluid Mechanics & Water Resource Engineering', 6, 4, TRUE),
('CE401', 'Environmental Engineering & Waste Management', 6, 3, TRUE);

-- 8. Mathematics & Foundational Sciences
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('MA101', 'Linear Algebra & Differential Calculus', 7, 4, TRUE),
('MA201', 'Probability, Statistics & Numerical Methods', 7, 4, TRUE);

-- 9. Department of Management Studies (MBA)
INSERT INTO subjects (subject_code, subject_name, department_id, credits, is_active) VALUES
('MB101', 'Principles of Management & Organizational Behaviour', 8, 3, TRUE),
('MB201', 'Financial Accounting & Managerial Economics', 8, 3, TRUE);

