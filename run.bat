@echo off
set "JAVA_HOME=%~dp0tools\jdk-21.0.3+9"
set "PATH=%JAVA_HOME%\bin;%~dp0tools\apache-maven-3.9.6\bin;%PATH%"
echo ===============================================================
echo Starting Secure QR Attendance System (Spring Boot - Dev Mode)
echo Access at: http://localhost:8080
echo ===============================================================
echo Java Home: %JAVA_HOME%
if exist "%~dp0target\qr-attendance-1.0.0.jar" (
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0target\qr-attendance-1.0.0.jar" --spring.profiles.active=dev
) else (
    mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
)
