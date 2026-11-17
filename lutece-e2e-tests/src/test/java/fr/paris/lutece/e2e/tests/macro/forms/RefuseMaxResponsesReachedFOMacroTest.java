package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.MaxResponsesDataSet;
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

import java.util.List;

/**
 * Brique negative FO : un formulaire dont le nombre maximum de reponses est atteint refuse une nouvelle saisie.
 *
 * <p>Condition reelle ({@code FormXPage}, {@code FormsResponseUtils.checkNumberMaxResponseForm}) : nombre de reponses
 * enregistrees superieur ou egal a {@code maxNumberResponse} (0 = pas de limite) => {@code SiteMessage} d'erreur
 * {@code forms.error.form.MaxResponse}. Ce controle passe avant celui de l'etat actif : un formulaire depublie dont le
 * maximum est atteint affiche ce message, pas « Formulaire indisponible ». Chemin reel : l'URL FO du formulaire
 * ({@code page=forms&view=stepView&id_form=F&init=true}). Effet prouve : page {@code SiteMessage.jsp}, aucun formulaire de
 * saisie {@code #form-validate}, message exact dans {@code .alert-content} (releve du 2026-11-09).</p>
 *
 * <p>Lit : {@code ctx.formId}. Prealable : maximum regle ({@code SetMaxResponses}) et atteint (reponses enregistrees).</p>
 */
@Epic("Forms")
@Feature("Refus")
@Story("Formulaire au nombre maximum de reponses")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class RefuseMaxResponsesReachedFOMacroTest extends MacroTest {

    /** {@code forms.error.form.MaxResponse} (forms_messages_fr.properties). */
    static final String EXPECTED_MESSAGE = "Formulaire indisponible car le nombre de réponses est atteint";

    @Step("Verifier qu'une nouvelle saisie FO est refusee, nombre maximum de reponses atteint")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId + "&init=true");

        Assertions.assertEquals(0, ctx.page.locator("form#form-validate").count(),
            "Le formulaire " + ctx.formId + " ne doit plus proposer de saisie en FO");
        Assertions.assertTrue(ctx.page.url().contains("SiteMessage.jsp"),
            "Le FO doit repondre par un message de site, url : " + ctx.page.url());
        List<String> messages = ctx.page.locator(".alert-content").allInnerTexts().stream().map(String::trim).toList();
        Assertions.assertTrue(messages.contains(EXPECTED_MESSAGE),
            "Message « " + EXPECTED_MESSAGE + " » attendu : " + messages);
    }

    @Test
    @DisplayName("FO : formulaire limite a une reponse, refuse apres la premiere")
    void standaloneRefuseMaxResponsesReachedFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            SetMaxResponsesMacroTest.run(ctx, MaxResponsesDataSet.of(1));
            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont"));
            GoToSummaryFOMacroTest.run(ctx);
            ValidateSummaryFOMacroTest.run(ctx);
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
