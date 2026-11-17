package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.DisplayConditionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionShownDataSet;
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
 * Brique de verification FO : une question de l'etape courante est affichee, ou rendue mais masquee.
 *
 * <p>Le bloc de la question ({@code .display_field_<id question>_0}) doit exister sur l'etape affichee : une question
 * absente de la page ne passe jamais pour « masquee ». L'etat est attendu jusqu'a 5 s (le masquage d'une condition
 * d'affichage est applique par le script de la page). Complement de {@code VerifyConditionalRevealFO}, qui exige la
 * pilote sur la meme etape : ici la pilote peut avoir ete repondue a une etape precedente (scenario LUT-34462).</p>
 *
 * <p>Prealable : formulaire ouvert en FO. Lit : {@code ctx.questions} (ids resolus). Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Verifier l'affichage d'une question")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyQuestionShownFOMacroTest extends MacroTest {

    private static final String FO_FORM = "form#form-validate";
    private static final double WAIT_MS = 5000;

    @Step("Verifier l'affichage de la question {data.questionIndex} en FO (affichee : {data.shown})")
    public static void run(FormsContext ctx, QuestionShownDataSet data) {
        Assertions.assertTrue(ctx.questions.size() > data.questionIndex(), "La question doit exister (ctx.questions)");
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu (VerifyQuestionsListed)");
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);
        Locator form = page.locator(FO_FORM);
        Assertions.assertEquals(1, form.count(), "Le formulaire FO #form-validate doit etre affiche (url " + page.url() + ")");
        Assertions.assertEquals(String.valueOf(question.stepId), form.locator("input[name='id_step']").inputValue(),
            "L'etape FO affichee doit etre celle de la question '" + question.title + "'");

        Locator block = page.locator(FO_FORM + " .display_field_" + question.id + "_0");
        Assertions.assertEquals(1, block.count(), "La question '" + question.title + "' doit etre rendue sur l'etape FO");
        try {
            block.waitFor(new Locator.WaitForOptions()
                .setState(data.shown() ? WaitForSelectorState.VISIBLE : WaitForSelectorState.HIDDEN).setTimeout(WAIT_MS));
        } catch (PlaywrightException e) {
            Assertions.fail("La question '" + question.title + "' doit etre " + (data.shown() ? "affichee" : "masquee")
                + " (style=" + block.getAttribute("style") + ")");
        }
    }

    @Test
    @DisplayName("FO : « Precisions » est masquee tant que la pilote n'a pas la valeur qui l'affiche")
    void standaloneVerifyQuestionShownFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Avez-vous un animal ?"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Precisions"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Oui", "oui"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Non", "non"));
            AddDisplayConditionMacroTest.run(ctx, DisplayConditionDataSet.of(0, 1, "Oui", "Non"));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            run(ctx, QuestionShownDataSet.shown(0));
            run(ctx, QuestionShownDataSet.hidden(1));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
