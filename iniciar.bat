@echo off
setlocal
title Taquilla de Cine - Inicio
cd /d "%~dp0"

echo ======================================
echo       TAQUILLA DE CINE
echo ======================================

rem Verificar archivos desde la ubicación del batch (taquilla-cine-api\)
if not exist "mvnw.cmd" goto error
if not exist "..\taquilla-cine-web\package.json" goto error
if not exist "application-local.properties" goto error

echo Configurando Node.js...
call nvm use 24.15.0
if errorlevel 1 goto error

echo Iniciando Spring Boot con Neon (perfil local + application-local.properties)...
start "Taquilla - Backend" /D "%~dp0" cmd /k mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./application-local.properties"

echo Iniciando Angular...
start "Taquilla - Frontend" /D "%~dp0..\taquilla-cine-web" cmd /k npm start

echo Esperando que los servicios esten disponibles...

powershell -NoProfile -Command "for ($i=0; $i -lt 60; $i++) { try { $b=Invoke-RestMethod 'http://localhost:8080/actuator/health' -TimeoutSec 3; $f=Invoke-WebRequest 'http://localhost:4200' -UseBasicParsing -TimeoutSec 3; if ($b.status -eq 'UP' -and $f.StatusCode -eq 200) { exit 0 } } catch {} ; Start-Sleep -Seconds 2 }; exit 1"

if errorlevel 1 goto error

echo Sistema iniciado correctamente.
start "" "http://localhost:4200"
exit /b 0

:error
echo.
echo No se pudo iniciar el sistema correctamente.
echo Revisa las rutas y las terminales del backend y frontend.
pause
exit /b 1