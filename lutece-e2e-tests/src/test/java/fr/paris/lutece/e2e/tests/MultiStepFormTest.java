package fr.paris.lutece.e2e.tests;

import fr.paris.lutece.e2e.actions.AuthActions;
import fr.paris.lutece.e2e.actions.FormsActions;
import fr.paris.lutece.e2e.pages.BasePage;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test E2E pour formulaire multi-étapes avec questions sur chaque étape.
 */
@EnableAutoWeld
@AddPackages({AuthActions.class, BasePage.class})
@DisplayName("Test formulaire multi-étapes")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiStepFormTest {

    @Inject
    AuthActions authActions;

    @Inject
    FormsActions formsActions;

    private String formTitle;
    private static final String STEP1 = "Saisie";
    private static final String STEP2 = "Validation";

    @BeforeAll
    void setup() {
        String runSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        formTitle = "FormMultiStep_" + runSuffix;
    }

    @Test
    @Order(1)
    @DisplayName("1. Connexion")
    void testLogin() {
        var result = authActions.login("admin", "adminadmin");
        assertTrue(result.isSuccess(), "Connexion: " + result.getError());
    }

    @Test
    @Order(2)
    @DisplayName("2. Création du formulaire")
    void testCreateForm() {
        var result = formsActions.createForm(formTitle, "WF_Test3");
        assertTrue(result.isSuccess(), "Création formulaire: " + result.getError());
        System.out.println(">>> Formulaire créé: " + formTitle);
    }

    @Test
    @Order(3)
    @DisplayName("3. Ajout étape 1 (Saisie - non finale)")
    void testAddStep1() {
        var result = formsActions.addStep(STEP1, false);
        assertTrue(result.isSuccess(), "Ajout étape 1: " + result.getError());
        System.out.println(">>> Étape 1 ajoutée: " + STEP1);
    }

    @Test
    @Order(4)
    @DisplayName("4. Ajout étape 2 (Validation - finale)")
    void testAddStep2() {
        var result = formsActions.addStep(STEP2, true);
        assertTrue(result.isSuccess(), "Ajout étape 2: " + result.getError());
        System.out.println(">>> Étape 2 ajoutée: " + STEP2);
    }

    @Test
    @Order(5)
    @DisplayName("5. Question texte 'Nom' sur Saisie")
    void testAddTextQuestionStep1() {
        var result = formsActions.addTextQuestion(STEP1, "Nom");
        assertTrue(result.isSuccess(), "Question Nom: " + result.getError());
        System.out.println(">>> Question 'Nom' ajoutée à " + STEP1);
    }

    @Test
    @Order(6)
    @DisplayName("6. Question nombre 'Age' sur Saisie")
    void testAddNumberQuestionStep1() {
        var result = formsActions.addNumberQuestion(STEP1, "Age");
        assertTrue(result.isSuccess(), "Question Age: " + result.getError());
        System.out.println(">>> Question 'Age' ajoutée à " + STEP1);
    }

    @Test
    @Order(7)
    @DisplayName("7. Question texte 'Commentaire' sur Validation")
    void testAddTextQuestionStep2() {
        var result = formsActions.addTextQuestion(STEP2, "Commentaire");
        assertTrue(result.isSuccess(), "Question Commentaire: " + result.getError());
        System.out.println(">>> Question 'Commentaire' ajoutée à " + STEP2);
    }

    @Test
    @Order(8)
    @DisplayName("8. Question date 'DateValidation' sur Validation")
    void testAddDateQuestionStep2() {
        var result = formsActions.addDateQuestion(STEP2, "DateValidation");
        assertTrue(result.isSuccess(), "Question DateValidation: " + result.getError());
        System.out.println(">>> Question 'DateValidation' ajoutée à " + STEP2);
    }

    @Test
    @Order(9)
    @DisplayName("9. Décocher Finale sur étape Saisie")
    void testUncheckFinaleStep1() {
        var result = formsActions.uncheckStepFinaleByName(STEP1);
        assertTrue(result.isSuccess(), "Décocher Finale: " + result.getError());
        System.out.println(">>> Finale décochée sur " + STEP1);
    }

    @Test
    @Order(10)
    @DisplayName("10. Transition Saisie -> Validation")
    void testConfigureTransition() {
        var result = formsActions.configureStepTransition(STEP1);
        assertTrue(result.isSuccess(), "Transition: " + result.getError());
        System.out.println(">>> Transition configurée: " + STEP1 + " -> " + STEP2);
    }

    @AfterAll
    void cleanup() {
        System.out.println(">>> Test terminé pour: " + formTitle);
        authActions.logout();
    }
}
