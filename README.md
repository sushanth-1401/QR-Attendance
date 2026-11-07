# Secure QR Code-Based Smart Attendance Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Security-Spring%20Security%206%20%2B%20ECDSA-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)

A comprehensive, production-grade academic and portfolio web application implementing **Dynamic QR Generation**, **GPS Location Verification (Haversine Geofencing)**, and **Cryptographic Device Authentication (Web Crypto ECDSA Challenge-Response)**.

---

## Key Features

1. **Dynamic Ephemeral QR Codes**:
   - 256-bit entropy cryptographic tokens generated using Java `SecureRandom`.
   - Regenerates every 25–30 seconds via ZXing.
   - SHA-256 hash validation on the backend; no sensitive credentials encoded in the QR.

2. **Two-Stage Verification Reservation**:
   - QR validity: 30 seconds.
   - Verification reservation: 120 seconds.
   - Decouples QR scanning from GPS acquisition and device cryptographic signing, preventing network race conditions.

3. **GPS Geofence Verification**:
   - HTML5 Geolocation API with high accuracy.
   - Haversine great-circle distance algorithm implemented in Java.
   - Rejects stale coordinates (> 60s) or poor GPS accuracy (> 100m).
   - Configurable classroom geofence radius.

4. **Public-Key Device Authentication**:
   - Uses browser-native Web Crypto API to generate non-extractable **ECDSA (P-256)** key pairs stored in IndexedDB.
   - Zero private keys transmitted or stored on the server.
   - Single-use random challenge signed on the client and verified by Java Cryptography Architecture.

5. **Multi-Role Portal**:
   - **Administrator**: Full CRUD for students, faculty, departments, courses, classroom geofences, audit logs, and analytics.
   - **Faculty**: Session scheduling, live auto-refreshing QR display screen, real-time student check-in roster, manual audit overrides, and Excel/PDF reporting.
   - **Student**: Profile dashboard, camera QR scanner, attendance percentage metrics, and subject-wise breakdown.

---

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.2.5, Spring Security 6, Spring Data JPA, Hibernate, Flyway.
- **Frontend**: HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3, Thymeleaf, Chart.js, Font Awesome.
- **Database**: MySQL 8.0+ with transactions and foreign key constraints.
- **Cryptography & QR**: ZXing 3.5.3, Web Crypto API, Java Cryptography Extension (JCE).
- **Reporting**: Apache POI (Excel `.xlsx`), iText 7 (PDF).
- **Testing**: JUnit 5, Mockito, Spring Boot Test.

---

## Database Setup

1. Create a MySQL database:
   ```sql
   CREATE DATABASE qr_attendance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Configure credentials in `src/main/resources/application.properties` or set environment variables:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/qr_attendance?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=root
   ```
3. Flyway will automatically execute migrations (`V1__create_schema.sql` and `V2__sample_data.sql`) upon startup.
   *(Alternatively, run `schema.sql` and `sample_data.sql` directly in MySQL Workbench).*

---

## Default Login Credentials

| Role | Username | Password | Notes |
|---|---|---|---|
| **Admin** | `admin` | `admin123` | Complete administration & geofence setup |
| **Faculty** | `prof.sharma` | `admin123` | CS Dept Faculty (DBMS, Security) |
| **Faculty** | `prof.patel` | `admin123` | CS Dept Faculty (OS, Networks) |
| **Student** | `rahul.kumar` | `admin123` | CSE-A Section (Register: 21CS101) |
| **Student** | `priya.singh` | `admin123` | CSE-A Section (Register: 21CS102) |

---

## How to Build and Run

### 1. Build using Maven:
```bash
mvn clean package -DskipTests
```

### 2. Run the application:
```bash
java -jar target/qr-attendance-1.0.0.jar
```
Or directly with Maven:
```bash
mvn spring-boot:run
```

### 3. Open in Browser:
Navigate to: [http://localhost:8080](http://localhost:8080)

---

## Running Unit and Integration Tests

Run the test suite using Maven:
```bash
mvn test
```
The test suite covers:
- Haversine distance calculations (`DistanceCalculatorTest`)
- 256-bit token entropy & hashing (`QRTokenGeneratorTest`)
- GPS within/outside geofence & accuracy checks (`LocationServiceTest`)
- Dynamic QR generation & expiry enforcement (`QRCodeServiceTest`)
- Device challenge-response & replay detection (`DeviceServiceTest`)
- Two-step scan reservation & duplicate attendance rejection (`AttendanceServiceTest`)

---

## Postman Collection

Import `docs/postman_collection.json` into Postman to test all REST endpoints:
- Authentication (`/api/auth/login`, `/api/auth/me`)
- Faculty session management & live QR generation (`/api/faculty/sessions/**`)
- Student scan, challenge, and verification (`/api/student/attendance/**`, `/api/student/devices/**`)
- Excel & PDF exports (`/api/reports/export/**`)

---

## Project Structure

```
QR Attendance/
├── pom.xml                               # Maven project configuration
├── schema.sql                            # Standalone DDL schema
├── sample_data.sql                       # Standalone seed data
├── README.md                             # Documentation & user guide
├── docs/
│   ├── architecture.md                   # System architecture & sequence diagrams
│   ├── database_er_diagram.md            # Database ER diagram & constraints
│   ├── postman_collection.json           # API testing collection
│   └── project_report.md                 # Academic CSE project report
└── src/
    ├── main/
    │   ├── java/com/attendance/
    │   │   ├── AttendanceApplication.java
    │   │   ├── config/                   # Security, Web, and App configs
    │   │   ├── controller/               # REST & MVC web controllers
    │   │   ├── dto/                      # Data Transfer Objects
    │   │   ├── entity/                   # JPA Entity classes
    │   │   ├── exception/                # Global exception handlers
    │   │   ├── repository/               # Spring Data JPA repositories
    │   │   ├── security/                 # Custom UserDetailsService & Principal
    │   │   ├── service/                  # Core Business & Validation Engine
    │   │   └── util/                     # Haversine calculator & token generator
    │   └── resources/
    │       ├── application.properties
    │       ├── db/migration/             # Flyway migration scripts (V1, V2)
    │       ├── static/                   # CSS, JS (Scanner, GPS, Web Crypto)
    │       └── templates/                # Responsive Thymeleaf templates
    └── test/                             # JUnit 5 & Mockito test suites
```

