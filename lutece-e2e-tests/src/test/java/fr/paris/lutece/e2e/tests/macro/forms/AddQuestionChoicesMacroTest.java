package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionChoicesDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : ajouter des choix a une question de type liste.
 *
 * <p>Lit : {@code ctx.questions}. Ecrit : rien dans le contexte.</p>
 *
 * <p>Creer une question a liste ne cree aucun choix : elle s'affiche vide en front office et ne
 * peut etre ni renseignee, ni utilisee comme condition d'embranchement. Les choix vivent sur un
 * ecran distinct, charge dans un panneau par une iframe depuis la page de la question ; on adresse
 * directement l'URL de cette iframe, ce qui evite de dependre de l'ouverture du panneau.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Ajouter des choix a une question de type liste")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddQuestionChoicesMacroTest extends MacroTest {

    @Step("Ajouter les choix d'une question de type liste")
    public static void run(FormsContext ctx, QuestionChoicesDataSet data) {
        FormsContext.QuestionRef question = ctx.questions.stream()
            .filter(q -> data.questionTitle().equals(q.title))
            .reduce((premier, dernier) -> dernier)
            .orElse(null);
        Assertions.assertNotNull(question,
            "La question '" + data.questionTitle() + "' doit avoir ete creee avant d'y ajouter des choix");
        Assertions.assertTrue(question.id > 0,
            "L'identifiant de la question '" + data.questionTitle() + "' n'a pas ete capture : "
                + "les choix ne peuvent pas lui etre rattaches");

        for (String choix : data.choices()) {
            ajouterChoix(ctx, question, choix);
        }

        verifierChoix(ctx, question, data);
    }

    /**
     * Cree un choix sur la question visee.
     *
     * @param ctx      contexte formulaire courant
     * @param question question a completer
     * @param libelle  libelle du choix
     */
    private static void ajouterChoix(FormsContext ctx, FormsContext.QuestionRef question, String libelle) {
        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ModifyEntry.jsp?option_no_display_title=true&view_createField=&id_question=" + question.id);

        page.locator("input[name='title']").first().fill(libelle);
        com.microsoft.playwright.Locator valeur = page.locator("input[name='value']");
        if (valeur.count() > 0) {
            valeur.first().fill(code(libelle));
        }
        page.locator("button[name='action_createField'], input[name='action_createField']").first().click();
        page.waitForLoadState();

        // Un choix refuse n'echoue pas sur place : Lutece affiche une page de message et le choix
        // n'existe tout simplement pas. Sans ce controle, l'absence ne se revelerait qu'au moment
        // ou une condition de transition ou le front office cherche en vain la reponse attendue.
        Assertions.assertFalse(page.url().contains("AdminMessage.jsp"),
            "La creation du choix '" + libelle + "' a ete refusee : "
                + page.locator("body").innerText().replaceAll("\\s+", " ").trim());
    }

    /**
     * Code technique derive d'un libelle de choix.
     *
     * <p>Lutece refuse les espaces et les caracteres speciaux dans la valeur d'un choix, mais pas
     * dans son titre : un libelle en plusieurs mots passe donc le titre et fait rejeter la valeur,
     * sans que le libelle lui-meme soit en cause.</p>
     *
     * @param libelle libelle du choix
     * @return un code sans accent, sans espace ni caractere special
     */
    private static String code(String libelle) {
        String sansAccent = java.text.Normalizer.normalize(libelle, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return sansAccent.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /**
     * Verifie que les choix demandes figurent bien sur la question.
     *
     * @param ctx      contexte formulaire courant
     * @param question question completee
     * @param data     choix demandes
     */
    private static void verifierChoix(FormsContext ctx, FormsContext.QuestionRef question,
        QuestionChoicesDataSet data) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=modifyQuestion&id_step=" + question.stepId
            + "&id_question=" + question.id);
        String contenu = ctx.page.locator("body").textContent().replaceAll("\\s+", " ");
        for (String choix : data.choices()) {
            Assertions.assertTrue(contenu.contains(choix),
                "Le choix '" + choix + "' devrait figurer sur la question '" + data.questionTitle()
                    + "' apres enregistrement");
        }

        // L'ecran de modification d'une question laisse le gestionnaire cote serveur positionne sur
        // cette question. Les traitements qui suivent — publication notamment — s'appuient sur ce
        // meme gestionnaire : on revient donc sur la liste des questions, qui est l'etat neutre.
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=manageQuestions&id_step=" + question.stepId);
    }

    @Test
    @DisplayName("Ajouter des choix a une question de type bouton radio (auto-provisionnement)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Nature de la demande"));
        run(ctx, QuestionChoicesDataSet.of("Nature de la demande", "Subvention", "Information"));
    }
}
