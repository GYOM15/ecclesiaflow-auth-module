@echo off
REM =============================================================================
REM SCRIPT DE DÉMARRAGE ECCLESIAFLOW AUTHENTICATION MODULE (Windows)
REM =============================================================================

setlocal enabledelayedexpansion

REM Couleurs pour Windows (limitées)
set "GREEN=[92m"
set "YELLOW=[93m"
set "RED=[91m"
set "BLUE=[94m"
set "NC=[0m"

REM Valeurs par défaut
set "PROFILE=dev"
set "CLEAN=false"
set "SKIP_TESTS=false"

echo.
echo %BLUE%==============================================
echo   EcclesiaFlow Authentication Module
echo ==============================================%NC%
echo.

REM Parsing des arguments
:parse_args
if "%~1"=="" goto :check_prerequisites
if "%~1"=="dev" set "PROFILE=dev" & shift & goto :parse_args
if "%~1"=="test" set "PROFILE=test" & shift & goto :parse_args
if "%~1"=="prod" set "PROFILE=prod" & shift & goto :parse_args
if "%~1"=="-c" set "CLEAN=true" & shift & goto :parse_args
if "%~1"=="--clean" set "CLEAN=true" & shift & goto :parse_args
if "%~1"=="-s" set "SKIP_TESTS=true" & shift & goto :parse_args
if "%~1"=="--skip" set "SKIP_TESTS=true" & shift & goto :parse_args
if "%~1"=="-h" goto :show_help
if "%~1"=="--help" goto :show_help

echo %RED%Option inconnue: %~1%NC%
goto :show_help

:show_help
echo Usage: %0 [PROFILE] [OPTIONS]
echo.
echo PROFILES:
echo   dev     - Développement (H2, debug logs, H2 console)
echo   test    - Tests (H2, logs minimaux)
echo   prod    - Production (MySQL, logs optimisés)
echo.
echo OPTIONS:
echo   -h, --help     Afficher cette aide
echo   -c, --clean    Nettoyer avant compilation
echo   -s, --skip     Ignorer les tests
echo.
echo EXEMPLES:
echo   %0 dev         # Démarrer en mode développement
echo   %0 prod -c     # Démarrer en production avec nettoyage
echo   %0 test -s     # Démarrer en test sans les tests
goto :eof

:check_prerequisites
echo Vérification des prérequis...

REM Vérifier Java
java -version >nul 2>&1
if errorlevel 1 (
    echo %RED%Java n'est pas installé%NC%
    exit /b 1
)
echo %GREEN%Java détecté%NC%

REM Vérifier Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo %RED%Maven n'est pas installé%NC%
    exit /b 1
)
echo %GREEN%Maven détecté%NC%

REM Vérifier .env pour production
if "%PROFILE%"=="prod" (
    if not exist ".env" (
        echo %YELLOW%Fichier .env manquant pour la production%NC%
        echo Copiez .env.example vers .env et configurez les variables
        set /p "continue=Continuer quand même ? (y/N): "
        if /i not "!continue!"=="y" exit /b 1
    )
)

:load_env
if exist ".env" (
    echo %GREEN%Chargement des variables d'environnement depuis .env%NC%
    REM Note: Le chargement automatique des variables .env est complexe en batch
    REM Il est recommandé de les définir manuellement ou d'utiliser un outil tiers
)

:compile_project
echo Compilation du projet...

set "MAVEN_OPTS="
if "%CLEAN%"=="true" (
    set "MAVEN_OPTS=clean"
    echo %GREEN%Nettoyage activé%NC%
)

if "%SKIP_TESTS%"=="true" (
    set "MAVEN_OPTS=%MAVEN_OPTS% compile -DskipTests"
    echo %YELLOW%Tests ignorés%NC%
) else (
    set "MAVEN_OPTS=%MAVEN_OPTS% compile test"
)

echo Exécution: mvn %MAVEN_OPTS% -P%PROFILE%
call mvn %MAVEN_OPTS% -P%PROFILE%
if errorlevel 1 (
    echo %RED%Échec de la compilation%NC%
    exit /b 1
)

echo %GREEN%Compilation réussie%NC%

:start_application
echo Démarrage de l'application (profil: %PROFILE%)...

REM Configuration spécifique par profil
if "%PROFILE%"=="dev" (
    echo %GREEN%Mode développement activé%NC%
    echo - Base de données H2 en mémoire
    echo - Console H2: http://localhost:8080/h2-console
    echo - Logs debug activés
)
if "%PROFILE%"=="test" (
    echo %GREEN%Mode test activé%NC%
    echo - Base de données H2 pour tests
    echo - Logs minimaux
)
if "%PROFILE%"=="prod" (
    echo %GREEN%Mode production activé%NC%
    echo - Base de données MySQL
    echo - Logs optimisés
    echo - Monitoring: http://localhost:8081/actuator
)

echo.
echo Configuration:
echo - Profil: %PROFILE%
echo - Nettoyage: %CLEAN%
echo - Ignorer tests: %SKIP_TESTS%
echo.
echo 🚀 Démarrage de l'application...
echo 📖 Documentation API: http://localhost:8080/swagger-ui.html
echo 🔍 Health check: http://localhost:8080/actuator/health
echo.

REM Démarrage avec le profil spécifié
call mvn spring-boot:run -Dspring-boot.run.profiles=%PROFILE% -P%PROFILE%

:eof
