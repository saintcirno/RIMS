@echo off
title RIMS - Rental Item Management System
cd /d "%~dp0"
echo ========================================================
echo  Starting RIMS - Rental Item Management System
echo  Access at: http://localhost:8080
echo ========================================================
java -jar target/rental-item-management-1.0.0.jar
pause
