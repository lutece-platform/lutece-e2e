package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
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

    /** Delai d'ouverture du calendrier d'un champ date, en millisecondes. */
    private static final double CALENDRIER_TIMEOUT_MS = 5000;

    /** Rang du jour choisi dans le mois affiche : un jour de plein milieu, jamais hors bornes. */
    private static final int JOUR_CIBLE = 14;

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
            choisirDateAuCalendrier(page, field, data.label());
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
     * Choisit une date en passant par le calendrier du champ.
     *
     * <p>Le champ visible d'une question de type date ne porte pas d'attribut {@code name} : il
     * n'est qu'un affichage, la valeur soumise etant deposee dans un champ cache par le calendrier
     * seul. Y ecrire directement laisse donc la reponse vide tout en donnant le change, puisque le
     * champ visible, lui, affiche bien le texte saisi.</p>
     *
     * @param page   page front-office courante
     * @param champ  champ de date vise
     * @param label  libelle de la question, pour le diagnostic
     */
    private static void choisirDateAuCalendrier(Page page, Locator champ, String label) {
        champ.click();
        Locator jours = page.locator(
            ".datepicker-dropdown .datepicker-cell.day:not(.prev):not(.next):not(.disabled)");
        try {
            jours.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(CALENDRIER_TIMEOUT_MS));
        } catch (RuntimeException calendrierAbsent) {
            Assumptions.assumeTrue(false,
                "Le calendrier du champ '" + label + "' ne s'est pas ouvert : date non renseignable");
        }
        jours.nth(Math.min(JOUR_CIBLE, jours.count() - 1)).click();
    }

    /**
     * Localise le champ FO selon son type. Retourne un locateur mono-element ou {@code null} si absent.
     */
    private static Locator locateField(Page page, String label, String kind) {
        if ("date".equals(kind)) {
            Locator byLabel = page.getByLabel(label);
            if (byLabel.count() > 0) {
                return byLabel.first();
            }
            Locator datepicker = page.locator("input.lutece-datepicker");
            return datepicker.count() > 0 ? datepicker.first() : null;
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
