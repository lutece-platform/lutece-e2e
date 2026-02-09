# Lutece E2E Agent

Agent IA conversationnel pour l'automatisation des tests E2E sur Lutece 8, utilisant Playwright et LangChain4j CDI.

## Technologies

- **Java 17** - Langage
- **Jakarta EE 10** (CDI, JAX-RS, JSON-B) - APIs Enterprise
- **MicroProfile** (Config 3.1, Health 4.0, OpenAPI 3.1) - Configuration externalisee, sante, doc API
- **OpenLiberty 26** - Serveur d'application (module `lutece-e2e-web`)
- **Playwright 1.41** - Automatisation navigateur (Chromium headless)
- **LangChain4j 1.9** + **langchain4j-cdi 1.0** - Framework IA avec integration CDI
- **Azure OpenAI GPT-4o** - LLM par defaut (Anthropic Claude et OpenAI supportes en alternative)
- **Weld SE 5.1** - Implementation CDI pour les modules CLI et tests
- **JUnit 5** + **Testcontainers** - Tests
- **Maven** - Build multi-modules

## Architecture multi-modules

```
lutece-e2e/                 (parent POM)
├── lutece-e2e-core/        Page Objects Playwright + Actions metier + BrowserManager
├── lutece-e2e-agent/       Service IA LangChain4j (@RegisterAIService) + Tools
├── lutece-e2e-tests/       Tests E2E (CDI + bo3 Playwright direct, Testcontainers)
├── lutece-e2e-cli/         Interface ligne de commande (Weld SE)
├── lutece-e2e-web/         API REST JAX-RS sur OpenLiberty (WAR) + interface web chat
└── playwright-driver/      Driver Playwright pre-extrait (contournement wsjar OpenLiberty)
```

### Dependances inter-modules

- `lutece-e2e-agent` -> `lutece-e2e-core`
- `lutece-e2e-tests` -> `lutece-e2e-core`, `lutece-e2e-agent`
- `lutece-e2e-web` -> `lutece-e2e-agent` (avec exclusions Weld/SmallRye car fournis par OpenLiberty)
- `lutece-e2e-cli` -> `lutece-e2e-agent`

## Architecture en couches

```
[Interface Web / CLI] -> [API REST JAX-RS] -> [LuteceAiService] -> [LLM]
                                                     |
                                               [Tools LangChain4j]
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

### Page Objects CDI (`lutece-e2e-core/pages/`)
- Scope CDI : `@Dependent` (nouvelle instance par injection)
- Etendent `BasePage` qui fournit acces a `Page` Playwright via `BrowserManager`
- Methodes chainees (fluent API) : `myPage.navigateTo().fillForm(value)`
- Selecteurs CSS/XPath pour les elements Lutece (Bootstrap 5, offcanvas)

### Page Objects POJO (`lutece-e2e-core/pages/bo/`)
- POJOs instancies via `new LoginPage(page, baseUrl)` (pas de CDI)
- Recoivent directement `Page` Playwright et `baseUrl` dans le constructeur
- Methodes chainees (fluent API) identique aux pages CDI
- Utilises par les tests bo3 (Playwright direct, sans conteneur CDI)
- 11 pages : LoginPage, AdminMenuPage, SitePropertiesPage, WorkflowListPage, WorkflowCreationFormPage, WorkflowEditPage, FormsListPage, FormsCreationPage, FormsEditPage, FormsFrontOfficePage, FormsResponsesPage

### Actions (`lutece-e2e-core/actions/`)
- Scope CDI : `@ApplicationScoped`
- Injectent les Page Objects et le `BrowserManager`
- Retournent `ActionResult<T>` (succes/echec avec message)

### Tools (`lutece-e2e-agent/tools/`)
- Scope CDI : `@ApplicationScoped`
- Annotes `@Tool("description pour l'IA")` (LangChain4j)
- Parametres annotes `@P("description du parametre")`
- Injectent les Actions correspondantes
- Retournent `String` (message pour le LLM)

### Service IA (`lutece-e2e-agent/agent/LuteceAiService.java`)
- Interface annotee `@RegisterAIService` (LangChain4j CDI)
- Declare les Tools disponibles, le chat model et la chat memory
- `@SystemMessage` definit le comportement de l'agent (instructions en francais)

### API REST (`lutece-e2e-web/web/`)
- `AgentResource` : `@Path("/agent")` - endpoints `/chat`, `/health`, `/config`, `/config/url`, `/config/test`
- `ChatEndpoint` : WebSocket pour le streaming (si present)
- DTOs internes en classes statiques (ChatRequest, ChatResponse, etc.)
- `JaxRsApplication` : `@ApplicationPath("/api")`

### BrowserManager (`lutece-e2e-core/core/BrowserManager.java`)
- `@ApplicationScoped` - singleton gerant le cycle de vie Playwright
- `@PostConstruct` : initialise Playwright + Chromium + contexte
- `@PreDestroy` : cleanup navigateur
- Gestion de l'etat d'authentification (sauvegarde/restauration)
- URL de base configurable dynamiquement via `setBaseUrl()`

### Interface Web (`lutece-e2e-web/src/main/webapp/index.html`)
- Application single-file HTML avec CSS + JS inline
- Theme clair inspire de Claude.ai (terracotta #D97757, cream #FAF9F7)
- Chat, modals (settings, workflow, formulaires), toast notifications

## Configuration

### MicroProfile Config
- Fichiers dans `src/main/resources/META-INF/microprofile-config.properties`
- Le module `lutece-e2e-web` surcharge avec `config_ordinal=500`
- Variables Azure OpenAI injectees depuis les variables d'environnement

### Proprietes cles
```properties
# LLM
azure.openai.api.key=...
azure.openai.endpoint=...
azure.openai.deployment.name=...

# LangChain4j CDI
dev.langchain4j.cdi.plugin.lutece-chat-model.class=...
dev.langchain4j.cdi.plugin.lutece-chat-model.config.*=...

# Lutece cible
lutece.base.url=http://localhost:9080/site-deontologie
lutece.admin.username=admin
lutece.admin.password=adminadmin

# Playwright
browser.headless=true
browser.timeout=30000
playwright.driver.path=...
```

### OpenLiberty (`lutece-e2e-web/src/main/liberty/config/`)
- `server.xml` : features webProfile-10.0, mpConfig-3.1, mpHealth-4.0, mpOpenAPI-3.1
- `server.env` : variables d'environnement (cles API, driver path)
- `jvm.options` : proxy reseau si necessaire
- Classloader `parentLast` pour compatibilite ServiceLoader LangChain4j

## Build et execution

```bash
# Build complet
mvn clean install -DskipTests

# Demarrer le serveur web
cd lutece-e2e-web && mvn liberty:run
# -> http://localhost:9090/lutece-e2e-web/
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

# Suite complete avec conteneurs (23 tests) - Docker requis, aucune instance prealable
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
lutece-e2e-core/src/main/java/fr/paris/lutece/e2e/pages/
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
│   └── LuteceContainer.java       # GenericContainer + health check HTTP
└── testsuites/
    ├── ContainerSetup.java                # Demarre MariaDB + Lutece via Testcontainers
    ├── ContainerIntegrationSuite.java     # Suite Docker (23 tests)
    ├── WorkflowFormsIntegrationSuite.java # Suite externe (22 tests)
    ├── RbacConfigurationTest.java         # 5 tests - droits utilisateur
    ├── WorkflowCreationTest.java          # 7 tests - workflow + etats + actions
    ├── FormsCreationTest.java             # 9 tests - formulaires + questions
    ├── FormsSubmissionTest.java           # 1 test  - soumission front-office
    ├── LoginTest.java                     # 4 tests - authentification
    ├── AdminNavigationTest.java           # 4 tests - navigation back-office
    ├── LoginContainerTest.java            # 1 test  - login via conteneur
    └── CreationQuestionTypeTextLongTest.java  # 1 test standalone
```

### Configuration MicroProfile (`config_ordinal`)

Les tests utilisent SmallRye/MicroProfile Config avec des ordinals :
- `100` (defaut) : `lutece-e2e-core` et `lutece-e2e-agent`
- `350` : `lutece-e2e-tests` (surcharge core/agent)
- `400` : System properties `-D...` (surcharge tout)

Cela permet de surcharger via la ligne de commande : `-Dtest.headless=true`, `-Dlutece.base.url=...`

## Ajouter une nouvelle fonctionnalite (Tool)

1. **Page Object** dans `lutece-e2e-core/src/main/java/.../pages/MyPage.java` (`@Dependent`)
2. **Action** dans `lutece-e2e-core/src/main/java/.../actions/MyActions.java` (`@ApplicationScoped`)
3. **Tool** dans `lutece-e2e-agent/src/main/java/.../tools/MyTools.java` (`@ApplicationScoped`)
4. **Enregistrer** le Tool dans `LuteceAiService` : `tools = {..., MyTools.class}`

## Points d'attention

- Le driver Playwright est pre-extrait dans `playwright-driver/` pour contourner l'incompatibilite wsjar d'OpenLiberty
- `PreextractedDriver` est une implementation custom referencee via `playwright.driver.impl`
- Les selecteurs Lutece 8 utilisent Bootstrap 5 (offcanvas, modals) - attention aux changements de version
- La chat memory est limitee a 20 messages (`maxMessages=20`)
- L'agent demande l'URL du site Lutece au premier message si elle n'est pas configuree
- Les tests bo3 utilisent `setFullPage(true)` pour les screenshots debug - necessite un timeout suffisant (30s)
- En mode conteneur, le demarrage de Lutece prend ~3-4 minutes (Liquibase + initialisation)
- Les tests CDI et bo3 coexistent dans des packages separes sans conflit
- Les Page Objects bo (POJOs) sont dans `lutece-e2e-core` (`fr.paris.lutece.e2e.pages.bo`) pour centraliser tout le code Playwright dans un seul module
