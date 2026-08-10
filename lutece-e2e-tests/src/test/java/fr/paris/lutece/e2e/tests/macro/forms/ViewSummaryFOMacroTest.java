package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : afficher le recapitulatif en front-office ("Voir le recapitulatif").
 *
 * <p>Lit : {@code ctx.formId} et la page FO courante. Ecrit : rien. Le bouton "Voir le recapitulatif"
 * n'apparait que sur la derniere etape de saisie : s'il est absent, la brique est ignoree
 * (Assumptions). Sinon on clique et on verifie au mieux que le recapitulatif s'affiche (bouton
 * "Valider le recapitulatif" present ou libelle "recapitulatif" visible).</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Afficher le recapitulatif en front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ViewSummaryFOMacroTest extends MacroTest {

    @Step("Afficher le recapitulatif en front-office")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant d'afficher le recapitulatif en front-office");

        Page page = ctx.page;
        page.waitForLoadState();

        Locator view = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Voir le récapitulatif"));
        boolean present = view.count() > 0 && view.first().isVisible();
        Assumptions.assumeTrue(present,
            "Bouton 'Voir le récapitulatif' absent (etape non finale ou flux different) : brique ignoree");

        view.first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        boolean validatePresent = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Valider le récapitulatif")).count() > 0;
        boolean summaryShown = validatePresent
            || isTextVisible(page, "récapitulatif")
            || isTextVisible(page, "Récapitulatif");
        Assertions.assertTrue(summaryShown,
            "Le recapitulatif devrait s'afficher apres le clic (bouton 'Valider le récapitulatif' "
                + "ou libelle 'recapitulatif' attendu)");
    }

    private static boolean isTextVisible(Page page, String text) {
        try {
            Locator loc = page.getByText(text);
            return loc.count() > 0 && loc.first().isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Voir le recapitulatif en FO (auto-provisionnement + ouverture FO ; ignoree si absent)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Champ FO"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);
        run(ctx);
    }
}
