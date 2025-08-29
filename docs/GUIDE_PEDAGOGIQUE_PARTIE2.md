# 📚 Guide Pédagogique - Partie 2

## 🌐 Couche présentation (Controllers & DTOs)

### Comprendre les contrôleurs REST

Un contrôleur est le **point d'entrée** de votre API. Il traduit les requêtes HTTP en appels métier.

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    
    @PostMapping("/token")
    public ResponseEntity<JwtAuthenticationResponse> generateToken(
        @Valid @RequestBody SigninRequest request) {
        
        // 1. Validation automatique (@Valid)
        // 2. Conversion DTO → Domain
        SigninCredentials credentials = MemberMapper.fromSigninRequest(request);
        
        // 3. Appel service métier
        JwtAuthenticationResponse response = 
            authenticationService.getAuthenticatedMember(credentials);
        
        // 4. Retour HTTP 200 + JSON
        return ResponseEntity.ok(response);
    }
}
```

### Annotations expliquées

**`@RestController` vs `@Controller`**
```java
// @Controller - Retourne des vues (HTML)
@Controller
public class WebController {
    @GetMapping("/login")
    public String loginPage() {
        return "login.html"; // Nom de la vue
    }
}

// @RestController - Retourne des données (JSON)
@RestController
public class ApiController {
    @GetMapping("/api/users")
    public List<User> getUsers() {
        return users; // Sérialisé en JSON automatiquement
    }
}
```

**`@Valid` - Validation automatique**
```java
public class SigninRequest {
    @NotBlank(message = "Email obligatoire")
    @Email(message = "Format email invalide")
    private String email;
    
    @NotBlank(message = "Mot de passe obligatoire")
    @Size(min = 6, message = "Minimum 6 caractères")
    private String password;
}

// Spring valide automatiquement et retourne 400 si erreur
@PostMapping("/token")
public ResponseEntity<?> generateToken(@Valid @RequestBody SigninRequest request) {
    // Si validation échoue, méthode jamais appelée
    // Client reçoit automatiquement HTTP 400 + détails erreurs
}
```

### Pattern DTO et mapping

**Pourquoi des DTOs ?**
```
┌─────────────────────────────────────────────────────────────┐
│                    SANS DTO (Problématique)                │
├─────────────────────────────────────────────────────────────┤
│  Client ←→ Entity directement                               │
│                                                             │
│  ❌ Exposition des données internes (ID, timestamps...)    │
│  ❌ Couplage fort client/base de données                   │
│  ❌ Évolution difficile (changement schema = casse client) │
│  ❌ Sécurité (mot de passe exposé)                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    AVEC DTO (Solution)                     │
├─────────────────────────────────────────────────────────────┤
│  Client ←→ DTO ←→ Domain ←→ Entity                         │
│                                                             │
│  ✅ Contrôle total sur l'exposition des données           │
│  ✅ Validation côté API                                    │
│  ✅ Évolution indépendante                                 │
│  ✅ Sécurité renforcée                                     │
└─────────────────────────────────────────────────────────────┘
```

**Exemple concret :**
```java
// Entity (Base de données)
@Entity
public class Member {
    private Long id;
    private String email;
    private String password; // ❌ Ne JAMAIS exposer
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    // ... autres champs internes
}

// DTO Response (Client)
public class MemberResponse {
    private String email;
    private String message;
    // ✅ Seulement ce que le client doit voir
}

// Mapper
public class MemberResponseMapper {
    public static MemberResponse fromMember(Member member, String message) {
        return MemberResponse.builder()
            .email(member.getEmail())
            .message(message)
            .build(); // Pas de mot de passe, pas d'ID interne
    }
}
```

### Gestion des erreurs

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex) {
        
        // Transformer les erreurs de validation en réponse claire
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
            
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Validation failed", errors));
    }
}
```

---

## 🔒 Sécurité et JWT

### Comprendre JWT (JSON Web Token)

**Structure d'un JWT :**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

│─────── HEADER ────────│─────── PAYLOAD ──────│─────── SIGNATURE ─────│
```

**Décodage :**
```json
// HEADER (Base64 décodé)
{
  "alg": "HS256",  // Algorithme de signature
  "typ": "JWT"     // Type de token
}

// PAYLOAD (Base64 décodé)
{
  "sub": "user@email.com",  // Subject (utilisateur)
  "iat": 1516239022,        // Issued At (timestamp)
  "exp": 1516325422         // Expiration (timestamp)
}

// SIGNATURE
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Implémentation JWT Service

```java
@Service
public class JWTServiceImpl implements JWTService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        
        // Ajouter des informations personnalisées
        claims.put("role", userDetails.getAuthorities());
        claims.put("email", userDetails.getUsername());
        
        return Jwts.builder()
            .setClaims(claims)                    // Données utilisateur
            .setSubject(userDetails.getUsername()) // Identifiant principal
            .setIssuedAt(new Date())              // Date création
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSignKey(), SignatureAlgorithm.HS256) // Signature
            .compact();
    }
    
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### Configuration Spring Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Désactivé pour API REST
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Endpoints publics
                .anyRequest().authenticated()                // Reste protégé
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### Filtre JWT personnalisé

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) {
        
        // 1. Extraire le token du header Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7); // Enlever "Bearer "
        
        // 2. Extraire l'email du token
        String userEmail = jwtService.extractUserName(token);
        
        // 3. Si utilisateur pas encore authentifié dans ce contexte
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // 4. Charger les détails utilisateur
            UserDetails userDetails = memberService.loadUserByUsername(userEmail);
            
            // 5. Valider le token
            if (jwtService.isTokenValid(token, userDetails)) {
                
                // 6. Créer l'authentification Spring Security
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                // 7. Définir le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 8. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}
```

### Questions de sécurité importantes

**1. Pourquoi STATELESS ?**
```java
.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
```
- Pas de session côté serveur
- Scalabilité horizontale possible
- Chaque requête est indépendante

**2. Pourquoi désactiver CSRF ?**
```java
.csrf(csrf -> csrf.disable())
```
- CSRF protège les formulaires web
- API REST avec JWT = pas de cookies = pas de risque CSRF
- Authentification par header, pas par cookie

**3. Pourquoi un refresh token ?**
```java
// Token principal : 24h (court pour sécurité)
// Refresh token : 7 jours (long pour UX)
```
- Compromis sécurité/expérience utilisateur
- Token volé = impact limité (24h max)
- Utilisateur ne se reconnecte pas tous les jours

---

## 📝 Logging et AOP

### Comprendre AOP (Aspect-Oriented Programming)

**Problème sans AOP :**
```java
@Service
public class AuthenticationService {
    
    public JwtResponse authenticate(SigninCredentials credentials) {
        // ❌ Code métier mélangé avec logging
        log.info("Tentative d'authentification pour: {}", credentials.getEmail());
        long startTime = System.currentTimeMillis();
        
        try {
            // Logique métier
            JwtResponse response = doAuthenticate(credentials);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Authentification réussie en {}ms", duration);
            return response;
            
        } catch (Exception e) {
            log.error("Erreur authentification: {}", e.getMessage());
            throw e;
        }
    }
}
```

**Solution avec AOP :**
```java
@Service
public class AuthenticationService {
    
    @LogExecution // ✅ Une simple annotation
    public JwtResponse authenticate(SigninCredentials credentials) {
        // ✅ Seulement la logique métier
        return doAuthenticate(credentials);
    }
}

@Aspect
@Component
public class LoggingAspect {
    
    @Around("@annotation(LogExecution)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed(); // Exécution méthode
            long duration = System.currentTimeMillis() - startTime;
            log.info("Méthode {} exécutée en {}ms", 
                joinPoint.getSignature().getName(), duration);
            return result;
            
        } catch (Exception e) {
            log.error("Erreur dans {}: {}", 
                joinPoint.getSignature().getName(), e.getMessage());
            throw e;
        }
    }
}
```

### Avantages de l'approche AOP

1. **Séparation des préoccupations** : Logique métier ≠ Logging
2. **Réutilisabilité** : Un aspect pour toute l'application
3. **Maintenabilité** : Changement de logging = un seul endroit
4. **Testabilité** : Tests métier sans pollution du logging

### Types d'aspects dans notre projet

```java
// 1. Aspect technique (performance, erreurs)
@Aspect
public class LoggingAspect {
    @Around("execution(* com.ecclesiaflow.springsecurity.services.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) {
        // Logging technique
    }
}

// 2. Aspect métier (opérations critiques)
@Aspect
public class BusinessOperationLoggingAspect {
    @AfterReturning("@annotation(LogExecution)")
    public void logBusinessOperation(JoinPoint joinPoint) {
        // Audit métier
    }
}
```

---

## 📖 Documentation API

### Pourquoi deux approches de documentation ?

**1. Annotations dans le code**
```java
@Operation(
    summary = "Génération de token d'authentification",
    description = "Authentifie un utilisateur et génère un token JWT"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Token généré avec succès"),
    @ApiResponse(responseCode = "401", description = "Identifiants incorrects")
})
public ResponseEntity<JwtAuthenticationResponse> generateToken(@Valid @RequestBody SigninRequest request) {
    // ...
}
```

**Avantages :**
- ✅ Documentation toujours à jour (couplée au code)
- ✅ Génération automatique
- ✅ Validation au compile-time

**Inconvénients :**
- ❌ Code verbeux
- ❌ Mélange documentation/logique

**2. Fichier OpenAPI séparé**
```yaml
paths:
  /api/auth/token:
    post:
      summary: Génération de token d'authentification
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SigninRequest'
```

**Avantages :**
- ✅ Documentation riche (exemples, descriptions détaillées)
- ✅ Séparation claire
- ✅ Réutilisable par d'autres outils

**Inconvénients :**
- ❌ Peut devenir obsolète
- ❌ Maintenance manuelle

### Configuration OpenAPI

```java
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI ecclesiaFlowOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("EcclesiaFlow Authentication API")
                .version("1.0.0")
                .description("API d'authentification centralisée"))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### Versioning API

```java
@PostMapping(value = "/token", produces = "application/vnd.ecclesiaflow.auth.v1+json")
```

**Pourquoi ce format ?**
- `vnd.` = Vendor-specific (format propriétaire)
- `ecclesiaflow.auth` = Namespace de l'application
- `v1` = Version de l'API
- `+json` = Format de données

**Avantages :**
- Évolution de l'API sans casser les clients
- Clients peuvent spécifier la version voulue
- Dépréciation progressive des anciennes versions

---

*[Le guide continue avec les tests, la qualité du code, et les bonnes pratiques...]*
