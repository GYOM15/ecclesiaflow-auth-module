#!/bin/bash

# =============================================================================
# SCRIPT DE DÉMARRAGE ECCLESIAFLOW AUTHENTICATION MODULE
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction d'affichage
print_header() {
    echo -e "${BLUE}"
    echo "=============================================="
    echo "  EcclesiaFlow Authentication Module"
    echo "=============================================="
    echo -e "${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Fonction d'aide
show_help() {
    echo "Usage: $0 [PROFILE] [OPTIONS]"
    echo ""
    echo "PROFILES:"
    echo "  dev     - Développement (H2, debug logs, H2 console)"
    echo "  test    - Tests (H2, logs minimaux)"
    echo "  prod    - Production (MySQL, logs optimisés)"
    echo ""
    echo "OPTIONS:"
    echo "  -h, --help     Afficher cette aide"
    echo "  -c, --clean    Nettoyer avant compilation"
    echo "  -s, --skip     Ignorer les tests"
    echo ""
    echo "EXEMPLES:"
    echo "  $0 dev         # Démarrer en mode développement"
    echo "  $0 prod -c     # Démarrer en production avec nettoyage"
    echo "  $0 test -s     # Démarrer en test sans les tests"
}

# Vérifier les prérequis
check_prerequisites() {
    echo "Vérification des prérequis..."
    
    # Java
    if ! command -v java &> /dev/null; then
        print_error "Java n'est pas installé"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_error "Java 21 ou supérieur requis (version actuelle: $JAVA_VERSION)"
        exit 1
    fi
    print_success "Java $JAVA_VERSION détecté"
    
    # Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven n'est pas installé"
        exit 1
    fi
    print_success "Maven détecté"
    
    # Fichier .env pour production
    if [ "$PROFILE" = "prod" ] && [ ! -f ".env" ]; then
        print_warning "Fichier .env manquant pour la production"
        echo "Copiez .env.example vers .env et configurez les variables"
        read -p "Continuer quand même ? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# Charger les variables d'environnement
load_env() {
    if [ -f ".env" ]; then
        print_success "Chargement des variables d'environnement depuis .env"
        export $(cat .env | grep -v '^#' | xargs)
    fi
}

# Compilation
compile_project() {
    echo "Compilation du projet..."
    
    MAVEN_OPTS=""
    if [ "$CLEAN" = true ]; then
        MAVEN_OPTS="clean"
        print_success "Nettoyage activé"
    fi
    
    if [ "$SKIP_TESTS" = true ]; then
        MAVEN_OPTS="$MAVEN_OPTS compile -DskipTests"
        print_warning "Tests ignorés"
    else
        MAVEN_OPTS="$MAVEN_OPTS compile test"
    fi
    
    if ! mvn $MAVEN_OPTS -P$PROFILE; then
        print_error "Échec de la compilation"
        exit 1
    fi
    
    print_success "Compilation réussie"
}

# Démarrage de l'application
start_application() {
    echo "Démarrage de l'application (profil: $PROFILE)..."
    
    # Configuration spécifique par profil
    case $PROFILE in
        dev)
            print_success "Mode développement activé"
            echo "- Base de données H2 en mémoire"
            echo "- Console H2: http://localhost:8080/h2-console"
            echo "- Logs debug activés"
            ;;
        test)
            print_success "Mode test activé"
            echo "- Base de données H2 pour tests"
            echo "- Logs minimaux"
            ;;
        prod)
            print_success "Mode production activé"
            echo "- Base de données MySQL"
            echo "- Logs optimisés"
            echo "- Monitoring: http://localhost:8081/actuator"
            ;;
    esac
    
    echo ""
    echo "🚀 Démarrage de l'application..."
    echo "📖 Documentation API: http://localhost:8080/swagger-ui.html"
    echo "🔍 Health check: http://localhost:8080/actuator/health"
    echo ""
    
    # Démarrage avec le profil spécifié
    mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE -P$PROFILE
}

# =============================================================================
# SCRIPT PRINCIPAL
# =============================================================================

print_header

# Valeurs par défaut
PROFILE="dev"
CLEAN=false
SKIP_TESTS=false

# Parsing des arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        dev|test|prod)
            PROFILE="$1"
            shift
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -s|--skip)
            SKIP_TESTS=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "Option inconnue: $1"
            show_help
            exit 1
            ;;
    esac
done

echo "Configuration:"
echo "- Profil: $PROFILE"
echo "- Nettoyage: $CLEAN"
echo "- Ignorer tests: $SKIP_TESTS"
echo ""

# Exécution
check_prerequisites
load_env
compile_project
start_application
