package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormWorkflowQuestionsDataSet;
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
 * Brique macro : declarer les questions rouvertes a l'usager en correction et en complement.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.questions}. Ecrit : rien dans le contexte.</p>
 *
 * <p>Les demandes de correction et de complement ne rouvrent pas tout le formulaire : l'onglet
 * Workflow du formulaire designe, question par question, celles que chacune des deux demandes
 * remet a la main de l'usager. Tant que rien n'y est declare, l'ecran d'execution de l'action ne
 * propose aucune question a selectionner et la demande part sans objet.</p>
 */
@Epic("Forms")
@Feature("Configuration workflow du formulaire")
@Story("Declarer les questions rouvertes en correction et en complement")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ConfigureFormWorkflowQuestionsMacroTest extends MacroTest {

    /** Prefixe des cases declarant une question rouverte en correction. */
    private static final String CORRECTION = "used_in_correct_form_response_";

    /** Prefixe des cases declarant une question rouverte en complement. */
    private static final String COMPLEMENT = "used_in_complete_form_response_";

    @Step("Declarer les questions rouvertes en correction et en complement")
    public static void run(FormsContext ctx, FormWorkflowQuestionsDataSet data) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister avant de declarer ses questions de correction et de complement");

        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageFormWorkflowConfig.jsp?view=manageWorkflow&id_form=" + ctx.formId);

        for (String titre : data.pourCorrection()) {
            cocher(ctx, CORRECTION, titre);
        }
        for (String titre : data.pourComplement()) {
            cocher(ctx, COMPLEMENT, titre);
        }

        page.locator("button[name='action_modifyWorkflowConfig'], input[name='action_modifyWorkflowConfig']")
            .first().click();
        page.waitForLoadState();

        verifier(ctx, data);
    }

    /**
     * Coche la case declarant une question pour l'une des deux demandes.
     *
     * @param ctx     contexte formulaire courant
     * @param prefixe prefixe du nom de la case
     * @param titre   titre de la question concernee
     */
    private static void cocher(FormsContext ctx, String prefixe, String titre) {
        int id = idQuestion(ctx, titre);
        Locator cases = ctx.page.locator("input[type='checkbox'][name='" + prefixe + id + "']");
        Assertions.assertTrue(cases.count() > 0,
            "La question '" + titre + "' n'est pas proposee dans la configuration workflow du "
                + "formulaire : elle n'appartient pas a ce formulaire");
        cases.first().check();
    }

    /**
     * Relit l'ecran et controle que les declarations ont ete enregistrees.
     *
     * @param ctx  contexte formulaire courant
     * @param data questions declarees
     */
    private static void verifier(FormsContext ctx, FormWorkflowQuestionsDataSet data) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageFormWorkflowConfig.jsp?view=manageWorkflow&id_form=" + ctx.formId);

        for (String titre : data.pourCorrection()) {
            exigerCochee(ctx, CORRECTION, titre, "correction");
        }
        for (String titre : data.pourComplement()) {
            exigerCochee(ctx, COMPLEMENT, titre, "complement");
        }
    }

    /**
     * Verifie qu'une declaration a bien ete conservee.
     *
     * @param ctx     contexte formulaire courant
     * @param prefixe prefixe du nom de la case
     * @param titre   titre de la question
     * @param demande libelle de la demande concernee, pour le diagnostic
     */
    private static void exigerCochee(FormsContext ctx, String prefixe, String titre, String demande) {
        int id = idQuestion(ctx, titre);
        boolean cochee = ctx.page.locator("input[type='checkbox'][name='" + prefixe + id + "']")
            .first().isChecked();
        Assertions.assertTrue(cochee,
            "La question '" + titre + "' devrait rester declaree pour la demande de " + demande
                + " : sans elle, l'action ne proposera rien a l'usager");
    }

    /**
     * Identifiant d'une question du contexte, par son titre.
     *
     * @param ctx   contexte formulaire courant
     * @param titre titre de la question
     * @return son identifiant
     */
    private static int idQuestion(FormsContext ctx, String titre) {
        return ctx.questions.stream()
            .filter(q -> titre.equals(q.title))
            .map(q -> q.id)
            .filter(id -> id > 0)
            .reduce((premier, dernier) -> dernier)
            .orElseThrow(() -> new IllegalStateException(
                "Question '" + titre + "' absente du contexte ou sans identifiant capture"));
    }

    @Test
    @DisplayName("Declarer les questions de correction et de complement (auto-provisionnement)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom du demandeur"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXTAREA, "Objet de la demande"));
        run(ctx, FormWorkflowQuestionsDataSet.memesQuestions("Nom du demandeur", "Objet de la demande"));
    }
}
