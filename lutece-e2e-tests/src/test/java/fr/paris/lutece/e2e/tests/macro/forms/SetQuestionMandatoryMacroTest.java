package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionTargetDataSet;
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
 * Brique macro : rendre une question obligatoire.
 *
 * <p>Chemin reel : ecran {@code ManageQuestions.jsp?view=modifyQuestion&id_step=S&id_question=Q} (form
 * {@code #modifyEntryForm}), interrupteur {@code #mandatory}, bouton « Appliquer » ({@code action_saveQuestion}).
 * Effet prouve en rechargeant l'ecran : l'interrupteur reste coche. Prealable de {@code RefuseMissingMandatoryFO}.</p>
 *
 * <p>Lit : {@code ctx.questions} (id resolu, cf. {@code VerifyQuestionsListed}). Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Rendre une question obligatoire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class SetQuestionMandatoryMacroTest extends MacroTest {

    @Step("Rendre obligatoire la question {data.questionIndex}")
    public static void run(FormsContext ctx, QuestionTargetDataSet data) {
        Assertions.assertTrue(ctx.questions.size() > data.questionIndex(), "La question ciblee doit exister (ctx.questions)");
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu (VerifyQuestionsListed)");
        String url = MacroSupport.FORMS + "ManageQuestions.jsp?view=modifyQuestion&id_step=" + question.stepId
            + "&id_question=" + question.id;

        MacroSupport.navigate(ctx, url);
        Locator form = ctx.page.locator("form#modifyEntryForm");
        Assertions.assertEquals(1, form.count(), "L'ecran de modification de la question " + question.id + " doit etre affiche");
        form.locator("#mandatory").check();
        form.locator("button[name='action_saveQuestion']").click();
        ctx.page.waitForLoadState();

        MacroSupport.navigate(ctx, url);
        Assertions.assertTrue(ctx.page.locator("form#modifyEntryForm #mandatory").isChecked(),
            "La question '" + question.title + "' doit etre enregistree obligatoire");
    }

    @Test
    @DisplayName("Rendre obligatoire une question texte")
    void standaloneSetQuestionMandatory() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            VerifyQuestionsListedMacroTest.run(ctx);
            run(ctx, QuestionTargetDataSet.of(0));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
