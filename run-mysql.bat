@echo off
set "JAVA_HOME=%~dp0tools\jdk-21.0.3+9"
set "PATH=%JAVA_HOME%\bin;%~dp0tools\apache-maven-3.9.6\bin;%PATH%"
echo ===============================================================
echo Starting Secure QR Attendance System (Spring Boot - MySQL Mode)
echo ===============================================================
echo Ensure MySQL 8 is running on localhost:3306 with schema 'qr_attendance'
echo Java Home: %JAVA_HOME%
mvn spring-boot:run

