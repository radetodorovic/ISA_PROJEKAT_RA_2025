@echo off
echo ========================================
echo  GENERISANJE PROTOBUF JAVA KLASA
echo ========================================
echo.

REM Pronađi Maven instalaciju (možda je u IntelliJ IDEA bundled Maven)
SET MAVEN_CMD=

REM Pokušaj 1: Maven u PATH
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    SET MAVEN_CMD=mvn
    echo [OK] Pronađen Maven u PATH
    goto :build
)

REM Pokušaj 2: IntelliJ bundled Maven (tipična lokacija)
SET IDEA_MAVEN=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3\plugins\maven\lib\maven3\bin\mvn.cmd
if exist "%IDEA_MAVEN%" (
    SET MAVEN_CMD=%IDEA_MAVEN%
    echo [OK] Pronađen IntelliJ bundled Maven
    goto :build
)

REM Pokušaj 3: IntelliJ Community bundled Maven
SET IDEA_MAVEN_CE=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3\plugins\maven\lib\maven3\bin\mvn.cmd
if exist "%IDEA_MAVEN_CE%" (
    SET MAVEN_CMD=%IDEA_MAVEN_CE%
    echo [OK] Pronađen IntelliJ Community bundled Maven
    goto :build
)

echo [ERROR] Maven nije pronađen!
echo.
echo Molim te instaliraj Maven ili koristi IntelliJ IDEA za build:
echo   1. Otvori projekt u IntelliJ IDEA
echo   2. Desni klik na pom.xml -^> Maven -^> Generate Sources and Update Folders
echo.
pause
exit /b 1

:build
echo.
echo Pokretanje: %MAVEN_CMD% clean compile -DskipTests
echo.
"%MAVEN_CMD%" clean compile -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo  BUILD USPEŠAN!
    echo ========================================
    echo.
    echo Protobuf klase su generisane u:
    echo target\generated-sources\protobuf\java\com\isa\backend\proto\
    echo.
) else (
    echo.
    echo ========================================
    echo  BUILD NEUSPEŠAN!
    echo ========================================
    echo.
)

pause

