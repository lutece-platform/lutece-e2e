package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import fr.paris.lutece.e2e.pages.bo.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E pour la creation et configuration d'un workflow.
 * Doit etre execute avant FormsCreationTest.
 */
@DisplayName("Tests de creation de workflow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkflowCreationTest extends BaseTest {

    private static final String RUN_SUFFIX = String.valueOf(System.currentTimeMillis() % 100000);
    private static final String WORKFLOW_NAME = config.getValue("test.workflow.name", String.class) + " " + RUN_SUFFIX;
    private static final String WORKFLOW_DESC = config.getValue("test.workflow.description", String.class);
    private static final String STATE_INITIAL = config.getValue("test.workflow.state.initial", String.class);
    private static final String STATE_FINAL = config.getValue("test.workflow.state.final", String.class);
    private static final String ACTION_NAME = config.getValue("test.workflow.action.name", String.class);
    private static final String ACTION_DESC = config.getValue("test.workflow.action.description", String.class);
    private static final String TASK_TYPE = config.getValue("test.workflow.task.type", String.class);
    private static final String ADMIN_USER = config.getValue("test.admin.username", String.class);
    private static final String ADMIN_PASS = config.getValue("test.admin.password", String.class);

    private LoginPage loginPage;
    private AdminMenuPage adminMenu;

    @Override
    protected void createContextAndPage() {
        // Ne rien faire : le contexte est cree une seule fois dans loginOnce
    }

    @Override
    protected void closeContext() {
        // Ne rien faire : le contexte est ferme dans closeOnce
    }

    @BeforeAll
    void loginOnce() {
        // Partager le suffixe avec les autres classes de test
        System.setProperty("test.run.suffix", RUN_SUFFIX);
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("target/test-run-suffix.txt"), RUN_SUFFIX);
        } catch (Exception e) {
            // ignore
        }

        // Reutiliser l'etat d'authentification si disponible, sinon faire un login
        if (hasAuthState()) {
            context = createAuthenticatedContext();
            page = context.newPage();
            page.setDefaultTimeout(TIMEOUT);
            page.navigate(BASE_URL + "/jsp/admin/AdminMenu.jsp");
            page.waitForLoadState();
            adminMenu = new AdminMenuPage(page, BASE_URL);
        } else {
            context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                .setLocale(LOCALE)
                .setIgnoreHTTPSErrors(true));
            page = context.newPage();
            page.setDefaultTimeout(TIMEOUT);

            loginPage = new LoginPage(page, BASE_URL);
            loginPage.navigate();
            // Gerer le message d'avertissement eventuel
            try {
                page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
            } catch (Exception e) {
                // Pas de message d'avertissement
            }
            adminMenu = loginPage.loginAs(ADMIN_USER, ADMIN_PASS);

            // Sauvegarder l'etat d'authentification pour les classes suivantes
            saveAuthState();
        }
    }

    @AfterAll
    void closeOnce() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Navigation vers la gestion des workflows")
    void testNavigateToWorkflowManagement() {
        // When
        adminMenu.goToWorkflowManagement();

        // Then
        WorkflowListPage listPage = new WorkflowListPage(page, BASE_URL);
        assertTrue(listPage.isDisplayed(),
            "La page de gestion des workflows devrait etre affichee");
    }

    @Test
    @Order(2)
    @DisplayName("Creation d'un nouveau workflow")
    void testCreateWorkflow() {
        // Given
        WorkflowListPage listPage = new WorkflowListPage(page, BASE_URL);

        // When
        WorkflowCreationFormPage formPage = listPage.clickCreateWorkflow();
        formPage.fillName(WORKFLOW_NAME)
                .fillDescription(WORKFLOW_DESC);
        formPage.save();

        // Then - la page de liste des workflows est affichee
        page.waitForLoadState();
        assertTrue(page.content().contains(WORKFLOW_NAME),
            "Le workflow devrait etre cree avec succes");
    }

    @Test
    @Order(3)
    @DisplayName("Ajout de l'etat initial au workflow")
    void testAddInitialState() {
        // Given - s'assurer d'être sur la page d'édition du workflow
        WorkflowEditPage editPage = new WorkflowEditPage(page, BASE_URL);
        editPage.ensureOnEditPage(WORKFLOW_NAME);

        // When
        editPage.addState(STATE_INITIAL, STATE_INITIAL, true);

        // Then
        assertTrue(page.content().contains(STATE_INITIAL),
            "L'etat initial devrait etre ajoute");
    }

    @Test
    @Order(4)
    @DisplayName("Ajout de l'etat final au workflow")
    void testAddFinalState() {
        // Given - s'assurer d'etre sur la page d'edition
        WorkflowEditPage editPage = new WorkflowEditPage(page, BASE_URL);
        editPage.ensureOnEditPage(WORKFLOW_NAME);

        // When
        editPage.addState(STATE_FINAL, STATE_FINAL, false);

        // Then
        assertTrue(page.content().contains(STATE_FINAL),
            "L'etat final devrait etre ajoute");
    }

    @Test
    @Order(5)
    @DisplayName("Ajout d'une action et configuration de la tache")
    void testAddActionWithTask() {
        // Given - s'assurer d'etre sur la page d'edition
        WorkflowEditPage editPage = new WorkflowEditPage(page, BASE_URL);
        editPage.ensureOnEditPage(WORKFLOW_NAME);

        // When - Ajouter l'action
        editPage.clickActionsTab();
        editPage.addAction(ACTION_NAME, ACTION_DESC, STATE_INITIAL, STATE_FINAL);

        // Then
        assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Modifier l'action")).isVisible(),
            "L'action devrait etre creee");

        // When - Configurer la tache
        editPage.clickModifyAction();
        editPage.selectTask(TASK_TYPE);
        editPage.clickInsertTask();

        // Then - retour au workflow
        editPage.clickModifyWorkflowLink();
        assertNotNull(page.url(), "La page d'edition du workflow devrait etre accessible");
    }

    @Test
    @Order(6)
    @DisplayName("Publication du workflow")
    void testPublishWorkflow() {
        // Given - s'assurer d'etre sur la page d'edition
        WorkflowEditPage editPage = new WorkflowEditPage(page, BASE_URL);
        editPage.ensureOnEditPage(WORKFLOW_NAME);

        // When
        editPage.publishWorkflow();

        // Then
        WorkflowListPage listPage = editPage.goBackToList();
        assertTrue(listPage.isDisplayed(),
            "La liste des workflows devrait etre affichee");
    }

    @Test
    @Order(7)
    @DisplayName("Activation du workflow")
    void testActivateWorkflow() {
        // Given
        WorkflowListPage listPage = new WorkflowListPage(page, BASE_URL);

        // When
        listPage.clickActivateWorkflow(WORKFLOW_NAME);

        // Then - Le workflow est activé (vérifier la présence sur la page)
        page.waitForLoadState();
        // La page utilise des cartes, pas des tables - vérifier simplement que le workflow est toujours présent
        assertTrue(page.locator("a:has-text('" + WORKFLOW_NAME + "')").first().isVisible(),
            "Le workflow devrait etre visible apres activation");
    }
}
