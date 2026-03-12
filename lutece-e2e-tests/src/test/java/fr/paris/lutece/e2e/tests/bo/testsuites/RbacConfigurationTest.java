package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import fr.paris.lutece.e2e.pages.bo.LoginPage;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de configuration RBAC (Role-Based Access Control).
 *
 * Ce test configure les permissions pour un role admin:
 * - Ajout des controles de ressources (Forms, Workflow, Comments, etc.)
 * - Attribution des droits aux utilisateurs
 * - Configuration des fonctionnalites dans les groupes
 *
 * Note: Utilise @TestInstance(PER_CLASS) pour conserver la session entre les tests.
 * Sauvegarde l'etat d'authentification pour les classes suivantes.
 */
@DisplayName("Configuration RBAC")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RbacConfigurationTest extends BaseTest {

    private static final Logger LOGGER = LogManager.getLogger(RbacConfigurationTest.class);

    private String runSuffix;

    // Types de ressources RBAC a configurer
    private static final String[] RESOURCE_TYPES = {
        "GLOBAL_FORMS_ACTION",
        "FORMS_FORM",
        "FORM_PANEL_CONF",
        "WORKFLOW_ACTION_TYPE",
        "COMMENT",
        "WORKFLOW_STATE_TYPE",
        "UPLOAD_WORKFLOW_HISTORY"
    };

    /**
     * Cree le contexte et la page une seule fois pour toute la classe.
     * Override de BaseTest pour conserver la session entre les tests ordonnes.
     */
    @BeforeAll
    void setupContext() {
        // En mode container, utiliser le suffixe du contexte, sinon en generer un
            runSuffix = String.valueOf(System.currentTimeMillis() % 100000);

        context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            .setLocale(LOCALE)
            .setIgnoreHTTPSErrors(true));

        page = context.newPage();
        page.setDefaultTimeout(TIMEOUT);

        // Partager le suffixe avec les autres classes de test
        System.setProperty("test.run.suffix", runSuffix);
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("target/test-run-suffix.txt"), runSuffix);
        } catch (Exception e) {
            // ignore
        }

        LOGGER.info("Context cree pour les tests RBAC - Run suffix: {}", runSuffix);
    }

    /**
     * Ne rien faire entre les tests pour conserver la session.
     */
    @Override
    @BeforeEach
    protected void createContextAndPage() {
        // Ne pas recreer le contexte entre les tests
    }

    /**
     * Ne rien faire apres chaque test pour conserver la session.
     */
    @Override
    @AfterEach
    protected void closeContext() {
        // Ne pas fermer le contexte entre les tests
    }

    /**
     * Ferme le contexte apres tous les tests.
     */
    @AfterAll
    void teardownContext() {
        if (context != null) {
            context.close();
            LOGGER.info("Context ferme apres les tests RBAC");
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Connexion admin")
    void testLogin() {
        LOGGER.info("Connexion admin");
        LoginPage loginPage = new LoginPage(page, BASE_URL);
        loginPage.navigate();

        // Gerer le message d'avertissement eventuel
        try {
            if (page.locator("button:has-text('OK')").count() > 0) {
                page.locator("button:has-text('OK')").first().click();
            }
        } catch (Exception e) {
            // Pas de message
        }

        loginPage.loginAs("admin", "adminadmin");
        page.waitForLoadState();

        // Sauvegarder l'etat d'authentification pour les classes suivantes
        saveAuthState();

        assertFalse(page.url().contains("AdminLogin"), "Connexion reussie");
    }

    @Test
    @Order(2)
    @DisplayName("2. Navigation vers la gestion des roles")
    void testNavigateToRoleManagement() {
        LOGGER.info("Navigation vers la gestion des roles");

        // Navigation directe vers la page d'edition du role super_admin
        page.navigate(BASE_URL + "/jsp/admin/rbac/ViewRoleDescription.jsp?role_key=super_admin");
        page.waitForLoadState();

        LOGGER.info("Page de gestion des roles affichee");
    }

    @Test
    @Order(3)
    @DisplayName("3. Ajout des controles de ressources RBAC")
    void testAddResourceControls() {
        LOGGER.info("Ajout des controles de ressources RBAC");

        for (String resourceType : RESOURCE_TYPES) {
            addResourceControl(resourceType);
        }

        LOGGER.info("Tous les controles de ressources ont ete ajoutes");
    }

    private void addResourceControl(String resourceType) {
        LOGGER.info("Ajout du controle de ressource: {}", resourceType);

        // Selectionner le type de ressource
        page.locator("#resource_type").selectOption(resourceType);

        // Cliquer sur "Ajouter un controle"
        page.locator("button:has-text('Ajouter un contrôle')").click();
        page.waitForLoadState();

        // Cliquer sur "Suivant"
        page.locator("button:has-text('Suivant')").click();
        page.waitForLoadState();

        // Cliquer sur "Valider"
        page.locator("button:has-text('Valider')").click();
        page.waitForLoadState();

        LOGGER.info("Controle {} ajoute", resourceType);
    }
    @Test
    @Order(4)
    @DisplayName("4. Configuration des groupes de fonctionnalites")
    void testConfigureFeatureGroups() {
        LOGGER.info("Configuration des groupes de fonctionnalites");

        // Ouvrir le menu Systeme
        page.locator("a:has-text('Système')").first().click();
        page.waitForTimeout(500);

        // Navigation vers les parametres techniques
        page.locator("a:has-text('Paramètres techniques')").first().click();
        page.waitForLoadState();

        // Aller dans l'onglet "Affectation des fonctionnalites"
        page.locator("a:has-text('Affectation des fonctionnalit')").first().click();
        page.waitForLoadState();

        // Configurer FORMS_MANAGEMENT dans le groupe CONTENT (si present)
        if (page.locator("#group_name-FORMS_MANAGEMENT").count() > 0) {
            page.locator("#group_name-FORMS_MANAGEMENT").selectOption("CONTENT");
            // Le selectOption declenche un onchange qui soumet le formulaire automatiquement
            page.waitForLoadState();
        }

        // Configurer FORMS_SEARCH_INDEXATION dans le groupe CONTENT (si present)
        // Re-naviguer vers l'onglet features_management car le submit precedent a recharge la page
        page.locator("a:has-text('Affectation des fonctionnalit')").first().click();
        page.waitForLoadState();

        if (page.locator("#group_name-FORMS_SEARCH_INDEXATION").count() > 0) {
            page.locator("#group_name-FORMS_SEARCH_INDEXATION").selectOption("CONTENT");
            page.waitForLoadState();
        }

        LOGGER.info("Groupes de fonctionnalites configures");
    }
    @Test
    @Order(5)
    @DisplayName("5. Configuration des droits utilisateur")
    void testConfigureUserRights() {
        LOGGER.info("Configuration des droits utilisateur");

        // Navigation directe vers la page de gestion des droits de l'utilisateur admin (id=1)
        page.navigate(BASE_URL + "/jsp/admin/user/ManageUserRights.jsp?id_user=1");
        page.waitForLoadState();
        page.waitForTimeout(2000);

        // Screenshot pour debug
        takeScreenshotDebug("04-user-rights-page");
        LOGGER.info("URL actuelle: {}", page.url());

        // Verifier qu'on est sur la page des droits
        boolean isOnRightsPage = page.url().contains("ManageUserRights") ||
                                 page.locator("text=Liste des droits").count() > 0 ||
                                 page.locator("text=Droits").count() > 0;

        assertTrue(isOnRightsPage, "Page de gestion des droits affichee");

        // Cliquer sur le bouton Modifier pour passer en mode edition
        page.locator("a:has-text('Modifier'), button:has-text('Modifier')").first().click();
        page.waitForLoadState();
        page.waitForTimeout(1000);

        // Screenshot apres modification
        takeScreenshotDebug("04-user-rights-edit-mode");

        // Selectionner tous les droits
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Selectionner tout")).first().click();
        page.waitForTimeout(500);

        // Appliquer la liste de droits
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Appliquer cette liste de")).first().click();
        page.waitForLoadState();

        LOGGER.info("Droits utilisateur - configuration appliquee avec succes");
    }

    private void takeScreenshotDebug(String name) {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target/screenshots"));
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/" + name + ".png"))
                .setFullPage(true));
            LOGGER.info("Screenshot saved: target/screenshots/{}.png", name);
        } catch (Exception e) {
            LOGGER.warn("Failed to take screenshot: {}", e.getMessage());
        }
    }

}
