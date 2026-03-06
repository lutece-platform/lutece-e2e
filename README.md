# Lutece E2E

Tests E2E pour Lutece 8 avec Playwright.

## Architecture

```
lutece-e2e/                 (parent POM - herite de lutece-global-pom 8.0.1)
├── lutece-e2e-core/        Page Objects Playwright + Actions metier + BrowserManager
├── lutece-e2e-tests/       Tests E2E (Playwright direct + Testcontainers)
└── playwright-driver/      Driver Playwright pre-extrait
```

```
[JUnit 5 / Suites]
       |
 [Page Objects POJO]
       |
 [BrowserManager / Playwright]
       |
 [Site Lutece 8]
```

## Prerequis

- Java 17+
- Maven 3.9+
- Docker ou Podman (mode Testcontainers)
- Site Lutece 8 demarre (mode instance existante)

## Build

```bash
mvn clean install -DskipTests
```

## Tests

### Mode instance existante (developpeur)

Necessite une instance Lutece 8 demarree et accessible.

```bash
# Suite complete (22 tests) - RBAC, Workflow, Forms, Soumission FO
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.WorkflowFormsIntegrationSuite

# Surcharger l'URL cible
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.WorkflowFormsIntegrationSuite \
  -Dlutece.base.url=http://mon-serveur:9080/site-deontologie
```

### Mode Testcontainers (CI/Docker)

Aucune instance Lutece prealable requise. Docker demarre automatiquement MariaDB + Lutece.

```bash
mvn verify -pl lutece-e2e-tests -Pcontainer-tests \
  -Dlutece.image=nexus-docker-fastdeploy.api.paris.mdp/bild/p30/site-integration-forms:8.0.0-SNAPSHOT \
  -Dtest.headless=true
```

### Couverture des tests

| Suite | Tests | Couverture |
|---|---|---|
| `RbacConfigurationTest` | 5 | Login admin, droits utilisateur, selection roles |
| `WorkflowCreationTest` | 6 | Creation workflow, etats initial/final, action, activation |
| `FormsCreationTest` | 9 | Formulaire, etapes, questions (texte, nombre, date, commentaire) |
| `FormsSubmissionTest` | 1 | Soumission front-office + verification reponse |
| **Total** | **22** | **Parcours complet RBAC -> Workflow -> Forms -> FO** |

## Configuration

Les proprietes MicroProfile Config sont resolues par ordre de priorite :

| Priorite | Source | Ordinal |
|---|---|---|
| Haute | System properties (`-D...`) | 400 |
| | `lutece-e2e-tests` config | 350 |
| Basse | `lutece-e2e-core` config | 100 |

### Proprietes cles

```properties
lutece.base.url=http://localhost:9080/site-deontologie
lutece.admin.username=admin
lutece.admin.password=adminadmin
browser.headless=true
browser.timeout=30000
```

## Structure du code

### Sources (`lutece-e2e-core/src/java/`)

Convention Lutece : les sources sont dans `src/java/` (pas `src/main/java/`).

```
src/java/fr/paris/lutece/e2e/
├── actions/               # Actions metier (@ApplicationScoped)
│   ├── AuthActions.java
│   ├── FormsActions.java
│   └── WorkflowActions.java
├── core/                  # Infrastructure Playwright
│   ├── ActionResult.java
│   ├── BrowserManager.java
│   ├── PlaywrightDriverResolver.java
│   └── PreextractedDriver.java
└── pages/                 # Page Objects
    ├── BasePage.java      # Base CDI (@Dependent)
    ├── LoginPage.java
    ├── AdminMenuPage.java
    ├── WorkflowPage.java
    ├── FormsPage.java
    └── bo/                # POJOs Playwright direct
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
```

### Tests (`lutece-e2e-tests/src/test/java/`)

```
tests/bo/
├── config/
│   └── BaseTest.java              # Cycle de vie Playwright
├── containers/
│   └── LuteceContainer.java       # GenericContainer + patch JDBC
└── testsuites/
    ├── ContainerSetup.java
    ├── ContainerIntegrationSuite.java
    ├── WorkflowFormsIntegrationSuite.java
    ├── RbacConfigurationTest.java
    ├── WorkflowCreationTest.java
    ├── FormsCreationTest.java
    └── FormsSubmissionTest.java
```

## Ajouter une fonctionnalite

1. **Page Object** dans `lutece-e2e-core/src/java/.../pages/bo/MyPage.java`
2. **Test** dans `lutece-e2e-tests/src/test/java/.../tests/bo/testsuites/MyTest.java`
3. Ajouter le test dans la suite (`ContainerIntegrationSuite` / `WorkflowFormsIntegrationSuite`)

## Depannage

### JDBC sous Podman

`LuteceContainer` patch automatiquement le `<jdbcDriver>` du `server.xml` pour forcer les classes MySQL. Voir `CLAUDE.md` pour les details techniques.

### Timeout Playwright

- Verifier que le site Lutece est demarre et accessible
- Les selecteurs Lutece 8 utilisent Bootstrap 5 + Tabler Icons (`.ti-*`)
- Augmenter le timeout via `-Dbrowser.timeout=60000`

## Technologies

- **Java 17** + **Jakarta EE 10** (CDI)
- **MicroProfile Config 3.1** (SmallRye)
- **Playwright 1.41** (Chromium headless)
- **Log4j2** (logging)
- **JUnit 5** + **Testcontainers** (tests)
- **Maven** (build, parent: `lutece-global-pom:8.0.1-SNAPSHOT`)

## Licence

Projet interne - Ville de Paris
