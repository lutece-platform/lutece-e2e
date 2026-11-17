package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ChoiceDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
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
 * Brique macro FO : cocher une reponse d'une question a choix (bouton radio, case a cocher) de l'etape courante.
 *
 * <p>{@code FillFieldFO} ne pilote que les champs de saisie (texte, nombre, date). Ici la valeur est cochee par son
 * libelle exact dans le bloc de la question ({@code .display_field_<id question>_0}, une entree
 * {@code nIt0_attribute<id>} par valeur, releve du 2026-11-06), comme l'usager. Effet prouve : l'entree est cochee.</p>
 *
 * <p>Prealable : formulaire ouvert en FO sur l'etape de la question. Lit : {@code ctx.questions} (ids resolus). Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Cocher une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ChooseValueFOMacroTest extends MacroTest {

    private static final String FO_FORM = "form#form-validate";

    @Step("Cocher '{data.label}' a la question {data.questionIndex} en FO")
    public static void run(FormsContext ctx, ChoiceDataSet data) {
        Assertions.assertTrue(ctx.questions.size() > data.questionIndex(), "La question doit exister (ctx.questions)");
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu (VerifyQuestionsListed)");
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);
        Assertions.assertEquals(1, page.locator(FO_FORM).count(), "Le formulaire FO #form-validate doit etre affiche (url " + page.url() + ")");

        Locator block = page.locator(FO_FORM + " .display_field_" + question.id + "_0");
        Assertions.assertEquals(1, block.count(), "La question '" + question.title + "' doit etre sur l'etape FO affichee");
        Locator choice = block.getByLabel(data.label(), new Locator.GetByLabelOptions().setExact(true));
        Assertions.assertEquals(1, choice.count(), "La valeur '" + data.label() + "' doit etre proposee par '" + question.title + "'");
        choice.check();
        Assertions.assertTrue(choice.isChecked(), "La valeur '" + data.label() + "' doit etre cochee");
    }

    @Test
    @DisplayName("Cocher « Autre » a « Votre statut ? » en FO")
    void standaloneChooseValueFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Statut"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Votre statut ?"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Fonctionnaire", "fonctionnaire"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Autre", "autre"));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            run(ctx, ChoiceDataSet.of(0, "Autre"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
