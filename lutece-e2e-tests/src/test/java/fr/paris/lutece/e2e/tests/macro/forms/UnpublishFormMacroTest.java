package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
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
 * Brique macro : depublier un formulaire (dates de disponibilite videes).
 *
 * <p>Chemin reel : le bouton de publication de la liste ouvre en iframe
 * {@code ManageForms.jsp?view=modifyPublication&id_form=F} ; ses deux dates sont des champs flatpickr
 * ({@code #availabilityStartDate}, {@code #availabilityEndDate}) vides par {@code clear()}, puis
 * {@code action_modifyFormPublication} (depuis LUT-34237 cette action ne met a jour que les dates ; avant,
 * {@code action_modifyForm}). Sans date, {@code Form.isActive()} est faux.</p>
 *
 * <p>Effet prouve deux fois : relu cote BO (les deux dates de l'ecran de publication sont vides, le titre du
 * formulaire est inchange) et cote FO (l'ouverture du formulaire aboutit au message de site, sans formulaire
 * de saisie {@code #form-validate}).</p>
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.formTitle}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Depublier le formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class UnpublishFormMacroTest extends MacroTest {

    private static final String SUBMIT = "button[name='action_modifyFormPublication']";
    private static final String DATES = "['availabilityStartDate', 'availabilityEndDate']";

    @Step("Depublier le formulaire (dates de disponibilite videes)")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        Page page = ctx.page;

        openPublication(ctx);
        page.evaluate("() => " + DATES + ".forEach(i => { const e = document.getElementById(i);"
            + " if (e && e._flatpickr) { e._flatpickr.clear(); } })");
        page.locator(SUBMIT).click();
        page.waitForLoadState();

        openPublication(ctx);
        Assertions.assertEquals("", page.locator("input[name='availabilityStartDate']").inputValue(),
            "La date de debut de disponibilite du formulaire " + ctx.formId + " doit etre vide");
        Assertions.assertEquals("", page.locator("input[name='availabilityEndDate']").inputValue(),
            "La date de fin de disponibilite du formulaire " + ctx.formId + " doit etre vide");
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId);
        Assertions.assertEquals(ctx.formTitle, ctx.page.locator("form#modify_form #title").inputValue(),
            "La depublication ne doit pas modifier le titre du formulaire");

        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId + "&init=true");
        Assertions.assertEquals(0, page.locator("form#form-validate").count(),
            "Le formulaire depublie ne doit plus proposer de saisie en FO");
        Assertions.assertTrue(page.url().contains("SiteMessage.jsp"),
            "Le FO doit repondre par un message de site (formulaire indisponible), url : " + page.url());
    }

    private static void openPublication(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyPublication&id_form=" + ctx.formId);
        Locator form = ctx.page.locator("form:has(input[name='id_form'][value='" + ctx.formId + "'])"
            + ":has(" + SUBMIT + ")");
        Assertions.assertEquals(1, form.count(),
            "L'ecran de publication du formulaire " + ctx.formId + " doit etre affiche");
    }

    @Test
    @DisplayName("Depublier un formulaire publie : dates videes et formulaire indisponible en FO")
    void standaloneUnpublishForm() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
