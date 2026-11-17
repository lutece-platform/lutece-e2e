package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
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
 * Brique negative FO : un formulaire hors de ses dates de disponibilite refuse la saisie.
 *
 * <p>Condition reelle ({@code FormXPage}) : formulaire inactif => {@code SiteMessage} d'erreur
 * {@code forms.error.form.inactive} (« Formulaire indisponible »). Chemin reel : l'URL FO du formulaire
 * ({@code page=forms&view=stepView&id_form=F&init=true}). Effet prouve : page {@code SiteMessage.jsp} portant le
 * message, aucun formulaire de saisie {@code #form-validate}. Le message est attendu tel quel, sans la suite
 * « car le nombre de reponses est atteint » de {@code forms.error.form.MaxResponse}, qui commence par les memes mots
 * (revue du 2026-10-30, constat 13).</p>
 *
 * <p>Lit : {@code ctx.formId}. Prealable : formulaire depublie ({@code UnpublishForm}) ou jamais publie.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Refus : formulaire indisponible")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("negative")
@Tag("nonreg")
public class RefuseUnavailableFormFOMacroTest extends MacroTest {

    static final String EXPECTED_MESSAGE = "Formulaire indisponible";
    /** {@code forms.error.form.MaxResponse} (forms_messages_fr.properties). */
    static final String MAX_RESPONSE_MESSAGE = "car le nombre de r\u00e9ponses est atteint";

    @Step("Refus FO : formulaire indisponible")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId + "&init=true");

        Assertions.assertEquals(0, ctx.page.locator("form#form-validate").count(),
            "Le formulaire " + ctx.formId + " ne doit pas proposer de saisie en FO");
        Assertions.assertTrue(ctx.page.url().contains("SiteMessage.jsp"),
            "Le FO doit repondre par un message de site, url : " + ctx.page.url());
        String body = ctx.page.locator("body").innerText();
        Assertions.assertTrue(ctx.page.getByText(EXPECTED_MESSAGE, new Page.GetByTextOptions().setExact(true)).count() >= 1,
            "Message « " + EXPECTED_MESSAGE + " » attendu tel quel : " + body.trim());
        Assertions.assertFalse(body.contains(MAX_RESPONSE_MESSAGE),
            "Le refus doit tenir a l'indisponibilite, pas au nombre de reponses : " + body.trim());
    }

    @Test
    @DisplayName("Refus FO : formulaire publie puis depublie")
    void standaloneRefuseUnavailableFormFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            UnpublishFormMacroTest.run(ctx);
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
