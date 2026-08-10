package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import fr.paris.lutece.e2e.pages.bo.FormsEditPage;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionDataSet;
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
 * Brique macro : passer a l'etape suivante en front-office ("Etape suivante").
 *
 * <p>Lit : {@code ctx.formId} et la page FO courante. Ecrit : rien. Le bouton "Etape suivante"
 * n'existe que sur un formulaire multi-etapes non final : s'il est absent (formulaire mono-etape ou
 * etape finale), la brique est ignoree (Assumptions). Sinon on clique (POST) et on verifie au mieux
 * que la page a change.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Passer a l'etape suivante en front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class NextStepFOMacroTest extends MacroTest {

    @Step("Passer a l'etape suivante en front-office")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant de passer a l'etape suivante en front-office");

        Page page = ctx.page;
        page.waitForLoadState();

        Locator next = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Etape suivante"));
        boolean present = next.count() > 0 && next.first().isVisible();
        Assumptions.assumeTrue(present,
            "Bouton 'Etape suivante' absent (formulaire mono-etape ou etape finale) : brique ignoree");

        String before = signature(page);
        next.first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        String after = signature(page);

        Assertions.assertNotEquals(before, after,
            "La page devrait avoir change apres 'Etape suivante' (avancement attendu ; "
                + "un blocage de validation laisse la page inchangee)");
    }

    /** Signature best-effort de l'etat de page : URL + 1er titre visible + comptes de boutons de flux. */
    private static String signature(Page page) {
        String url = page.url();
        String heading = firstText(page, "h1, h2, h3, legend");
        int nextCount = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Etape suivante")).count();
        int summaryCount = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Voir le récapitulatif")).count();
        int validateCount = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Valider le récapitulatif")).count();
        return url + "|" + heading + "|next=" + nextCount + "|summary=" + summaryCount + "|validate=" + validateCount;
    }

    private static String firstText(Page page, String selector) {
        Locator loc = page.locator(selector);
        int n = Math.min(loc.count(), 10);
        for (int i = 0; i < n; i++) {
            Locator c = loc.nth(i);
            try {
                if (c.isVisible()) {
                    String t = c.textContent();
                    if (t != null && !t.isBlank()) {
                        return t.trim();
                    }
                }
            } catch (RuntimeException ignored) {
                // element detache : on continue
            }
        }
        return "";
    }

    @Test
    @DisplayName("Etape suivante en FO (auto-provisionnement multi-etapes + ouverture FO)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        // Etape 1 : premiere etape (forcee initiale + finale a la creation cote serveur), destinee a
        // etre la page de depart du front-office.
        CreateStepMacroTest.run(ctx, StepDataSet.of("Etape 1"));
        // Etape 2 finale : sa presence permet de rendre l'etape 1 non finale et sert de cible.
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Fin"));
        // Rendre l'etape 1 non finale : sinon le FO n'affiche pas le bouton "Etape suivante".
        FormsContext.StepRef first = ctx.steps.get(0);
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + first.id);
        new FormsEditPage(ctx.page, ctx.baseUrl).uncheckFinalAndSave();
        first.isFinal = false;
        // Une question sur l'etape 1 et une transition vers l'etape 2 rendent le flux multi-etapes
        // complet (bouton "Etape suivante" present et menant a l'etape suivante).
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Champ FO"));
        CreateTransitionMacroTest.run(ctx, TransitionDataSet.defaults());
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);
        run(ctx);
    }
}
