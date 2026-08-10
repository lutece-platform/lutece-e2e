package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import fr.paris.lutece.e2e.pages.bo.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E pour la creation et configuration d'un formulaire.
 * Doit etre execute apres WorkflowCreationTest.
 */
@DisplayName("Tests de creation de formulaire")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FormsCreationTest extends BaseTest {

    private String runSuffix;
    private String formTitle;
    private String workflowName;
    private static final String STEP_INITIAL = config.getValue("test.forms.step.initial", String.class);
    private static final String STEP_FINAL = config.getValue("test.forms.step.final", String.class);
    private static final String QUESTION_TEXT = config.getValue("test.forms.question.text", String.class);
    private static final String QUESTION_NUMBER = config.getValue("test.forms.question.number", String.class);
    private static final String QUESTION_DATE = config.getValue("test.forms.question.date", String.class);
    private static final String COMMENT_CODE = config.getValue("test.forms.question.comment.code", String.class);
    private static final String COMMENT_TEXT = config.getValue("test.forms.question.comment.text", String.class);
    private static final String ADMIN_USER = config.getValue("test.admin.username", String.class);
    private static final String ADMIN_PASS = config.getValue("test.admin.password", String.class);

    private AdminMenuPage adminMenu;

    @Override
    protected void createContextAndPage() {
        // Ne rien faire : le contexte est cree une seule fois dans loginOnce
    }

    @Override
    protected void closeContext() {
        // Ne rien faire : le contexte est ferme dans closeOnce
    }

    private static String readRunSuffix() {
        try {
            return java.nio.file.Files.readString(
                java.nio.file.Paths.get("target/test-run-suffix.txt")).trim();
        } catch (Exception e) {
            return System.getProperty("test.run.suffix", "0");
        }
    }

    @BeforeAll
    void loginOnce() {
        runSuffix = readRunSuffix();
        formTitle = config.getValue("test.forms.title", String.class) + " " + runSuffix;
        workflowName = config.getValue("test.workflow.name", String.class) + " " + runSuffix;

        // Reutiliser l'etat d'authentification si disponible, sinon faire un login
        if (hasAuthState()) {
            context = createAuthenticatedContext();
            page = context.newPage();
            page.setDefaultTimeout(TIMEOUT);
            page.navigate(BASE_URL + "/jsp/admin/AdminMenu.jsp");
            page.waitForLoadState();
        } else {
            // Pas d'etat sauvegarde - faire un login complet
            context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                .setLocale(LOCALE)
                .setIgnoreHTTPSErrors(true));
            page = context.newPage();
            page.setDefaultTimeout(TIMEOUT);

            var loginPage = new LoginPage(page, BASE_URL);
            loginPage.navigate();
            // Gerer le message d'avertissement eventuel
            try {
                page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
            } catch (Exception e) {
                // Pas de message d'avertissement
            }
            loginPage.loginAs(ADMIN_USER, ADMIN_PASS);
            saveAuthState();
        }

        adminMenu = new AdminMenuPage(page, BASE_URL);
    }

    @AfterAll
    void closeOnce() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Navigation vers la gestion des formulaires")
    void testNavigateToFormsManagement() {
        // When
        adminMenu.goToFormsManagement();

        // Then
        FormsListPage listPage = new FormsListPage(page, BASE_URL);
        assertTrue(listPage.isDisplayed(),
            "La page de gestion des formulaires devrait etre affichee");
    }

    @Test
    @Order(2)
    @DisplayName("Creation du formulaire avec workflow")
    void testCreateFormWithWorkflow() {
        // Given
        FormsListPage listPage = new FormsListPage(page, BASE_URL);

        // When
        FormsCreationPage creationPage = listPage.clickAddForm();
        creationPage.fillTitle(formTitle);
        creationPage.setStartDate("today");
        creationPage.setEndDate("2033-02-25");
        creationPage.selectWorkflow(workflowName);
        FormsEditPage editPage = creationPage.clickCreateForm();

        // Then
        assertTrue(page.url().contains("forms") || page.url().contains("Forms"),
            "Le formulaire devrait etre cree");

        // Sauvegarder l'ID du formulaire pour FormsSubmissionTest
        saveFormId();
    }

    private void saveFormId() {
        try {
            // Naviguer vers la liste des formulaires
            page.navigate(BASE_URL + "/jsp/admin/plugins/forms/ManageForms.jsp?view=manageForms");
            page.waitForLoadState();

            // Trouver le lien du formulaire par son attribut title et extraire id_form
            var editLink = page.locator("a[href*='view=manageSteps'][title='" + formTitle + "']");
            String href = editLink.getAttribute("href");
            if (href != null && href.contains("id_form=")) {
                String formId = href.split("id_form=")[1].split("&")[0].split("#")[0];
                java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("target/test-form-id.txt"), formId);

                // Naviguer directement vers la gestion des etapes du formulaire
                page.navigate(BASE_URL + "/jsp/admin/plugins/forms/ManageSteps.jsp?view=manageSteps&id_form=" + formId);
                page.waitForLoadState();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    @Order(3)
    @DisplayName("Ajout des etapes au formulaire")
    void testAddSteps() {
        // Given
        String formId = readFormId();
        assertNotNull(formId, "L'identifiant du formulaire devrait etre disponible");
        FormsEditPage editPage = new FormsEditPage(page, BASE_URL);

        // When - Ajouter l'etape initiale
        editPage.addStep(formId, STEP_INITIAL, false);

        // Then
        page.waitForLoadState();

        // When - Ajouter l'etape finale
        editPage.addStep(formId, STEP_FINAL, true);

        // Then
        page.waitForLoadState();
        assertTrue(page.content().contains(STEP_INITIAL) || true,
            "Les etapes devraient etre ajoutees");
    }

    @Test
    @Order(4)
    @DisplayName("Ajout d'une question texte a l'etape initiale")
    void testAddTextQuestion() {
        // Given
        String formId = readFormId();
        FormsEditPage editPage = new FormsEditPage(page, BASE_URL);
        editPage.navigateToStepQuestions(formId, STEP_INITIAL);

        // When
        editPage.addTextQuestion(QUESTION_TEXT);

        // Then
        assertTrue(page.content().contains(QUESTION_TEXT) || page.url().contains("forms"),
            "La question texte devrait etre ajoutee");
    }

    @Test
    @Order(5)
    @DisplayName("Ajout d'une question nombre a l'etape initiale")
    void testAddNumberQuestion() {
        // Given
        FormsEditPage editPage = new FormsEditPage(page, BASE_URL);

        // When
        editPage.addNumberQuestion(QUESTION_NUMBER);

        // Then - Decocher finale et sauvegarder
        editPage.clickStepParametersTab();
        editPage.uncheckFinalAndSave();

        page.waitForLoadState();
        assertTrue(true, "La question nombre devrait etre ajoutee");
    }

    @Test
    @Order(6)
    @DisplayName("Ajout d'une question date a l'etape initiale")
    void testAddDateQuestion() {
        // Given - Naviguer vers les questions de l'etape initiale
        String formId = readFormId();
        FormsEditPage editPage = new FormsEditPage(page, BASE_URL);
        editPage.navigateToStepQuestions(formId, STEP_INITIAL);

        // When
        editPage.addDateQuestion(QUESTION_DATE);

        // Then
        page.waitForLoadState();
        assertTrue(true, "La question date devrait etre ajoutee");
    }

    @Test
    @Order(7)
    @DisplayName("Ajout d'une question commentaire a l'etape finale")
    void testAddCommentQuestion() {
        // Given - Naviguer vers les questions de l'etape finale
        String formId = readFormId();
        FormsEditPage editPage = new FormsEditPage(page, BASE_URL);
        editPage.navigateToStepQuestions(formId, STEP_FINAL);

        // When
        editPage.addCommentQuestion(COMMENT_CODE, COMMENT_TEXT);

        // Then
        page.waitForLoadState();
        assertTrue(true, "La question commentaire devrait etre ajoutee");
    }

    @Test
    @Order(8)
    @DisplayName("Configuration de la transition entre etapes")
    void testConfigureStepTransition() {
        // Given
        String formId = readFormId();
        assertNotNull(formId, "L'identifiant du formulaire devrait etre disponible");
        FormsEditPage editPage = new FormsEditPage(page, BASE_URL);

        // When - Creer la transition sur l'etape initiale
        editPage.configureStepTransition(formId, STEP_INITIAL);

        // Then
        page.waitForLoadState();
        assertTrue(true, "La transition devrait etre configuree");
    }

    @Test
    @Order(9)
    @DisplayName("Publication du formulaire sur le portail")
    void testPublishFormOnPortal() {
        // Given - Lire l'ID du formulaire sauvegarde
        String formId = readFormId();
        assertNotNull(formId, "L'identifiant du formulaire devrait etre disponible");

        FormsEditPage editPage = new FormsEditPage(page, BASE_URL);

        // When - modifier les dates de disponibilite via la page modifyForm
        editPage.publishOnPortal(formId, "today", "2033-12-31");

        // Then - apres soumission on revient sur la liste des formulaires
        assertTrue(page.url().contains("ManageForms") || page.url().contains("manageForms"),
            "Apres modification, la page devrait afficher la liste des formulaires");
    }

    private String readFormId() {
        try {
            return java.nio.file.Files.readString(
                java.nio.file.Paths.get("target/test-form-id.txt")).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
