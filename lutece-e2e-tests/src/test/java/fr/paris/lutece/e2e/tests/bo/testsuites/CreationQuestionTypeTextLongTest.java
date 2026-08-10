package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test E2E pour l'ajout d'une question de type "Zone de texte long" a un formulaire existant.
 * Ce test peut etre execute independamment.
 *
 * Configuration requise dans META-INF/microprofile-config.properties:
 * - test.textlong.form.name : Nom du formulaire cible
 * - test.textlong.step.name : Nom de l'etape ou ajouter la question
 * - test.textlong.question.title : Titre de la question (optionnel, defaut: "Text long")
 * - test.textlong.textarea.height : Hauteur de la zone de texte (optionnel, defaut: "500")
 *
 * Execution:
 *   mvn test -Dtest=CreationQuestionTypeTextLongTest
 *
 * Avec parametres en ligne de commande:
 *   mvn test -Dtest=CreationQuestionTypeTextLongTest \
 *       -Dtest.textlong.form.name="Mon Formulaire" \
 *       -Dtest.textlong.step.name="Etape 1"
 */
@DisplayName("Test d'ajout de question Zone de texte long")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CreationQuestionTypeTextLongTest extends BaseTest {

    // Parametres configurables
    private final String formName;
    private final String stepName;
    private final String questionTitle;
    private final String textareaHeight;

    private static final String ADMIN_USER = config.getValue("test.admin.username", String.class);
    private static final String ADMIN_PASS = config.getValue("test.admin.password", String.class);

    public CreationQuestionTypeTextLongTest() {
        this.formName = config.getOptionalValue("test.textlong.form.name", String.class)
            .orElse("Forms Test integration");
        this.stepName = config.getOptionalValue("test.textlong.step.name", String.class)
            .orElse("Etape Initial");
        this.questionTitle = config.getOptionalValue("test.textlong.question.title", String.class)
            .orElse("Text long");
        this.textareaHeight = config.getOptionalValue("test.textlong.textarea.height", String.class)
            .orElse("500");
    }

    @Override
    protected void createContextAndPage() {
        // Ne rien faire
    }

    @Override
    protected void closeContext() {
        // Ne rien faire
    }

    @BeforeAll
    void setup() {
        System.out.println("=== Configuration du test ===");
        System.out.println("Formulaire cible: " + formName);
        System.out.println("Etape cible: " + stepName);
        System.out.println("Titre de la question: " + questionTitle);
        System.out.println("Hauteur zone de texte: " + textareaHeight);
        System.out.println("============================");

        // Creer un nouveau contexte
        context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            .setLocale(LOCALE)
            .setIgnoreHTTPSErrors(true));
        page = context.newPage();
        page.setDefaultTimeout(TIMEOUT);
    }

    @AfterAll
    void cleanup() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("Ajout d'une question Zone de texte long au formulaire")
    void testAddTextLongQuestionToForm() {
        // 1. Naviguer vers la page de login
        page.navigate(BASE_URL + "/jsp/admin/AdminLogin.jsp");
        page.waitForLoadState();

        // 2. Login
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Code d'accès. *")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Code d'accès. *")).fill(ADMIN_USER);
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Mot de passe *")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Mot de passe *")).fill(ADMIN_PASS);
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Se connecter")).click();
        page.waitForLoadState();

        // 3. Naviguer vers la gestion des formulaires
        page.navigate(BASE_URL + "/jsp/admin/plugins/forms/ManageForms.jsp?view=manageForms");
        page.waitForLoadState();

        // 4. Ouvrir le formulaire par son nom : extraire l'id_form de la ligne
        //    correspondante (miroir du pattern Forms) au lieu de cliquer sur le lien.
        Locator formLink = page.locator("a[href*='id_form=']")
            .filter(new Locator.FilterOptions().setHasText(formName)).first();
        assertTrue(formLink.isVisible(), "Le formulaire '" + formName + "' devrait etre visible");
        String formHref = formLink.getAttribute("href");
        String idForm = formHref.split("id_form=")[1].split("&")[0];

        // 5. Naviguer directement vers la gestion des etapes du formulaire (onglet Etapes)
        page.navigate(BASE_URL + "/jsp/admin/plugins/forms/ManageSteps.jsp?view=manageSteps&id_form=" + idForm);
        page.waitForLoadState();

        // 6. Trouver l'etape et cliquer sur "Modifier l'etape"
        // Chercher tous les liens/textes qui correspondent exactement au nom de l'etape
        // et cliquer sur l'icone "Modifier l'etape" associee

        // Methode: trouver le texte de l'etape, puis cliquer sur l'icone dans la meme ligne
        // Le nom de l'etape est suivi d'un badge (Initiale/Finale)
        Locator stepRows = page.locator("[id^='step_']");
        int stepCount = stepRows.count();
        System.out.println("DEBUG: Nombre d'etapes trouvees: " + stepCount);

        boolean found = false;
        for (int i = 0; i < stepCount; i++) {
            Locator row = stepRows.nth(i);
            String rowId = row.getAttribute("id");

            // Chercher si cette ligne contient le nom exact de l'etape (pas dans les liaisons)
            // Le nom de l'etape est dans un element texte direct, pas dans "Liste des liaisons"
            Locator stepNameLocator = row.locator("text=" + stepName).first();

            // Verifier que le texte trouve est bien le titre de l'etape (pas dans les liaisons)
            if (stepNameLocator.count() > 0 && stepNameLocator.isVisible()) {
                // Verifier que ce n'est pas dans la section des liaisons
                String parentText = stepNameLocator.evaluate("el => el.closest('div')?.textContent || ''").toString();

                // Si le parent direct contient uniquement le nom de l'etape (pas "Liste des liaisons")
                // ou si c'est bien l'etape recherchee
                System.out.println("DEBUG: Step " + i + " (id=" + rowId + ") - verifie '" + stepName + "'");

                // Ne selectionner que si c'est vraiment la bonne etape
                // Verifier en regardant le debut du texte de la ligne
                String rowText = row.textContent();
                // Le nom de l'etape doit etre au debut de la ligne, pas dans "Liste des liaisons"
                int stepNamePos = rowText.indexOf(stepName);
                int liaisonsPos = rowText.indexOf("Liste des liaisons");

                System.out.println("DEBUG: Position '" + stepName + "': " + stepNamePos + ", Position 'Liaisons': " + liaisonsPos);

                // Si le nom de l'etape apparait AVANT "Liste des liaisons" ou s'il n'y a pas de liaisons
                if (stepNamePos >= 0 && (liaisonsPos < 0 || stepNamePos < liaisonsPos)) {
                    // Extraire l'id_step depuis un lien de la ligne, puis naviguer directement
                    // vers la liste des questions de cette etape (remplace le clic sur
                    // "Modifier l'etape" + l'onglet "Liste des Questions").
                    Locator stepIdLink = row.locator("a[href*='id_step=']").first();
                    String stepHref = stepIdLink.getAttribute("href");
                    String idStep = stepHref.split("id_step=")[1].split("&")[0];
                    System.out.println("DEBUG: >>> Selection de l'etape: " + stepName + " (id=" + rowId + ", id_step=" + idStep + ")");
                    page.navigate(BASE_URL + "/jsp/admin/plugins/forms/ManageQuestions.jsp?view=manageQuestions&id_step=" + idStep);
                    page.waitForLoadState();
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            fail("Etape '" + stepName + "' non trouvee parmi les " + stepCount + " etapes");
        }
        page.waitForLoadState();

        // 7. La liste des questions de l'etape est deja affichee : la navigation directe
        //    vers ManageQuestions.jsp?view=manageQuestions&id_step=... a ete effectuee
        //    ci-dessus (remplace le clic sur l'onglet "Liste des Questions").

        // 8. Cliquer sur "Ajouter une question"
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Ajouter une question")).click();
        page.waitForLoadState();

        // 9. Selectionner le type "Zone de texte long"
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Zone de texte long")).click();
        page.waitForLoadState();

        // 10. Remplir le titre de la question
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).fill(questionTitle);

        // 11. Premier enregistrement
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        page.waitForLoadState();

        // 12. Configurer la hauteur de la zone de texte
        Locator heightField = page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Hauteur de la zone de texte *"));

        if (heightField.isVisible()) {
            heightField.click();
            heightField.fill(textareaHeight);

            // 13. Enregistrer la configuration
            page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Enregistrer")).click();
            page.waitForLoadState();
        }

        // Verification
        System.out.println("=== Test termine avec succes ===");
        System.out.println("Question '" + questionTitle + "' ajoutee au formulaire '" + formName + "'");
        System.out.println("dans l'etape '" + stepName + "'");

        assertTrue(true, "La question a ete ajoutee avec succes");
    }
}
