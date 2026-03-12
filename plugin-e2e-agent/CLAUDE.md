# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

`plugin-e2e-agent` is a **Lutece 8 plugin** (`lutece-plugin` packaging) that exposes a REST API and web UI for an AI-powered E2E testing agent. It bridges the Lutece 8 core with the `lutece-e2e-agent` module (LangChain4j AI service + Tools) and `lutece-e2e-core` (Playwright Page Objects + Actions).

This plugin runs **inside a Lutece 8 site** (Tomcat), not as a standalone app. The parent multi-module project (`lutece-e2e/`) contains sibling modules (`lutece-e2e-core`, `lutece-e2e-agent`, `lutece-e2e-tests`, `lutece-e2e-web`, `lutece-e2e-cli`) — see the parent `CLAUDE.md` for the full architecture.

## Build

```bash
# Build this plugin only (from this directory)
mvn clean install -DskipTests

# Build the entire multi-module project (from parent lutece-e2e/)
cd .. && mvn clean install -DskipTests
```

The plugin uses `lutece-global-pom:8.0.1-SNAPSHOT` as parent and requires the Lutece snapshot repository (`https://dev.lutece.paris.fr/snapshot_repository`).

## Source Layout

Sources use the Lutece plugin convention — **not** `src/main/java` but `src/java`:

```
src/java/fr/paris/lutece/plugins/e2eagent/
├── agent/
│   ├── LuteceAiService.java            # LangChain4j AI service (@RegisterAIService, @MemoryId)
│   └── UserChatMemoryProvider.java     # ChatMemoryProvider CDI (@Named("per-user-memory"), ConcurrentHashMap)
├── tools/
│   ├── AuthTools.java              # Authentification (login/logout)
│   ├── ConfigTools.java            # Configuration URL Lutece
│   ├── FormsTools.java             # Gestion formulaires
│   ├── IntegrationTools.java       # Suite d'integration complete (workflow + form + soumission FO)
│   └── WorkflowTools.java         # Gestion workflows
└── web/
    ├── AgentResource.java               # JAX-RS REST API (@Path("/agent"), @ApplicationScoped)
    ├── ChatMemorySessionListener.java   # @WebListener: evict ChatMemory on HttpSession expiry
    └── PlaywrightStartupBean.java       # CDI startup: eager init of BrowserManager
src/java/META-INF/
    beans.xml                       # CDI bean-discovery-mode="annotated"
    microprofile-config.properties  # config_ordinal=500
webapp/
    plugins/e2e-agent/index.html    # Single-file web UI (vanilla JS, no frameworks)
    WEB-INF/plugins/e2e-agent.xml   # Plugin descriptor (no DB, no admin features)
```

## REST API

All endpoints are under `/rest/agent` (plugin-rest provides `@ApplicationPath("/rest/")`):

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/agent/chat` | Chat with AI agent (`LuteceAiService.chat()`) |
| GET | `/agent/health` | Health check |
| GET | `/agent/session/active` | Check if ChatMemory exists for X-Session-Id (session expiry check) |
| GET | `/agent/config` | Get current Lutece URL config |
| POST | `/agent/config/url` | Set target Lutece site URL |
| POST | `/agent/config/test` | Test connection + Lutece authentication (CSRF token flow) |

DTOs are inner static classes of `AgentResource`: `ChatRequest`, `ChatResponse`, `ConfigResponse`, `UrlRequest`, `TestResponse`.

## Key Dependencies

- **`lutece-e2e-agent`** — AI service (`LuteceAiService`) and LangChain4j Tools. Imported with exclusions for Weld SE, SmallRye Config, Jakarta APIs, SLF4J (all provided by Lutece/Tomcat).
- **`plugin-rest`** — Provides `LuteceRestApplication` and JAX-RS support.
- **`langchain4j-cdi-portable-ext` + `langchain4j-cdi-config`** — CDI integration and MicroProfile Config bridge for LangChain4j.
- **`langchain4j-azure-open-ai`** — Default LLM provider (Azure OpenAI).

## Configuration (MicroProfile Config)

Properties in `src/java/META-INF/microprofile-config.properties` with `config_ordinal=500`:

- `azure.openai.api.key/endpoint/deployment.name` — Azure OpenAI credentials (from env vars)
- `lutece.base.url` — Target Lutece site URL
- `lutece.admin.username/password` — Admin credentials
- `browser.headless/timeout/viewport.*` — Playwright browser settings

## Web UI

`webapp/plugins/e2e-agent/index.html` is a single-file HTML app (inline CSS + JS, no build step). Light theme inspired by Claude.ai with terracotta primary (#D97757) and cream backgrounds. Uses CSS variables in `:root` for theming.

Features:
- Chat interface with AI agent
- Persistance de l'historique des messages dans `localStorage` (partage entre onglets)
- Verification de session serveur au chargement (`GET /session/active`) — purge automatique si session expiree
- Synchronisation temps reel entre onglets via l'evenement `storage`
- Settings modal (URL Lutece cible, identifiants)
- Workflow modal (creation workflow avec etats/actions configurables)
- Form modal (creation formulaire avec etapes/questions configurables)
- Bouton "Test forms complet" (lance la suite d'integration via `IntegrationTools.runIntegrationSuite()`)
- Toast notifications
- URL badge affichant l'URL complete du site cible
- Preset URL : `https://p30-forms-integration.rec.apps.paris.mdp/lutece`

## LangChain4j Tools

| Tool | Description |
|------|-------------|
| `ConfigTools` | Configuration URL Lutece (`setLuteceUrl`, `isLuteceUrlConfigured`, `getLuteceUrl`) |
| `AuthTools` | Authentification (`login`, `logout`, `loginWithConfiguredCredentials`) |
| `WorkflowTools` | Workflows (`createCompleteWorkflow`, `listWorkflows`, `activateWorkflow`) |
| `FormsTools` | Formulaires (`createCompleteForm`, `listForms`, `publishForm`, types de questions) |
| `IntegrationTools` | Suite d'integration complete (`runIntegrationSuite`) — workflow + formulaire + soumission FO en un seul appel |

### IntegrationTools.runIntegrationSuite()

Reproduit le comportement de `ContainerIntegrationSuite` (sans ContainerSetup et RbacConfigurationTest) :
1. Verification authentification
2. Creation workflow (2 etats, 1 action, tache de publication, activation)
3. Creation formulaire avec workflow (2 etapes, questions texte/nombre/date + commentaire, transition, publication)
4. Soumission du formulaire en front office

Utilise les memes valeurs par defaut que les tests pipeline (`microprofile-config.properties` de `lutece-e2e-tests`).

## Chat Memory Architecture

La memoire de conversation est isolee par utilisateur :

- **`UserChatMemoryProvider`** (`@ApplicationScoped`, `@Named("per-user-memory")`) : implemente `ChatMemoryProvider`. Stocke une `MessageWindowChatMemory(maxMessages=20)` par `X-Session-Id` dans une `ConcurrentHashMap`
- **`LuteceAiService`** : utilise `chatMemoryProviderName = "per-user-memory"` (pas `chatMemoryName`). Les methodes `chat()` et `executeTask()` prennent `@MemoryId String sessionId` en premier parametre
- **`ChatMemorySessionListener`** (`@WebListener`) : enregistre les `X-Session-Id` comme attributs du `HttpSession`. Quand Liberty invalide la session (timeout), appelle `UserChatMemoryProvider.evict()` pour chaque ID enregistre
- **`AgentResource`** : injecte `HttpServletRequest`, appelle `httpRequest.getSession(true)` pour creer/obtenir le `HttpSession`, et enregistre le `X-Session-Id` via `ChatMemorySessionListener.registerSessionId()`
- **Frontend** : `localStorage` pour l'historique visuel (partage entre onglets). Au chargement, verifie `GET /session/active` — si `false`, purge l'historique et reaffiche le message de bienvenue. L'evenement `storage` synchronise les onglets
- **Configuration** : `<httpSession invalidationTimeout="30m"/>` dans `server.xml`

## Architecture Gotchas

- **Exclusions are critical**: `lutece-e2e-agent` dependency must exclude Weld SE, SmallRye Config, and Jakarta APIs to avoid conflicts with Lutece's CDI container.
- **Eager initialization**: `PlaywrightStartupBean` observes `@Initialized(ApplicationScoped.class)` to force BrowserManager init at startup — Playwright needs time to launch Chromium.
- **Authentication test flow**: `AgentResource.testConnection()` implements a multi-step HTTP flow: GET login page → extract CSRF token → POST credentials → follow redirect → verify landing on `AdminMenu.jsp`.
- **No database**: Plugin descriptor declares `db-pool-required=0`. All state is in-memory (BrowserManager, chat memory).
- **ChatMemory cleanup**: Tied to `HttpSession` lifecycle — no external dependency (Caffeine, etc.). `UserChatMemoryProvider.hasMemory()` checks existence without creating (used by `/session/active`).
