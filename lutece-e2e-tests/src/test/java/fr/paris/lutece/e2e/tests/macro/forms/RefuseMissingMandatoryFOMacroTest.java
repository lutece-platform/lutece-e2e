package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
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
 * Brique negative FO : valider l'etape courante en laissant vide une question obligatoire est refuse.
 *
 * <p>Sur l'etape FO affichee, la brique vide le champ de la question ciblee ({@code #nIt0_attribute<id>}) puis
 * clique le seul bouton de validation visible de l'etape ({@code action_doSaveStep} ou
 * {@code action_formResponseSummary}, hors doublon {@code aria-hidden}). Effet prouve : le formulaire FO est
 * reaffiche sur la meme etape ({@code id_step} inchange), le champ porte {@code is-invalid} et le message
 * « Ce champ est obligatoire. » ({@code forms.message.mandatory.entry}) est affiche.</p>
 *
 * <p>Lit : {@code ctx.questions} (id resolu). Prealable : la question est obligatoire ({@code SetQuestionMandatory})
 * et l'etape FO qui la porte est affichee ({@code OpenFormFO}).</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Refus : question obligatoire non renseignee")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("negative")
@Tag("nonreg")
public class RefuseMissingMandatoryFOMacroTest extends MacroTest {

    private static final String FO_FORM = "form#form-validate";
    static final String EXPECTED_ERROR = "Ce champ est obligatoire.";

    @Step("Refus FO : question obligatoire {data.questionIndex} laissee vide")
    public static void run(FormsContext ctx, QuestionTargetDataSet data) {
        Assertions.assertTrue(ctx.questions.size() > data.questionIndex(), "La question ciblee doit exister (ctx.questions)");
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu (VerifyQuestionsListed)");
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);

        Locator form = page.locator(FO_FORM);
        Assertions.assertEquals(1, form.count(), "Le formulaire FO #form-validate doit etre affiche (url " + page.url() + ")");
        String stepBefore = form.locator("input[name='id_step']").inputValue();
        Locator field = form.locator("#nIt0_attribute" + question.id);
        Assertions.assertEquals(1, field.count(), "La question '" + question.title + "' doit etre affichee sur l'etape FO " + stepBefore);
        field.fill("");
        Locator submit = form.locator("button[name='action_doSaveStep']:not([aria-hidden='true']), "
            + "button[name='action_formResponseSummary']:not([aria-hidden='true'])");
        Assertions.assertEquals(1, submit.count(), "L'etape FO " + stepBefore + " doit proposer un seul bouton de validation visible");
        submit.click();
        page.waitForLoadState();

        Assertions.assertEquals(1, page.locator(FO_FORM).count(), "Le formulaire FO doit etre reaffiche apres le refus (url " + page.url() + ")");
        Assertions.assertEquals(stepBefore, page.locator(FO_FORM + " input[name='id_step']").inputValue(),
            "Le refus doit laisser l'usager sur l'etape " + stepBefore);
        Locator refused = page.locator(FO_FORM + " #nIt0_attribute" + question.id);
        Assertions.assertTrue(String.valueOf(refused.getAttribute("class")).contains("is-invalid"),
            "Le champ '" + question.title + "' doit etre marque invalide : class=" + refused.getAttribute("class"));
        Assertions.assertTrue(page.locator(FO_FORM + " .invalid-feedback").allInnerTexts().stream()
                .anyMatch(t -> t.contains(EXPECTED_ERROR)),
            "Message « " + EXPECTED_ERROR + " » attendu : " + page.locator(FO_FORM + " .invalid-feedback").allInnerTexts());
    }

    @Test
    @DisplayName("Refus FO : passer au recapitulatif sans renseigner le nom obligatoire")
    void standaloneRefuseMissingMandatoryFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville"));
            VerifyQuestionsListedMacroTest.run(ctx);
            SetQuestionMandatoryMacroTest.run(ctx, QuestionTargetDataSet.of(0));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Ville", "Paris"));
            run(ctx, QuestionTargetDataSet.of(0));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
