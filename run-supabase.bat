@echo off
set "JAVA_HOME=%~dp0tools\jdk-21.0.3+9"
set "PATH=%JAVA_HOME%\bin;%~dp0tools\apache-maven-3.9.6\bin;%PATH%"
echo ===============================================================
echo Starting SCSVMV Portal with Direct Supabase PostgreSQL
echo Host: db.rsxttzenmxkuwrfrbkbv.supabase.co:5432
echo ===============================================================
if "%SPRING_DATASOURCE_PASSWORD%"=="" (
    set /p SPRING_DATASOURCE_PASSWORD="Enter your Supabase Database Password:"
)
"%JAVA_HOME%\bin\java.exe" -jar "%~dp0target\qr-attendance-1.0.0.jar" --spring.profiles.active=supabase

