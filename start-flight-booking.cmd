@echo off
REM Start the Airline Booking System in dev mode (H2 in-memory database)
cd /d "%~dp0"
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
