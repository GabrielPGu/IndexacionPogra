@echo off
title ClusterComputing_1 en Red
echo Conectando a la ruta de red y compilando...

:: Cambia automáticamente a la ruta de red (funciona con carpetas compartidas)
pushd "\\192.168.124.60\data"

if %errorlevel% neq 0 (
    echo [ERROR] No se pudo acceder a la ruta de red. Verifica tu conexion.
    pause
    exit /b
)

echo Compilando el archivo Java...
javac ClusterComputing_1.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Hubo un problema al compilar el codigo.
    popd
    pause
    exit /b
)

echo.
echo Ejecutando el programa...
echo ----------------------------------------
java ClusterComputing_1
echo ----------------------------------------

:: Regresa a la ruta original de donde se ejecuto el bat
popd

echo.
echo Programa finalizado.
pause