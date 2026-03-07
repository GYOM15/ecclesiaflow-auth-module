# EcclesiaFlow Auth Module — Guide de Deploiement Production (Hostinger VPS)

> **Date**: 2026-03-01
> **VPS recommande**: Hostinger KVM 2 (8 GB RAM, 2 vCPU, ~12 EUR/mois)
> **OS**: Ubuntu 24.04 LTS

---

## Table des matieres

1. [Generer la cle SSH](#1-generer-la-cle-ssh)
2. [Configurer le VPS Hostinger](#2-configurer-le-vps-hostinger)
3. [Configurer GitHub](#3-configurer-github)
4. [Generer les secrets de production](#4-generer-les-secrets-de-production)
5. [Creer le fichier .env.prod sur le VPS](#5-creer-le-fichier-envprod-sur-le-vps)
6. [Premier deploiement](#6-premier-deploiement)
7. [Certificat TLS (Let's Encrypt)](#7-certificat-tls-lets-encrypt)
8. [Verification post-deploiement](#8-verification-post-deploiement)
9. [Commandes utiles (Makefile)](#9-commandes-utiles-makefile)
10. [DNS — Pointer le domaine vers le VPS](#10-dns--pointer-le-domaine-vers-le-vps)

---

## 1. Generer la cle SSH

Sur ton Mac :

```bash
# Generer une paire de cles dediee au deploiement (PAS de passphrase — appuie Entree)
ssh-keygen -t ed25519 -C "ton-email@gmail.com" -f ~/.ssh/ecclesiaflow_deploy

# Resultat :
#   ~/.ssh/ecclesiaflow_deploy       -> cle PRIVEE (pour GitHub Secrets)
#   ~/.ssh/ecclesiaflow_deploy.pub   -> cle PUBLIQUE (pour le VPS Hostinger)
```

---

## 2. Configurer le VPS Hostinger

### 2a. Ajouter la cle publique SSH

**Option A — Via le panel Hostinger :**

1. Hostinger Panel > VPS > ton VPS > Settings > SSH Keys
2. Coller le contenu de :
   ```bash
   cat ~/.ssh/ecclesiaflow_deploy.pub
   ```

**Option B — Manuellement via SSH (si acces root existant) :**

```bash
ssh root@<IP-VPS>
mkdir -p ~/.ssh
echo "<contenu de ecclesiaflow_deploy.pub>" >> ~/.ssh/authorized_keys
```

### 2b. Lancer le script de setup initial

```bash
# Depuis ton Mac — initialise Docker, firewall, fail2ban, user deploy
ssh root@<IP-VPS> 'bash -s' < deploy/setup-vps.sh
```

Ce script :
- Installe Docker et Docker Compose
- Cree un user `deploy` avec acces Docker
- Configure le firewall UFW (ports 22, 80, 443)
- Installe fail2ban (protection brute-force)
- Durcit SSH (desactive root login + password auth)

### 2c. Copier la cle publique pour le user deploy

```bash
ssh root@<IP-VPS> "mkdir -p /home/deploy/.ssh && \
  cp ~/.ssh/authorized_keys /home/deploy/.ssh/ && \
  chown -R deploy:deploy /home/deploy/.ssh"
```

### 2d. Tester la connexion

```bash
ssh -i ~/.ssh/ecclesiaflow_deploy deploy@<IP-VPS>
```

> **Note** : `<IP-VPS>` = l'adresse IP de ton VPS (visible dans Hostinger Panel > VPS).
> Ce n'est PAS ton nom de domaine.

---

## 3. Configurer GitHub

### 3a. Creer l'environment "production"

GitHub repo > **Settings > Environments > New environment** > `production`

### 3b. Ajouter les Secrets

GitHub repo > **Settings > Secrets and variables > Actions > New repository secret** :

| Secret         | Valeur                                                          |
|----------------|-----------------------------------------------------------------|
| `VPS_HOST`     | L'IP de ton VPS Hostinger                                       |
| `VPS_USER`     | `deploy`                                                        |
| `VPS_SSH_KEY`  | Le contenu ENTIER de `cat ~/.ssh/ecclesiaflow_deploy` (PRIVEE)  |
| `VPS_SSH_PORT` | `22` (ou le port personnalise si modifie)                       |

---

## 4. Generer les secrets de production

```bash
# Executer pour chaque secret a generer :
openssl rand -hex 32
```

| Variable                             | Comment la generer                                              |
|--------------------------------------|-----------------------------------------------------------------|
| `MYSQL_PASSWORD`                     | `openssl rand -hex 32`                                          |
| `MYSQL_ROOT_PASSWORD`                | `openssl rand -hex 32`                                          |
| `KEYCLOAK_ADMIN_PASSWORD`            | `openssl rand -hex 32`                                          |
| `KEYCLOAK_DB_PASSWORD`               | `openssl rand -hex 32`                                          |
| `KEYCLOAK_BACKEND_CLIENT_SECRET`     | `openssl rand -hex 32`                                          |
| `KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET` | `openssl rand -hex 32`                                        |
| `KEYCLOAK_SMTP_PASSWORD`            | App password Gmail (Google > Securite > Mots de passe d'app)    |
| `NGINX_HOST` / `KC_HOSTNAME`         | Ton domaine (ex: `auth.gyom-tech.com`)                         |
| `CORS_ALLOWED_ORIGINS`               | L'URL de ton frontend (ex: `https://app.gyom-tech.com`)        |

---

## 5. Creer le fichier .env.prod sur le VPS

```bash
# Se connecter au VPS
ssh -i ~/.ssh/ecclesiaflow_deploy deploy@<IP-VPS>

# Creer le repertoire
mkdir -p /opt/ecclesiaflow/docker

# Creer le fichier (copier le contenu de .env.prod.example et remplir les secrets)
nano /opt/ecclesiaflow/docker/.env.prod
```

> **IMPORTANT** : Ce fichier reste UNIQUEMENT sur le VPS, jamais dans git.

---

## 6. Premier deploiement

### Option A — Deploiement automatique via GitHub Actions (CD)

```bash
# Merger feature/deployment dans main et push
git checkout main
git merge feature/deployment
git push origin main
# -> GitHub Actions declenche automatiquement le pipeline CD
```

### Option B — Deploiement manuel

```bash
make deploy VPS=deploy@<IP-VPS>
```

---

## 7. Certificat TLS (Let's Encrypt)

Apres le premier deploiement (le stack tourne avec un certificat self-signed) :

```bash
# Depuis le VPS
cd /opt/ecclesiaflow
make tls-init DOMAIN=auth.gyom-tech.com
```

Le renouvellement automatique est gere par le container certbot.
Pour forcer un renouvellement manuel :

```bash
make tls-renew
```

---

## 8. Verification post-deploiement

```bash
# Status de tous les containers
make prod-status

# Tester le health check
docker exec ecclesiaflow-auth-module wget -qO- http://localhost:8081/actuator/health

# Verifier les logs
make prod-logs

# Tester depuis l'exterieur (apres TLS)
curl -s https://auth.gyom-tech.com/actuator/health | jq
```

### Checklist post-deploiement

```
[ ] Tous les containers sont "healthy"
[ ] Le health check retourne {"status":"UP"}
[ ] Le certificat TLS est valide (pas de warning navigateur)
[ ] Les endpoints API repondent correctement
[ ] Les endpoints actuator sensibles sont proteges (metrics retourne 401/403)
[ ] Les logs ne contiennent pas d'erreurs
[ ] Keycloak est accessible et fonctionnel
```

---

## 9. Commandes utiles (Makefile)

| Commande           | Description                              |
|--------------------|------------------------------------------|
| `make prod-build`  | Build l'image Docker auth-module         |
| `make prod-up`     | Demarrer tout le stack production        |
| `make prod-down`   | Arreter tout le stack                    |
| `make prod-logs`   | Suivre les logs auth-module              |
| `make prod-status` | Status de tous les services              |
| `make prod-restart`| Rebuild + restart auth-module uniquement |
| `make tls-init DOMAIN=...` | Obtenir le certificat TLS initial |
| `make tls-renew`   | Forcer le renouvellement TLS             |
| `make deploy VPS=user@host` | Deploiement manuel via SSH       |

---

## 10. DNS — Pointer le domaine vers le VPS

Chez ton registraire de domaine (ou Hostinger si le domaine est chez eux) :

```
Type : A
Name : auth        (ou le sous-domaine souhaite)
Value: <IP-VPS>    (l'IP de ton VPS Hostinger)
TTL  : 3600
```

La propagation DNS peut prendre jusqu'a 24h (generalement 15-30 min).

Pour verifier :

```bash
dig auth.gyom-tech.com +short
# Doit retourner l'IP de ton VPS
```

---

## 11. Notes de configuration par environnement

### Swagger / OpenAPI (springdoc)

En dev, Swagger UI est accessible sans authentification via les paths whitelistes dans `SecurityConfiguration` :
- `/swagger-ui/**`
- `/v3/api-docs/**`

**En production**, desactiver Swagger UI completement en ajoutant dans `.env.prod` :

```
SPRINGDOC_SWAGGER_UI_ENABLED=false
SPRINGDOC_API_DOCS_ENABLED=false
```

Et dans `application.properties` (ou `application-prod.properties`) :

```properties
springdoc.swagger-ui.enabled=${SPRINGDOC_SWAGGER_UI_ENABLED:true}
springdoc.api-docs.enabled=${SPRINGDOC_API_DOCS_ENABLED:true}
```

> **Pourquoi ?** Les endpoints Swagger sont en `permitAll()` dans la config securite.
> En prod, il vaut mieux les desactiver completement plutot que de les proteger,
> car ils exposent la structure complete de l'API.

---

## Resume du flux

```
Mac (toi)                    GitHub                     Hostinger VPS
---------                    ------                     -------------
git push main ----------> Actions CI/CD -----------> docker pull + deploy
                          (build, test,              (compose up)
                           push image GHCR)
                                |
                          SSH via cle privee -------> user: deploy
                          (VPS_SSH_KEY secret)        /opt/ecclesiaflow/
                                                      +-- docker/.env.prod
                                                      +-- docker/docker-compose.prod.yml
```
