# Lutece E2E

Tests E2E pour Lutece 8 avec Playwright.

## Technologies

- **Java 17** - Langage
- **Jakarta EE 10** (CDI) - Injection de dependances
- **MicroProfile Config 3.1** - Configuration externalisee
- **Playwright 1.41** - Automatisation navigateur (Chromium headless)
- **Log4j2 2.23** - Logging
- **Weld SE 5.1** - Implementation CDI pour les tests
- **JUnit 5** + **Testcontainers** - Tests
- **Maven** - Build multi-modules
- **Parent POM** : `lutece-global-pom:8.0.1-SNAPSHOT`

## Architecture multi-modules

```
lutece-e2e/                 (parent POM)
├── lutece-e2e-core/        Page Objects Playwright + Actions metier + BrowserManager
├── lutece-e2e-tests/       Tests E2E (CDI + bo3 Playwright direct, Testcontainers)
└── playwright-driver/      Driver Playwright pre-extrait (contournement wsjar OpenLiberty)
```

### Dependances inter-modules

- `lutece-e2e-tests` -> `lutece-e2e-core`

## Architecture en couches

```
[JUnit 5 / Suites]
       |
 [Actions metier]
       |
 [Page Objects]
       |
 [BrowserManager / Playwright]
       |
 [Site Lutece 8]
```

## Patterns et conventions

### Page Objects CDI (`lutece-e2e-core/src/java/.../pages/`)
- Scope CDI : `@Dependent` (nouvelle instance par injection)
- Etendent `BasePage` qui fournit acces a `Page` Playwright via `BrowserManager`
- Methodes chainees (fluent API) : `myPage.navigateTo().fillForm(value)`
- Selecteurs CSS/XPath pour les elements Lutece (Bootstrap 5, offcanvas)

### Page Objects POJO (`lutece-e2e-core/src/java/.../pages/bo/`)
- POJOs instancies via `new LoginPage(page, baseUrl)` (pas de CDI)
- Recoivent directement `Page` Playwright et `baseUrl` dans le constructeur
- Methodes chainees (fluent API) identique aux pages CDI
- Utilises par les tests bo3 (Playwright direct, sans conteneur CDI)
- 11 pages : LoginPage, AdminMenuPage, SitePropertiesPage, WorkflowListPage, WorkflowCreationFormPage, WorkflowEditPage, FormsListPage, FormsCreationPage, FormsEditPage, FormsFrontOfficePage, FormsResponsesPage

### Actions (`lutece-e2e-core/src/java/.../actions/`)
- Scope CDI : `@ApplicationScoped`
- Injectent les Page Objects et le `BrowserManager`
- Retournent `ActionResult<T>` (succes/echec avec message)

### BrowserManager (`lutece-e2e-core/src/java/.../core/BrowserManager.java`)
- `@ApplicationScoped` - singleton gerant le cycle de vie Playwright
- `@PostConstruct` : initialise Playwright + Chromium + contexte
- `@PreDestroy` : cleanup navigateur
- Gestion de l'etat d'authentification (sauvegarde/restauration)
- URL de base configurable dynamiquement via `setBaseUrl()`

### PlaywrightDriverResolver (`lutece-e2e-core/src/java/.../core/PlaywrightDriverResolver.java`)
- Classe utilitaire statique pour la resolution du driver Playwright
- Ordre de resolution : env `PLAYWRIGHT_DRIVER_PATH` -> property `playwright.driver.path` -> `$HOME/.playwright/driver/playwright.sh`
- Configure via system properties uniquement (pas de hack reflection)
- Appelee dans le bloc `static {}` de `BrowserManager`

### PreextractedDriver (`lutece-e2e-core/src/java/.../core/PreextractedDriver.java`)
- Implementation custom de `Driver` Playwright utilisant un driver pre-extrait
- Lit le chemin via `PlaywrightDriverResolver.getConfiguredDriverPath()`
- Contourne l'incompatibilite du filesystem `wsjar://` d'OpenLiberty

### Logging
- **Log4j2** dans `lutece-e2e-core` (API `org.apache.logging.log4j`)
- `StatusLogger` pour le logging dans les blocs statiques (avant initialisation Log4j2)
- Configuration dans `lutece-e2e-core/src/main/resources/log4j2.xml`
- Pont `log4j-slf4j2-impl` disponible dans le parent POM pour les librairies tierces utilisant SLF4J

## Configuration

### MicroProfile Config
- Fichiers dans `src/main/resources/META-INF/microprofile-config.properties`

### Proprietes cles
```properties
# Lutece cible
lutece.base.url=http://localhost:9080/site-deontologie
lutece.admin.username=admin
lutece.admin.password=adminadmin

# Playwright
browser.headless=true
browser.timeout=30000
playwright.driver.path=...
```

## Build et execution

```bash
# Build complet
mvn clean install -DskipTests
```

## Tests E2E (`lutece-e2e-tests`)

Le module de tests combine deux patterns de tests qui coexistent :

### Deux patterns de tests

1. **Tests CDI** (`fr.paris.lutece.e2e.tests.*`) - Utilise `@EnableAutoWeld`, injection CDI, Page Objects CDI du core (`fr.paris.lutece.e2e.pages`)
2. **Tests bo3** (`fr.paris.lutece.e2e.tests.bo.*`) - Playwright direct via `BaseTest`, Page Objects POJO du core (`fr.paris.lutece.e2e.pages.bo`), suites ordonnees

### Deux modes d'execution

```
Mode Instance Existante                 Mode Testcontainers (Docker)
========================                ================================

 Developpeur                             CI / Pipeline Jenkins
     |                                        |
     v                                        v
 mvn test                               mvn verify -Pcontainer-tests
 -Dtest=...Suite                         -Dlutece.image=...
     |                                        |
     v                                        v
 +------------------+                    +------------------+
 | JUnit 5 Suite    |                    | Failsafe Plugin  |
 +------------------+                    +------------------+
     |                                        |
     v                                        v
 +------------------+                    +------------------+
 | BaseTest         |                    | ContainerSetup   |
 | (Playwright)     |                    |   |              |
 +------------------+                    |   v              |
     |                                   | Testcontainers   |
     v                                   | +MariaDB         |
 +------------------+                    | +LuteceContainer |
 | Instance Lutece  |                    +------------------+
 | (deja demarree)  |                         |
 | localhost:9080   |                         v
 +------------------+                    +------------------+
                                         | Instance Lutece  |
                                         | (Docker auto)    |
                                         | port dynamique   |
                                         +------------------+
```

### Commandes

```bash
# --- Mode instance existante (developpeur) ---

# Suite complete bo3 (22 tests) - necessite Lutece demarre sur localhost:9080
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.WorkflowFormsIntegrationSuite

# Tests CDI existants
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.ListFormsTest

# Test standalone
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.CreationQuestionTypeTextLongTest

# --- Mode Testcontainers (CI/Docker) ---

# Suite complete avec conteneurs (22 tests) - Docker requis, aucune instance prealable
mvn verify -pl lutece-e2e-tests -Pcontainer-tests \
  -Dlutece.image=nexus-docker-fastdeploy.api.paris.mdp/bild/f98/site-deontologie:1.0.0-SNAPSHOT \
  -Dtest.headless=true

# Surcharger l'URL de base (mode externe)
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.WorkflowFormsIntegrationSuite \
  -Dlutece.base.url=http://mon-serveur:9080/site-deontologie
```

### Structure des tests bo3

```
lutece-e2e-core/src/java/fr/paris/lutece/e2e/pages/
├── BasePage.java              # Base CDI (existant)
├── LoginPage.java             # CDI (existant)
├── AdminMenuPage.java         # CDI (existant)
├── WorkflowPage.java          # CDI (existant)
├── FormsPage.java             # CDI (existant)
└── bo/                        # POJOs Playwright direct
    ├── LoginPage.java
    ├── AdminMenuPage.java
    ├── SitePropertiesPage.java
    ├── WorkflowListPage.java
    ├── WorkflowCreationFormPage.java
    ├── WorkflowEditPage.java
    ├── FormsListPage.java
    ├── FormsCreationPage.java
    ├── FormsEditPage.java
    ├── FormsFrontOfficePage.java
    └── FormsResponsesPage.java

lutece-e2e-tests/src/test/java/fr/paris/lutece/e2e/tests/bo/
├── config/
│   └── BaseTest.java              # Cycle de vie Playwright (BeforeAll/AfterAll)
├── containers/
│   └── LuteceContainer.java       # GenericContainer + patch JDBC MySQL + health check HTTP
└── testsuites/
    ├── ContainerSetup.java                # Demarre MariaDB + Lutece via Testcontainers
    ├── ContainerIntegrationSuite.java     # Suite Docker (22 tests)
    ├── WorkflowFormsIntegrationSuite.java # Suite externe (22 tests)
    ├── RbacConfigurationTest.java         # 5 tests - droits utilisateur
    ├── WorkflowCreationTest.java          # 6 tests - workflow + etats + actions
    ├── FormsCreationTest.java             # 9 tests - formulaires + questions
    ├── FormsSubmissionTest.java           # 1 test  - soumission front-office
    ├── LoginTest.java                     # 4 tests - authentification
    ├── AdminNavigationTest.java           # 4 tests - navigation back-office
    ├── LoginContainerTest.java            # 1 test  - login via conteneur
    └── CreationQuestionTypeTextLongTest.java  # 1 test standalone
```

### Configuration MicroProfile (`config_ordinal`)

Les tests utilisent SmallRye/MicroProfile Config avec des ordinals :
- `100` (defaut) : `lutece-e2e-core`
- `350` : `lutece-e2e-tests` (surcharge core)
- `400` : System properties `-D...` (surcharge tout)

Cela permet de surcharger via la ligne de commande : `-Dtest.headless=true`, `-Dlutece.base.url=...`

## Ajouter une nouvelle fonctionnalite

1. **Page Object** dans `lutece-e2e-core/src/java/.../pages/MyPage.java` (`@Dependent`)
2. **Action** dans `lutece-e2e-core/src/java/.../actions/MyActions.java` (`@ApplicationScoped`)
3. **Test** dans `lutece-e2e-tests/src/test/java/.../tests/bo/testsuites/MyTest.java`

## Points d'attention

- Le driver Playwright est pre-extrait dans `playwright-driver/` pour contourner l'incompatibilite wsjar d'OpenLiberty
- `PreextractedDriver` est une implementation custom referencee via `playwright.driver.impl`
- `PlaywrightDriverResolver` resout le chemin du driver via env var / property / `$HOME` (pas de chemin en dur)
- Les selecteurs Lutece 8 utilisent Bootstrap 5 + Tabler Icons (`.ti-*`) - attention aux changements de version
- Les tests bo3 utilisent `setFullPage(true)` pour les screenshots debug - necessite un timeout suffisant (30s)
- En mode conteneur, le demarrage de Lutece prend ~3-4 minutes (Liquibase + initialisation)
- Les tests CDI et bo3 coexistent dans des packages separes sans conflit
- Les Page Objects bo (POJOs) sont dans `lutece-e2e-core` (`fr.paris.lutece.e2e.pages.bo`) pour centraliser tout le code Playwright dans un seul module
- Les sources Java sont dans `src/java/` (convention Lutece global POM), pas `src/main/java/`

### Compatibilite Docker / Podman (JDBC)

`LuteceContainer.withMariaDB()` patch le `server.xml` de Liberty au demarrage du conteneur
pour forcer les classes MySQL sur le `<jdbcDriver>` :

```xml
<!-- Avant (dans l'image) -->
<jdbcDriver libraryRef="jdbcLib"/>

<!-- Apres (patche par sed au demarrage) -->
<jdbcDriver libraryRef="jdbcLib"
    javax.sql.DataSource="com.mysql.cj.jdbc.MysqlDataSource"
    javax.sql.ConnectionPoolDataSource="com.mysql.cj.jdbc.MysqlConnectionPoolDataSource"
    javax.sql.XADataSource="com.mysql.cj.jdbc.MysqlXADataSource"/>
```

**Pourquoi** : sous Podman, l'auto-detection JDBC de Liberty echoue car la feature
`persistence-3.1` fournit des classes H2 qui prennent le dessus sur MySQL.
L'erreur typique est `DSRA4000E: No implementations of org.h2.jdbcx.JdbcDataSource`.

**Contraintes techniques** :
- On NE change PAS `<properties>` en `<properties.mysql>` : cela modifie le comportement
  de connexion (DataSource class differente) et cause `Communications link failure` sur Docker
- On NE peut PAS utiliser `configDropins/overrides` : redeclarer un `<dataSource>` avec le meme
  `jndiName` cree un conflit JNDI (`CWWKG0031E`) car le dataSource original n'a pas d'attribut `id`
- On utilise `withCreateContainerCmdModifier` (override ENTRYPOINT) au lieu de `withCommand` (CMD)
  car certaines images Liberty ont un ENTRYPOINT qui ignore le CMD
- `BaseTest.BASE_URL` utilise `getOptionalValue().orElse(...)` pour eviter un
  `ExceptionInInitializerError` quand la propriete n'est pas encore definie (mode conteneur)
