# Project Report: Secure QR Code-Based Smart Attendance Management System

**Academic Project Report**  
*Degree of Bachelor of Technology in Computer Science and Engineering*

---

## 1. Abstract
Manual attendance methods in higher education institutions are prone to proxy attendance, buddy punching, time wastage, and manual aggregation errors. This project presents a **Secure Smart Attendance Management System** that combines dynamic QR code generation, GPS geofencing verification, and cryptographic device authentication.

By requiring three independent validation factors before attendance is recorded—(1) physical scan of a 30-second ephemeral dynamic QR code, (2) location verification within a configurable classroom radius using the Haversine distance formula, and (3) digital signature proof from a registered device public key using the Web Crypto API—the system drastically reduces proxy attendance while maintaining sub-second verification throughput.

---

## 2. Mathematical Model: Haversine Geofencing
To verify whether a student is physically located within the classroom boundaries, the server computes the great-circle distance $d$ between the student's reported coordinates $(\phi_1, \lambda_1)$ and the classroom's coordinates $(\phi_2, \lambda_2)$ using the **Haversine formula**:

$$\Delta \phi = \phi_2 - \phi_1$$
$$\Delta \lambda = \lambda_2 - \lambda_1$$

$$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1) \cdot \cos(\phi_2) \cdot \sin^2\left(\frac{\Delta \lambda}{2}\right)$$

$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)$$

$$d = R \cdot c$$

Where:
- $R$ is the Earth's mean radius ($6,371,000\text{ meters}$).
- $\phi_1, \phi_2$ are latitudes in radians.
- $\lambda_1, \lambda_2$ are longitudes in radians.

### Verification Rule:
$$d \le r_{\text{allowed}} + 0.5 \cdot \text{accuracy}$$
Where:
- $r_{\text{allowed}}$ is the classroom geofence radius configured by the administrator (e.g., $50\text{ m}$).
- $\text{accuracy}$ is the GPS horizontal uncertainty radius in meters reported by the device. If $\text{accuracy} > 100\text{ m}$, the reading is rejected as too coarse.

---

## 3. Cryptographic Device Authentication
To prevent students from sharing account credentials or sending remote links to friends:
1. **Key Generation**: When a student logs into their smartphone, the browser's native **Web Crypto API** creates an **ECDSA (Curve P-256 / SHA-256)** asymmetric keypair.
2. **Private Key Storage**: The private key is non-extractable and persisted in **IndexedDB**.
3. **Public Key Registration**: The public key is exported in **SubjectPublicKeyInfo (SPKI)** format and stored in the database.
4. **Challenge-Response Protocol**:
   - The server issues a single-use 256-bit cryptographically secure pseudorandom number ($C$) generated via `java.security.SecureRandom`.
   - The device computes the digital signature $S = \text{Sign}_{K_{\text{private}}}(C)$.
   - Java's Cryptography Architecture (`java.security.Signature`) verifies $S$ against $K_{\text{public}}$ and immediately consumes the challenge in a transactional operation to eliminate replay attacks.

---

## 4. Reservation & Idempotency Engine
To solve network latency and transmission delays:
- **QR Token Validity**: $30\text{ seconds}$.
- **Attendance Reservation**: $120\text{ seconds}$.
When a student scans before the 30-second token expires, the server opens an `attendance_attempt` reservation tied to the authenticated user and session. The student then has 120 seconds to acquire GPS coordinates and complete device authentication.

---

## 5. Security & Threat Modeling Analysis

| Attack Vector | Countermeasure Implemented |
|---|---|
| **QR Photograph Sharing** | QR tokens dynamically regenerate every 25–30 seconds with 256-bit randomness. A photo sent over chat will expire before the recipient can scan it. |
| **GPS Spoofing / Mock Locations** | Classroom radius verification + timestamp freshness checks (< 60s) + accuracy threshold enforcement (< 100m). |
| **Credential Sharing** | Cryptographic challenge-response requires possession of the physical device holding the non-extractable private key. |
| **Replay Attacks** | Challenge tokens and scan request IDs are single-use (`consumed = true`) and protected by database unique constraints. |
| **Concurrent Double Submission** | Enforced via database constraint `UNIQUE(student_id, session_id)` on `attendance_records` wrapped in `@Transactional`. |
| **IDOR / Parameter Tampering** | Backend derives student identity strictly from the Spring Security session context (`CurrentUser.getCurrentUserId()`), never trusting client-supplied IDs. |

---

## 6. Conclusion
The system successfully bridges modern cryptographic standards and responsive web engineering. It delivers an intuitive user experience for educators and students while establishing verifiable audit trails for academic governance.

