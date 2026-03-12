# Lutece E2E

Tests E2E pour Lutece 8 avec Playwright.

## Technologies

- **Java 17** - Langage
- **Jakarta EE 10** (CDI) - Injection de dependances
- **MicroProfile Config 3.1** - Configuration externalisee
- **Playwright 1.58** - Automatisation navigateur (Chromium headless)
- **Log4j2 2.23** - Logging
- **Allure 2.25** - Rapports de tests (screenshots, traces Playwright)
- **JUnit 5** + **Testcontainers** - Tests
- **Maven** - Build multi-modules
- **Parent POM** : `lutece-global-pom:8.0.1-SNAPSHOT`

## Architecture multi-modules

```
lutece-e2e/                 (parent POM)
├── lutece-e2e-core/        Page Objects Playwright + Actions metier + BrowserManager/BrowserSession
├── lutece-e2e-tests/       Tests E2E (Playwright direct, Testcontainers)
├── playwright-driver/      Driver Playwright 1.58 pre-extrait (contournement wsjar OpenLiberty)
└── plugin-e2e-agent/       Plugin Lutece avec agent IA (LangChain4j) pour creation workflow/formulaires
```

### Dependances inter-modules

- `lutece-e2e-tests` -> `lutece-e2e-core`
- `plugin-e2e-agent` -> `lutece-e2e-core`

## Architecture en couches

```
[JUnit 5 / Suites]                     [LangChain4j Agent]
       |                                       |
 [Page Objects POJO]                    [Page Objects CDI]
       |                                       |
 [BaseTest / Playwright]                [BrowserSession @RequestScoped]
       |                                       |
 [Site Lutece 8]                        [BrowserManager @ApplicationScoped]
                                               |
                                        [Site Lutece 8]
```

## Patterns et conventions

### Page Objects POJO (`lutece-e2e-core/src/java/.../pages/bo/`)
- POJOs instancies via `new LoginPage(page, baseUrl)` (pas de CDI)
- Recoivent directement `Page` Playwright et `baseUrl` dans le constructeur
- Methodes chainees (fluent API) : `loginPage.fillUsername("x").fillPassword("y").clickLogin()`
- 11 pages : LoginPage, AdminMenuPage, SitePropertiesPage, WorkflowListPage, WorkflowCreationFormPage, WorkflowEditPage, FormsListPage, FormsCreationPage, FormsEditPage, FormsFrontOfficePage, FormsResponsesPage
- Selecteurs CSS/XPath pour les elements Lutece (Bootstrap 5, offcanvas)
- **Utilises par** : `lutece-e2e-tests` (pipeline CI)

### Page Objects CDI (`lutece-e2e-core/src/java/.../pages/`)
- Scope CDI : `@Dependent` (nouvelle instance par injection)
- Etendent `BasePage` qui fournit acces a `Page` Playwright via `BrowserSession`
- Constructeur : `@Inject public MyPage(BrowserSession browser)`
- `FormsPage` inclut des methodes FO (front office) : `navigateToFrontOffice()`, `fillTextFieldFO()`, `fillNumberFieldFO()`, `fillDateFieldFO()`, `clickNextStepFO()`, `clickViewSummaryFO()`, `clickValidateSummaryFO()`
- **Utilises par** : `plugin-e2e-agent` (agent IA)

### Actions (`lutece-e2e-core/src/java/.../actions/`)
- Scope CDI : `@ApplicationScoped`
- Injectent les Page Objects CDI et `BrowserSession`
- Retournent `ActionResult<T>` (succes/echec avec message)
- `FormsActions` inclut `submitFormFrontOffice()` pour la soumission de formulaire en front office
- **Utilises par** : `plugin-e2e-agent` (agent IA)

### BrowserManager (`lutece-e2e-core/src/java/.../core/BrowserManager.java`)
- `@ApplicationScoped` - singleton gerant le processus Chromium (couteux a creer)
- `@PostConstruct` : initialise Playwright + lance Chromium
- `@PreDestroy` : ferme le navigateur et Playwright
- `getBrowser()` : retourne le browser Chromium (relance si necessaire)
- Configuration : URL de base, headless, timeout, viewport, locale, screenshots path
- **Ne gere PAS les contextes/pages** - delegue a `BrowserSession`

### BrowserSession (`lutece-e2e-core/src/java/.../core/BrowserSession.java`)
- `@RequestScoped` - un contexte navigateur isole par requete HTTP
- `@PostConstruct` : cree un `BrowserContext` + `Page` depuis `BrowserManager.getBrowser()`
- `@PreDestroy` : ferme automatiquement le contexte (fenetre) en fin de requete
- Methodes : `navigate()`, `screenshot()`, `getCurrentUrl()`, `getPage()`, `waitForLoad()`, `evaluate()`
- **Auto-recovery** : si `TargetClosedError`, recree le contexte et retente la navigation
- **Multi-utilisateurs** : chaque requete HTTP a son propre contexte isole, pas d'interference
- **Stateless** : aucun etat partage entre les requetes

### PlaywrightDriverResolver (`lutece-e2e-core/src/java/.../core/PlaywrightDriverResolver.java`)
- Classe utilitaire statique pour la resolution du driver Playwright
- Compatible Playwright 1.58+ (`node`) et legacy (`playwright.sh`)
- Ordre de resolution : env `PLAYWRIGHT_DRIVER_PATH` -> property `playwright.driver.path` -> `$HOME/.playwright/driver/node` (1.58+) -> `$HOME/.playwright/driver/playwright.sh` (legacy)
- Configure via system properties uniquement (pas de hack reflection)
- Appelee dans le bloc `static {}` de `BrowserManager`

### PreextractedDriver (`lutece-e2e-core/src/java/.../core/PreextractedDriver.java`)
- Implementation custom de `Driver` Playwright utilisant un driver pre-extrait
- Lit le chemin via `PlaywrightDriverResolver.getConfiguredDriverPath()`
- `driverDir()` retourne le repertoire parent du binaire (ex: `driver/linux/`)
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

### Pattern de tests

Les tests utilisent le pattern **POJO Playwright direct** via `BaseTest` :
- Package `fr.paris.lutece.e2e.tests.bo.*`
- Page Objects POJO du core (`fr.paris.lutece.e2e.pages.bo`)
- Suites ordonnees avec `@TestMethodOrder` et `@Order`
- **Independants de BrowserManager/BrowserSession** - gerent Playwright directement

### Fonctionnalites

- **Playwright Tracing** : chaque test est trace, la trace est sauvegardee en cas d'echec (`target/traces/*.zip`)
- **Allure Report** : screenshots et traces attaches automatiquement aux rapports (`target/allure-results/`)
- **Screenshot on failure** : capture automatique via `ScreenshotOnFailureExtension`

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

# Suite complete (22 tests) - necessite Lutece demarre sur localhost:9080
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.WorkflowFormsIntegrationSuite

# Test standalone
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.CreationQuestionTypeTextLongTest

# --- Mode Testcontainers (CI/Docker) ---

# Suite complete avec conteneurs (22 tests) - Docker requis, aucune instance prealable
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.ContainerIntegrationSuite \
  -Dlutece.image=nexus-docker-fastdeploy.api.paris.mdp/bild/p30/site-integration-forms:8.0.0-SNAPSHOT \
  -Dtest.headless=true

# Surcharger l'URL de base (mode externe)
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.WorkflowFormsIntegrationSuite \
  -Dlutece.base.url=http://mon-serveur:9080/site-deontologie
```

### Structure des tests

```
lutece-e2e-core/src/java/fr/paris/lutece/e2e/pages/bo/
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
│   ├── BaseTest.java                      # Cycle de vie Playwright + Tracing
│   └── ScreenshotOnFailureExtension.java  # Screenshots + traces Allure sur echec
├── containers/
│   └── LuteceContainer.java              # GenericContainer + patch JDBC MySQL + health check HTTP
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

1. **Page Object POJO** dans `lutece-e2e-core/src/java/.../pages/bo/MyPage.java`
2. **Test** dans `lutece-e2e-tests/src/test/java/.../tests/bo/testsuites/MyTest.java` (extends `BaseTest`)

Pour l'agent IA (optionnel) :
3. **Page Object CDI** dans `lutece-e2e-core/src/java/.../pages/MyPage.java` (`@Dependent`, constructeur `BrowserSession`)
4. **Action** dans `lutece-e2e-core/src/java/.../actions/MyActions.java` (`@ApplicationScoped`, injecte `BrowserSession`)
5. **Tool LangChain4j** dans `plugin-e2e-agent/src/java/.../tools/MyTools.java` (`@ApplicationScoped`, injecte l'Action)
6. **Enregistrer le Tool** dans `LuteceAiService.java` : ajouter la classe dans `@RegisterAIService(tools = {...})`

### Tools disponibles

| Tool | Classe | Description |
|------|--------|-------------|
| ConfigTools | `ConfigTools.java` | Configuration URL Lutece |
| AuthTools | `AuthTools.java` | Authentification (login/logout) |
| WorkflowTools | `WorkflowTools.java` | Gestion workflows (creation, etats, actions, activation) |
| FormsTools | `FormsTools.java` | Gestion formulaires (creation, etapes, questions, publication) |
| IntegrationTools | `IntegrationTools.java` | Suite d'integration complete en un seul appel (workflow + formulaire + soumission FO) |

## Driver Playwright pre-extrait

### Structure (Playwright 1.58+)

```
playwright-driver/java/driver/
├── linux/
│   ├── node              # Binaire Node.js
│   ├── playwright.sh     # Wrapper script (node + package/cli.js)
│   ├── package/          # Code Playwright (cli.js, lib/, etc.)
│   └── LICENSE
├── linux-arm64/
├── mac/
├── mac-arm64/
└── win32_x64/
```

### Mise a jour du driver

Pour mettre a jour le driver pre-extrait vers une nouvelle version :

```bash
cd playwright-driver/java
rm -rf driver
unzip -qo ~/.m2/repository/com/microsoft/playwright/driver-bundle/VERSION/driver-bundle-VERSION.jar "driver/*"
chmod +x driver/linux/node

# Creer le wrapper playwright.sh (requis pour OpenLiberty)
cat > driver/linux/playwright.sh << 'SCRIPT'
#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/node" "$DIR/package/cli.js" "$@"
SCRIPT
chmod +x driver/linux/playwright.sh
```

### Configuration pour OpenLiberty (plugin-e2e-agent)

Le driver pre-extrait est **obligatoire** sur OpenLiberty car le filesystem `wsjar://`
est incompatible avec l'extraction automatique du driver depuis le JAR.

```bash
# Variable d'environnement (recommande)
export PLAYWRIGHT_DRIVER_PATH=/chemin/vers/playwright-driver/java/driver/linux/node

# Ou system property dans jvm.options de Liberty
-Dplaywright.driver.path=/chemin/vers/playwright-driver/java/driver/linux/node
```

**Important** : la variable `-D` passee a Maven n'est PAS propagee a la JVM Liberty.
Utiliser la variable d'environnement `PLAYWRIGHT_DRIVER_PATH` ou `jvm.options`.

## plugin-e2e-agent

Plugin Lutece deploye avec un agent IA (LangChain4j) pour la creation de workflows
et formulaires via une interface web (index.html) et un webservice REST.

### Architecture stateless

L'agent utilise une architecture stateless multi-utilisateurs :

```
Requete HTTP POST /rest/agent/chat
       |
       v
 AgentResource (@ApplicationScoped)
       |
       v
 LuteceAiService (LangChain4j)    <-- @MemoryId sessionId pour isoler la memoire
       |
       v
 Tools (AuthTools, WorkflowTools, FormsTools, IntegrationTools, ConfigTools)
       |
       v
 Actions (AuthActions, WorkflowActions, FormsActions)
       |
       v
 BrowserSession (@RequestScoped)      <-- contexte isole par requete
       |
       v
 BrowserManager (@ApplicationScoped)  <-- processus Chromium partage
```

- **BrowserManager** : singleton, lance Chromium une seule fois au demarrage
- **BrowserSession** : cree un `BrowserContext` + `Page` par requete HTTP, ferme automatiquement en fin de requete (`@PreDestroy`)
- Chaque utilisateur a sa propre fenetre navigateur isolee
- Pas d'etat partage entre les requetes (stateless)

### Memoire de chat (ChatMemory)

La memoire de conversation est isolee par utilisateur via `ChatMemoryProvider` :

```
Frontend (onglet A)                Frontend (onglet B)
  X-Session-Id: abc-123              X-Session-Id: abc-123
  localStorage: historique           localStorage: synchronise via event 'storage'
         |                                  |
         └──────────┬───────────────────────┘
                    |
              HttpSession (JSESSIONID)
              attrs: { "e2e.agent.sessionIds": [abc-123] }
                    |
                    v
         UserChatMemoryProvider (@ApplicationScoped, @Named("per-user-memory"))
         ConcurrentHashMap<sessionId, ChatMemory(20 msgs)>
                    |
          ┌─────────┴──────────────┐
          v                        v
   Session active             Session expiree (Liberty timeout)
                                   |
                                   v
                        ChatMemorySessionListener (@WebListener)
                          sessionDestroyed() → evict(sessionId)
```

- **`UserChatMemoryProvider`** : CDI bean `@ApplicationScoped` implementant `ChatMemoryProvider`. Stocke une `MessageWindowChatMemory(maxMessages=20)` par `sessionId` dans une `ConcurrentHashMap`
- **`ChatMemorySessionListener`** : `@WebListener` Jakarta EE. A chaque requete, `AgentResource` enregistre le `X-Session-Id` dans le `HttpSession`. Quand Liberty invalide le `HttpSession` (timeout), le listener appelle `evict()` pour liberer la `ChatMemory`
- **`@MemoryId`** : annotation LangChain4j sur le parametre `sessionId` des methodes `chat()` et `executeTask()` de `LuteceAiService`. LangChain4j appelle `ChatMemoryProvider.get(sessionId)` a chaque invocation
- **Frontend** : `localStorage` (partage entre onglets) pour l'historique visuel des messages. Au chargement, `GET /rest/agent/session/active` verifie si la `ChatMemory` existe cote serveur. Si non (session expiree), l'historique local est purge et le message de bienvenue reaffiche. L'evenement `storage` synchronise les onglets en temps reel
- **Timeout** : configure dans `server.xml` via `<httpSession invalidationTimeout="30m"/>`

### Lancement local

```bash
export PLAYWRIGHT_DRIVER_PATH=/home/yahiaoui/lutece/workspace-site/playwright/lutece-e2e/playwright-driver/java/driver/linux/node
export http_proxy=http://192.168.64.41:8080
export https_proxy=http://192.168.64.41:8080

cd plugin-e2e-agent
mvn clean liberty:dev \
  -DconfigDirectory="/home/yahiaoui/lutece/openlibertyConfigFirectory/main/liberty/config" \
  -Dmaven.test.skip=true -DhotTests=false
```

**Attention** : ne pas mettre de `/` en fin d'URL proxy (`http://host:port` et non `http://host:port/`)
sinon Liberty echoue avec `For input string: "8080/"`.

## Points d'attention

- Le driver Playwright est pre-extrait dans `playwright-driver/` pour contourner l'incompatibilite wsjar d'OpenLiberty
- `PreextractedDriver` est une implementation custom referencee via `playwright.driver.impl`
- `PlaywrightDriverResolver` resout le chemin du driver via env var / property / `$HOME` (pas de chemin en dur)
- Compatible Playwright 1.58+ (`node`) et legacy <= 1.51 (`playwright.sh`)
- Les selecteurs Lutece 8 utilisent Bootstrap 5 + Tabler Icons (`.ti-*`) - attention aux changements de version
- Les tests utilisent `setFullPage(true)` pour les screenshots debug - necessite un timeout suffisant (30s)
- En mode conteneur, le demarrage de Lutece prend ~2-3 minutes (Liquibase + initialisation)
- Les Page Objects POJO sont dans `lutece-e2e-core` (`fr.paris.lutece.e2e.pages.bo`) pour centraliser tout le code Playwright dans un seul module
- Les sources Java sont dans `src/java/` (convention Lutece global POM), pas `src/main/java/`
- Les tests PER_CLASS qui gerent leur propre contexte doivent appeler `startTracing()` pour beneficier du tracing Playwright
- Les screenshots et l'etat d'authentification sont stockes dans `java.io.tmpdir` (`/tmp/lutece-e2e/`) pour compatibilite OpenLiberty
- Les tests pipeline (`lutece-e2e-tests`) sont **independants** de `BrowserManager`/`BrowserSession` — ils gerent Playwright via `BaseTest`

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
