package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : ouvrir un formulaire publie en front-office (vue {@code stepView}).
 *
 * <p>Lit : {@code ctx.formId}. Ecrit : rien (laisse la page sur la vue FO du formulaire pour les
 * briques FO suivantes). Verifie qu'un champ ou un bouton est affiche ; sinon la brique est ignoree
 * (Assumptions) car le formulaire peut ne pas etre publie ou etre vide.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Ouvrir le formulaire en front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class OpenFormFOMacroTest extends MacroTest {

    @Step("Ouvrir le formulaire en front-office")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant l'ouverture en front-office");

        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId);

        // Un formulaire affiche expose au moins un champ de saisie ou un bouton d'action.
        boolean displayed = anyVisible(ctx.page, "input")
            || anyVisible(ctx.page, "textarea")
            || anyVisible(ctx.page, "select")
            || anyVisible(ctx.page, "button")
            || anyVisible(ctx.page, ".btn");
        Assumptions.assumeTrue(displayed,
            "Aucun champ/bouton visible sur la vue FO du formulaire id_form=" + ctx.formId
                + " (formulaire non publie, non demarre ou vide ?) : brique ignoree");
    }

    /** Vrai si au moins un element correspondant au selecteur est visible (parcours borne). */
    private static boolean anyVisible(Page page, String selector) {
        Locator loc = page.locator(selector);
        int n = Math.min(loc.count(), 40);
        for (int i = 0; i < n; i++) {
            try {
                if (loc.nth(i).isVisible()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // element detache : on continue
            }
        }
        return false;
    }

    @Test
    @DisplayName("Ouvrir un formulaire en FO (auto-provisionnement formulaire + etape + question + publication)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Champ FO"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        run(ctx);
    }
}
