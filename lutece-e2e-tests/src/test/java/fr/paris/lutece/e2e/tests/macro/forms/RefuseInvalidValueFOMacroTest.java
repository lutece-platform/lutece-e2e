package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ControlDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.InvalidValueDataSet;
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
 * Brique macro FO (negative) : une saisie invalide pour un controle de validation est refusee sur l'etape courante.
 *
 * <p>Complement de {@code VerifyValidationErrorFO}, qui rouvre toujours le formulaire a sa premiere etape et se met en
 * skip quand le champ n'y est pas : inutilisable pour une question d'une etape suivante. Ici la question est cherchee
 * sur l'etape FO affichee ({@code #nIt0_attribute<id>}), la valeur saisie, l'etape soumise par son seul bouton visible
 * ({@code action_doSaveStep} ou {@code action_formResponseSummary}). Refus prouve par l'ecran (releve du 2026-11-12,
 * forms 4.0.2-SNAPSHOT) : meme {@code id_step}, champ {@code is-invalid} qui garde la valeur, message du controle dans
 * le {@code .invalid-feedback} du bloc de la question.</p>
 *
 * <p>Lit : {@code ctx.questions} (id resolu). Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Refus : saisie invalide pour un controle de validation")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("negative")
@Tag("nonreg")
public class RefuseInvalidValueFOMacroTest extends MacroTest {

    private static final String FO_FORM = "form#form-validate";

    @Step("Refus FO : saisie invalide « {data.value} » pour la question {data.questionIndex}")
    public static void run(FormsContext ctx, InvalidValueDataSet data) {
        Assertions.assertTrue(ctx.questions.size() > data.questionIndex(), "La question ciblee doit exister (ctx.questions)");
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu (VerifyQuestionsListed)");
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);

        Locator form = page.locator(FO_FORM);
        Assertions.assertEquals(1, form.count(), "Le formulaire FO #form-validate doit etre affiche (url " + page.url() + ")");
        String stepBefore = form.locator("input[name='id_step']").inputValue();
        Locator field = form.locator("#nIt0_attribute" + question.id);
        Assertions.assertEquals(1, field.count(), "La question '" + question.title + "' doit etre sur l'etape FO " + stepBefore);
        Assertions.assertTrue(field.isVisible(), "La question '" + question.title + "' doit etre affichee pour etre saisie");
        field.fill(data.value());
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
        Assertions.assertEquals(data.value(), refused.inputValue(), "Le champ refuse doit garder la saisie de l'usager");
        Locator feedback = page.locator(FO_FORM + " .display_field_" + question.id + "_0 .invalid-feedback");
        Assertions.assertTrue(feedback.allInnerTexts().stream().anyMatch(t -> t.trim().equals(data.errorMessage())),
            "Message « " + data.errorMessage() + " » attendu sous la question '" + question.title + "' : " + feedback.allInnerTexts());
        Assertions.assertTrue(feedback.first().isVisible(), "Le message d'erreur doit etre visible");
    }

    @Test
    @DisplayName("Refus FO : adresse invalide pour un controle par expression reguliere « Email »")
    void standaloneRefuseInvalidValueFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Courriel"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddValidationControlMacroTest.run(ctx, ControlDataSet.validation(0, "Email", "Adresse invalide"));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            run(ctx, InvalidValueDataSet.of(0, "@@@", "Adresse invalide"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
