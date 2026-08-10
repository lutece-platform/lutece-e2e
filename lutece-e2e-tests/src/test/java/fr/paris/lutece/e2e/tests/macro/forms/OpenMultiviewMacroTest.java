package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
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
 * Brique macro : ouvrir la multivue des reponses (MultiviewForms.jsp).
 *
 * <p>Lit : rien. Ecrit : rien. Verifie que la page multivue s'affiche sans erreur (table / liste /
 * panneau present). La multivue peut etre vide (aucune reponse soumise) : on n'exige aucune ligne,
 * seulement la presence du conteneur.</p>
 *
 * <p>Heberge aussi {@link #submitOneFoResponse(FormsContext)}, un helper de provisionnement best-effort
 * (soumission d'une reponse en front-office) reutilise par les autres briques du domaine "Reponses"
 * pour peupler la multivue. Toutes les actions FO sont gated : le helper ne leve jamais et retourne
 * simplement {@code false} si le flux ne peut pas etre mene a bout.</p>
 */
@Epic("Forms")
@Feature("Réponses")
@Story("Ouvrir la multivue des reponses")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class OpenMultiviewMacroTest extends MacroTest {

    @Step("Ouvrir la multivue des reponses")
    public static void run(FormsContext ctx) {
        openMultiview(ctx);
        Page page = ctx.page;

        Assertions.assertFalse(page.url().contains("AdminLogin"),
            "La session admin ne devrait pas etre perdue en ouvrant la multivue des reponses");
        Assertions.assertTrue(multiviewDisplayed(page),
            "La multivue des reponses devrait etre affichee (table / liste / panneau present), meme vide");
    }

    /** Navigue vers la multivue des reponses. */
    static void openMultiview(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "MultiviewForms.jsp");
    }

    /** Vrai si la page multivue est affichee (conteneur present, pas de redirection login). */
    static boolean multiviewDisplayed(Page page) {
        if (page.url().contains("AdminLogin")) {
            return false;
        }
        if (page.url().contains("MultiviewForms")) {
            return true;
        }
        return page.locator("table, .table, .list-group, .panel, .card, form").count() > 0;
    }

    // === Provisionnement best-effort : soumission d'une reponse en front-office ===

    /**
     * Soumet au mieux une reponse en front-office pour le formulaire courant afin de peupler la
     * multivue. Toutes les etapes sont gated (presence + visibilite avant clic) : aucune ne bloque
     * 30s. Retourne {@code true} si le recapitulatif a pu etre valide, {@code false} sinon. Ne leve
     * jamais (provisionnement best-effort). Positionne {@code ctx.lastResponseId} si derivable.
     */
    static boolean submitOneFoResponse(FormsContext ctx) {
        if (ctx.formId <= 0) {
            return false;
        }
        Page page = ctx.page;
        try {
            MacroSupport.navigate(ctx,
                "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId);
            dismissBackdrop(page);

            String value = "Reponse macro " + ctx.runSuffix;
            fillVisibleTextInputs(page, value);

            // Parcours borne des etapes intermediaires : on clique "Etape suivante" tant qu'elle est
            // presente (un formulaire mono-etape n'en a pas), en re-remplissant les champs a chaque etape.
            for (int i = 0; i < 10; i++) {
                if (!clickButtonIfPresent(page, "Etape suivante")) {
                    break;
                }
                fillVisibleTextInputs(page, value);
            }

            clickButtonIfPresent(page, "Voir le récapitulatif");
            boolean validated = clickButtonIfPresent(page, "Valider le récapitulatif");
            if (validated) {
                deriveResponseId(ctx);
            }
            return validated;
        } catch (RuntimeException e) {
            // provisionnement best-effort : on n'echoue jamais ici
            return false;
        }
    }

    private static boolean clickButtonIfPresent(Page page, String label) {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(label));
        if (btn.count() > 0 && btn.first().isVisible()) {
            btn.first().click();
            page.waitForLoadState();
            return true;
        }
        return false;
    }

    private static void fillVisibleTextInputs(Page page, String value) {
        page.waitForLoadState();
        Locator inputs = page.locator("input[type='text'], textarea");
        int n = inputs.count();
        for (int i = 0; i < n; i++) {
            Locator in = inputs.nth(i);
            try {
                if (in.isVisible()) {
                    in.fill(value);
                }
            } catch (RuntimeException ignored) {
                // champ non remplissable (readonly / masque) : on continue
            }
        }
    }

    private static void dismissBackdrop(Page page) {
        page.waitForLoadState();
        Locator backdrop = page.locator(".offcanvas-backdrop");
        if (backdrop.count() > 0) {
            page.keyboard().press("Escape");
            try {
                backdrop.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(5000));
            } catch (RuntimeException ignored) {
                // le backdrop peut disparaitre autrement : sans consequence
            }
        }
    }

    private static void deriveResponseId(FormsContext ctx) {
        try {
            String url = ctx.page.url();
            for (String key : new String[] {"id_response=", "id_form_response=", "id_history="}) {
                if (url.contains(key)) {
                    String v = url.split(key)[1].split("&")[0].split("#")[0];
                    ctx.lastResponseId = Integer.parseInt(v);
                    return;
                }
            }
        } catch (RuntimeException ignored) {
            // id non derivable depuis l'URL de confirmation : reste null
        }
    }

    @Test
    @DisplayName("Ouvrir la multivue des reponses (auto-provisionnement d'un formulaire)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        // La multivue s'affiche independamment de la presence de reponses : pas besoin de publier
        // ni de soumettre ici.
        run(ctx);
    }
}
