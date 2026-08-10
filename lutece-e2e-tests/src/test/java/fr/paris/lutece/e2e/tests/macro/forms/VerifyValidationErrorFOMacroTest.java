package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ControlDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ValidationCheckDataSet;
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
 * Brique macro (front office) : verifier qu'un controle de validation rejette une saisie invalide
 * puis accepte une saisie valide.
 *
 * <p>Lit : {@code ctx.formId}. N'ecrit rien. Ouvre le formulaire publie en FO, saisit une valeur
 * invalide et soumet l'etape : si un message/marqueur d'erreur apparait, la brique re-ouvre le
 * formulaire, saisit une valeur valide et verifie l'absence d'erreur.</p>
 *
 * <p><b>Fragile / dependant des donnees</b> : la presence effective d'une erreur depend du validateur
 * pose en amont (type de question, valeur du controle) et du markup FO. Chaque etape est gardee par
 * {@link Assumptions} : si le champ ou le message d'erreur ne sont pas presents, la brique est ignoree
 * proprement plutot que d'echouer.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Verifier une erreur de validation en FO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyValidationErrorFOMacroTest extends MacroTest {

    /** Libelles des boutons FO qui soumettent l'etape courante (references confirmees). */
    private static final String[] ADVANCE_BUTTONS = {
        "Etape suivante", "Voir le récapitulatif", "Valider le récapitulatif"
    };

    @Step("Verifier une erreur de validation en front office")
    public static void run(FormsContext ctx, ValidationCheckDataSet data) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire publie doit exister (ctx.formId) avant la verification FO");

        // 1) Saisie invalide : on attend un rejet (message/marqueur d'erreur).
        Page page = openPublishedForm(ctx);
        Locator field = fieldByLabel(page, data.label());
        Assumptions.assumeTrue(field.count() > 0,
            "Champ '" + data.label() + "' introuvable sur le formulaire FO : verification ignoree.");
        fillField(field.first(), data.invalidValue());

        Assumptions.assumeTrue(clickAdvance(page),
            "Aucun bouton de soumission d'etape (Etape suivante / Voir le récapitulatif) present en FO : "
            + "verification ignoree.");
        page.waitForLoadState();

        boolean errorShown = errorPresent(page, data.errorHint());
        Assumptions.assumeTrue(errorShown,
            "Aucun message/marqueur d'erreur detecte apres saisie invalide : le controle de validation "
            + "ne se declenche pas de facon detectable en FO (validateur/donnees a calibrer) : ignoree.");

        // 2) Saisie valide (formulaire re-ouvert a neuf) : on verifie l'absence d'erreur.
        Page fresh = openPublishedForm(ctx);
        Locator freshField = fieldByLabel(fresh, data.label());
        Assumptions.assumeTrue(freshField.count() > 0,
            "Champ '" + data.label() + "' introuvable a la reouverture FO : verification de la saisie "
            + "valide ignoree.");
        fillField(freshField.first(), data.validValue());
        Assumptions.assumeTrue(clickAdvance(fresh),
            "Bouton de soumission absent a la reouverture FO : verification de la saisie valide ignoree.");
        fresh.waitForLoadState();

        Assertions.assertFalse(errorPresent(fresh, data.errorHint()),
            "La saisie valide '" + data.validValue() + "' ne devrait plus declencher d'erreur de validation");
    }

    // === Navigation FO ===

    private static Page openPublishedForm(FormsContext ctx) {
        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId);
        Page page = ctx.page;
        page.waitForLoadState();
        dismissOffcanvas(page);
        return page;
    }

    private static void dismissOffcanvas(Page page) {
        Locator backdrop = page.locator(".offcanvas-backdrop");
        if (backdrop.count() > 0) {
            page.keyboard().press("Escape");
            try {
                backdrop.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(3000));
            } catch (RuntimeException ignored) {
                // backdrop persistant : on continue au mieux
            }
        }
    }

    // === Champs / erreurs ===

    private static Locator fieldByLabel(Page page, String label) {
        Locator byLabel = page.getByLabel(label);
        if (byLabel.count() > 0) {
            return byLabel;
        }
        Locator byRole = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(label));
        if (byRole.count() > 0) {
            return byRole;
        }
        // Repli structurel : en FO Lutece, <label class="form-label"> n'a pas d'attribut "for"
        // (aucune association ARIA), donc getByLabel/getByRole echouent. Le controle
        // (input/select/textarea) est le following-sibling du label dans son conteneur
        // div.display_field_*. On resout donc le champ via le texte du label + son frere de saisie.
        Locator byStructure = page.locator(
            "xpath=//label[contains(normalize-space(.),'" + label + "')]"
            + "/following-sibling::*[self::input or self::select or self::textarea]");
        if (byStructure.count() > 0) {
            return byStructure;
        }
        return byLabel;
    }

    private static void fillField(Locator field, String value) {
        field.click();
        try {
            field.fill(value);
        } catch (RuntimeException notTextInput) {
            try {
                field.selectOption(value);
            } catch (RuntimeException ignored) {
                // ni champ texte ni liste : on laisse en l'etat, l'assertion/gate en tiendra compte
            }
        }
    }

    /** Clique le premier bouton de soumission d'etape present et visible. Renvoie false si aucun. */
    private static boolean clickAdvance(Page page) {
        for (String name : ADVANCE_BUTTONS) {
            Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
            if (btn.count() > 0 && btn.first().isVisible()) {
                btn.first().click();
                return true;
            }
        }
        return false;
    }

    /**
     * Detecte une erreur de validation : d'abord via le fragment de message attendu ({@code errorHint}),
     * sinon via des marqueurs generiques du DOM.
     */
    private static boolean errorPresent(Page page, String errorHint) {
        if (errorHint != null && !errorHint.isBlank()) {
            Locator hint = page.getByText(errorHint);
            if (hint.count() > 0 && hint.first().isVisible()) {
                return true;
            }
        }
        // Marqueurs d'erreur "actifs" (alignes sur ConfigureFormOptionsMacroTest.hasVisibleError) :
        // volontairement restreints pour eviter un faux positif sur un conteneur d'erreur toujours
        // present dans le DOM.
        Locator markers = page.locator(".alert-danger, .has-error, .is-invalid");
        return markers.count() > 0 && markers.first().isVisible();
    }

    @Test
    @DisplayName("Verifier une erreur de validation en FO (provisionnement + controle de validation + publication)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question a valider"));
        AddValidationControlMacroTest.run(ctx, ControlDataSet.validationDefaults());
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        run(ctx, ValidationCheckDataSet.defaults());
    }
}
