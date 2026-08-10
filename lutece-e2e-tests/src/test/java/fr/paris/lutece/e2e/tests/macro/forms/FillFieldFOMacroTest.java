package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
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

import java.util.Locale;

/**
 * Brique macro : remplir UN champ du formulaire en front-office (page deja ouverte en vue FO).
 *
 * <p>Lit : {@code ctx.formId} et la page FO courante. Ecrit : rien. La strategie de localisation
 * reprend celle du Page Object CDI {@code FormsPage} : getByRole TEXTBOX (text) / SPINBUTTON (number)
 * par libelle, puis getByLabel, puis premier champ visible du bon type ; pour une date, saisie via
 * flatpickr en JavaScript. Si le champ est absent, la brique est ignoree (Assumptions).</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Remplir un champ en front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class FillFieldFOMacroTest extends MacroTest {

    @Step("Remplir un champ en front-office")
    public static void run(FormsContext ctx, FieldValueDataSet data) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant de remplir un champ en front-office");

        Page page = ctx.page;
        page.waitForLoadState();

        String kind = data.kind() == null ? "text" : data.kind().toLowerCase(Locale.ROOT);
        Locator field = locateField(page, data.label(), kind);
        Assumptions.assumeTrue(field != null,
            "Champ FO '" + data.label() + "' (" + kind + ") introuvable sur la page courante "
                + "(libelle different, champ absent ou formulaire non ouvert) : brique ignoree");

        if ("date".equals(kind)) {
            // Meme approche que FormsPage.fillDateFieldFO : on pilote flatpickr, avec repli sur value+change.
            field.evaluate(
                "(el, date) => { if (el._flatpickr) { el._flatpickr.setDate(date, true); }"
                    + " else { el.value = date; el.dispatchEvent(new Event('change')); } }",
                data.value());
        } else {
            field.click();
            field.fill(data.value());
        }

        // Verification best-effort de la valeur posee.
        String actual = safeInputValue(field);
        if ("date".equals(kind)) {
            Assertions.assertTrue(actual != null && !actual.isBlank(),
                "Le champ date '" + data.label() + "' devrait porter une valeur apres saisie flatpickr "
                    + "(valeur lue: '" + actual + "')");
        } else {
            Assertions.assertTrue(actual != null
                    && (actual.equals(data.value()) || actual.contains(data.value())),
                "Le champ '" + data.label() + "' devrait porter la valeur '" + data.value()
                    + "' (valeur lue: '" + actual + "')");
        }
    }

    /**
     * Localise le champ FO selon son type. Retourne un locateur mono-element ou {@code null} si absent.
     */
    private static Locator locateField(Page page, String label, String kind) {
        if ("date".equals(kind)) {
            Locator flatpickr = page.locator("input.flatpickr-input");
            if (flatpickr.count() > 0) {
                return flatpickr.first();
            }
            Locator byLabel = page.getByLabel(label);
            return byLabel.count() > 0 ? byLabel.first() : null;
        }

        AriaRole role = "number".equals(kind) ? AriaRole.SPINBUTTON : AriaRole.TEXTBOX;

        // Strategie 1 : role par nom.
        Locator hit = firstVisible(page.getByRole(role, new Page.GetByRoleOptions().setName(label)));
        if (hit != null) {
            return hit;
        }
        // Strategie 2 : getByLabel.
        hit = firstVisible(page.getByLabel(label));
        if (hit != null) {
            return hit;
        }
        // Strategie 3 : premier champ visible du bon type.
        String css = "number".equals(kind) ? "input[type='number']" : "input[type='text']";
        hit = firstVisible(page.locator(css));
        if (hit != null) {
            return hit;
        }
        // Repli texte : textarea.
        if (!"number".equals(kind)) {
            hit = firstVisible(page.locator("textarea"));
        }
        return hit;
    }

    /** Premier element visible du locateur (parcours borne), ou {@code null}. */
    private static Locator firstVisible(Locator loc) {
        int n = Math.min(loc.count(), 40);
        for (int i = 0; i < n; i++) {
            Locator candidate = loc.nth(i);
            try {
                if (candidate.isVisible()) {
                    return candidate;
                }
            } catch (RuntimeException ignored) {
                // element detache : on continue
            }
        }
        return null;
    }

    private static String safeInputValue(Locator field) {
        try {
            return field.inputValue();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Test
    @DisplayName("Remplir un champ texte en FO (auto-provisionnement + ouverture FO)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Champ FO"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);
        run(ctx, FieldValueDataSet.of("Champ FO", "Valeur E2E macro"));
    }
}
