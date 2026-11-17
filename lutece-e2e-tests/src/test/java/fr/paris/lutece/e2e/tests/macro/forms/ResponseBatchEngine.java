package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.data.BatchSubmissionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.TestAbortedException;

import java.util.List;

/**
 * Moteur des briques de soumission en lot ({@code SubmitResponsesFOBatch}, {@code SubmitResponsesBOBatch}).
 * Pas une brique. FO ({@code Portal.jsp?page=forms}) et saisie BO ({@code ManageFormResponse.jsp}) partagent le
 * formulaire {@code #form-validate}, les actions {@code doSaveStep}, {@code formResponseSummary},
 * {@code doSaveResponseSummary} et le champ {@code id_step} : un seul parcours, assertions strictes a chaque pas
 * (aucun skip : une soumission qui ne peut pas aboutir fait echouer le lot avec son numero).
 */
final class ResponseBatchEngine {

    private ResponseBatchEngine() {}

    /** Point d'entree d'une soumission : url de l'ecran de saisie, relative au contexte. */
    static void run(FormsContext ctx, BatchSubmissionDataSet data, String entryUrl, String formAction) {
        Assertions.assertTrue(data.count() > 0, "Le lot doit compter au moins une soumission");
        Assertions.assertEquals(ctx.steps.size(), data.stepsFields().size(),
            "Le dataset doit decrire les champs de chaque etape du formulaire (" + ctx.steps.size() + ")");
        String form = "form#form-validate[action*='" + formAction + "']";
        for (int i = 1; i <= data.count(); i++) {
            String where = "soumission " + i + "/" + data.count();
            MacroSupport.navigate(ctx, entryUrl);
            for (int s = 0; s < ctx.steps.size(); s++) {
                assertStep(ctx, form, ctx.steps.get(s).id, where);
                for (FieldValueDataSet field : data.stepsFields().get(s)) {
                    fill(ctx, BatchSubmissionDataSet.forSubmission(field, i), where);
                }
                if (s < ctx.steps.size() - 1) {
                    click(ctx, form + " button[name='action_doSaveStep']", where + ", etape suivante");
                }
            }
            click(ctx, form + " button[name='action_formResponseSummary']", where + ", recapitulatif");
            click(ctx, "form button[name='action_doSaveResponseSummary']", where + ", validation");
        }
    }

    private static void assertStep(FormsContext ctx, String form, int stepId, String where) {
        Locator f = ctx.page.locator(form + ":has(input[name='id_form'][value='" + ctx.formId + "'])");
        Assertions.assertEquals(1, f.count(), where + " : formulaire de saisie attendu (url " + ctx.page.url()
            + ", alertes : " + ctx.page.locator(".alert").allInnerTexts() + ")");
        Assertions.assertEquals(String.valueOf(stepId), f.locator("input[name='id_step']").inputValue(), where + " : etape affichee");
    }

    private static void fill(FormsContext ctx, FieldValueDataSet field, String where) {
        try {
            FillFieldFOMacroTest.run(ctx, field);
        } catch (TestAbortedException e) {
            Assertions.fail(where + " : champ '" + field.label() + "' introuvable (" + e.getMessage() + ")");
        }
    }

    private static void click(FormsContext ctx, String selector, String where) {
        List<Locator> visible = ctx.page.locator(selector + ":not([aria-hidden='true'])").all();
        Assertions.assertEquals(1, visible.size(), where + " : un bouton attendu (" + selector + ")");
        visible.get(0).click();
        ctx.page.waitForLoadState();
    }
}
