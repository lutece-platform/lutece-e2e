package fr.paris.lutece.e2e.tests;

import fr.paris.lutece.e2e.actions.AuthActions;
import fr.paris.lutece.e2e.actions.FormsActions;
import fr.paris.lutece.e2e.actions.WorkflowActions;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E pour la creation de formulaires.
 * Utilise les Actions CDI injectees.
 */
@EnableAutoWeld
@AddPackages(AuthActions.class)
@DisplayName("Tests de creation de formulaire")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormsCreationTest {

    @Inject
    AuthActions authActions;

    @Inject
    WorkflowActions workflowActions;

    @Inject
    FormsActions formsActions;

    private String runSuffix;
    private String workflowName;
    private String formTitle;

    @BeforeAll
    void setup() {
        runSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        workflowName = "Test Workflow " + runSuffix;
        formTitle = "Test Form " + runSuffix;
    }

    @Test
    @Order(1)
    @DisplayName("Connexion a l'administration")
    void testLogin() {
        var result = authActions.login("admin", "adminadmin");

        assertTrue(result.isSuccess(), "La connexion devrait reussir: " + result.getError());
        assertTrue(authActions.isLoggedIn(), "La session devrait etre active");
    }

    @Test
    @Order(2)
    @DisplayName("Creation du workflow")
    void testCreateWorkflow() {
        var result = workflowActions.createWorkflow(workflowName, "Workflow de test");

        assertTrue(result.isSuccess(), "Le workflow devrait etre cree: " + result.getError());
    }

    @Test
    @Order(3)
    @DisplayName("Ajout des etats au workflow")
    void testAddStates() {
        var initialState = workflowActions.addState("Brouillon", "Etat initial", true);
        assertTrue(initialState.isSuccess(), "L'etat initial devrait etre cree");

        var finalState = workflowActions.addState("Publie", "Etat final", false);
        assertTrue(finalState.isSuccess(), "L'etat final devrait etre cree");
    }

    @Test
    @Order(4)
    @DisplayName("Ajout d'une action au workflow")
    void testAddAction() {
        var result = workflowActions.addAction("Publier", "Publication", "Brouillon", "Publie");

        assertTrue(result.isSuccess(), "L'action devrait etre creee: " + result.getError());
    }

    @Test
    @Order(5)
    @DisplayName("Activation du workflow")
    void testActivateWorkflow() {
        var result = workflowActions.activateWorkflow(workflowName);

        assertTrue(result.isSuccess(), "Le workflow devrait etre active: " + result.getError());
    }

    @Test
    @Order(6)
    @DisplayName("Creation du formulaire avec workflow")
    void testCreateForm() {
        var result = formsActions.createForm(formTitle, workflowName);

        assertTrue(result.isSuccess(), "Le formulaire devrait etre cree: " + result.getError());
        assertNotNull(result.getData(), "Les infos du formulaire ne devraient pas etre null");
        // Note: L'ID peut etre -1 si non extrait de l'URL, mais le formulaire est quand meme cree
    }

    @Test
    @Order(7)
    @DisplayName("Ajout d'une etape au formulaire")
    void testAddStep() {
        var result = formsActions.addStep("Informations personnelles", false);

        assertTrue(result.isSuccess(), "L'etape devrait etre ajoutee: " + result.getError());
    }

    @Test
    @Order(8)
    @DisplayName("Ajout de questions a l'etape")
    void testAddQuestions() {
        String stepName = "Informations personnelles";

        // Types de base (fonctionnent sans options)
        var textResult = formsActions.addTextQuestion(stepName, "Votre nom");
        assertTrue(textResult.isSuccess(), "La question texte devrait etre ajoutee");

        var numberResult = formsActions.addNumberQuestion(stepName, "Votre age");
        assertTrue(numberResult.isSuccess(), "La question nombre devrait etre ajoutee");

        var dateResult = formsActions.addDateQuestion(stepName, "Date de naissance");
        assertTrue(dateResult.isSuccess(), "La question date devrait etre ajoutee");

        // Types sans options
        var textareaResult = formsActions.addTextareaQuestion(stepName, "Description");
        assertTrue(textareaResult.isSuccess(), "La question zone de texte long devrait etre ajoutee");

        var fileResult = formsActions.addFileQuestion(stepName, "CV");
        assertTrue(fileResult.isSuccess(), "La question fichier devrait etre ajoutee");

        var imageResult = formsActions.addImageQuestion(stepName, "Photo");
        assertTrue(imageResult.isSuccess(), "La question image devrait etre ajoutee");

        var numberingResult = formsActions.addNumberingQuestion(stepName, "Numero dossier");
        assertTrue(numberingResult.isSuccess(), "La question numerotation devrait etre ajoutee");
    }

    @Test
    @Order(9)
    @DisplayName("Publication du formulaire")
    void testPublishForm() {
        var result = formsActions.publishForm(formTitle, "today");

        assertTrue(result.isSuccess(), "Le formulaire devrait etre publie: " + result.getError());
    }

    @AfterAll
    void cleanup() {
        authActions.logout();
    }
}
