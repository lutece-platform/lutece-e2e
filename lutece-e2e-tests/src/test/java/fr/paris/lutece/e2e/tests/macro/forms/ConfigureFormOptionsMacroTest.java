package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormOptionsDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : configurer les options d'un formulaire (page modifyForm).
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.formTitle}. Ecrit : rien (options persistees cote site).</p>
 *
 * <p>Ne modifie que les options effectivement presentes : chaque locator est garde par
 * {@code isVisible()} (dates de disponibilite, message d'indisponibilite, nombre max de reponses,
 * une reponse par utilisateur, recapitulatif, sauvegarde/brouillon, fil d'Ariane, authentification,
 * categorie, groupe de travail).</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Configurer les options du formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ConfigureFormOptionsMacroTest extends MacroTest {

    @Step("Configurer les options du formulaire")
    public static void run(FormsContext ctx, FormOptionsDataSet data) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant de configurer ses options");

        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId);

        // Dates de disponibilite (flatpickr) — selecteurs confirmes par FormsEditPage.publishOnPortal
        setFlatpickrIfPresent(page, "#availabilityStartDate", data.availabilityStartDate());
        setFlatpickrIfPresent(page, "#availabilityEndDate", data.availabilityEndDate());

        // Message d'indisponibilite (zone de texte ou champ texte)
        fillFirstPresent(page, data.unavailableMessage(),
            "#unavailableMessage", "textarea[name='unavailableMessage']", "input[name='unavailableMessage']");

        // Nombre maximum de reponses
        if (data.maxResponses() != null) {
            fillFirstPresent(page, String.valueOf(data.maxResponses()),
                "#maxResponse", "#max_response", "input[name='maxResponse']", "input[name='max_response']");
        }

        // Cases a cocher (uniquement si presentes)
        if (data.oneResponsePerUser()) {
            checkByLabelIfPresent(page, "Une seule réponse par utilisateur");
            checkFirstPresent(page, "#one_form_response", "input[name='one_form_response']");
        }
        if (data.displaySummary()) {
            checkByLabelIfPresent(page, "Afficher le récapitulatif");
            checkFirstPresent(page, "#displaySummary", "input[name='displaySummary']");
        }
        if (data.enableBackup()) {
            checkByLabelIfPresent(page, "Sauvegarde");
            checkByLabelIfPresent(page, "Brouillon");
            checkFirstPresent(page, "#backup", "input[name='backup']");
        }
        if (data.displayBreadcrumb()) {
            checkByLabelIfPresent(page, "Fil d'Ariane");
            checkFirstPresent(page, "#breadcrumb", "input[name='breadcrumb']");
        }
        if (data.requireAuthentication()) {
            checkByLabelIfPresent(page, "Authentification");
            checkFirstPresent(page, "#authentificationNeeded", "input[name='authentificationNeeded']");
        }

        // Categorie / groupe de travail (listes deroulantes)
        if (data.category() != null) {
            selectByLabelIfPresent(page, data.category(), "#category", "select[name='category']", "#idCategory");
        }
        if (data.workgroup() != null) {
            selectByLabelIfPresent(page, data.workgroup(), "#workgroup", "select[name='workgroup']", "#workgroup_key");
        }

        submitModify(page);
        page.waitForLoadState();

        Assertions.assertFalse(hasVisibleError(page),
            "La sauvegarde des options ne devrait pas afficher d'erreur");
        int id = MacroSupport.extractFormId(ctx, ctx.formTitle);
        Assertions.assertTrue(id > 0,
            "Le formulaire '" + ctx.formTitle + "' devrait toujours exister apres configuration des options");
    }

    // === Helpers gardes ===

    private static void setFlatpickrIfPresent(Page page, String selector, String date) {
        if (date == null || date.isBlank()) {
            return;
        }
        Locator input = page.locator(selector);
        if (input.count() > 0) {
            input.first().evaluate(
                "(el, d) => { if (el._flatpickr) { el._flatpickr.setDate(d, true); } else { el.value = d; } }",
                date);
        }
    }

    private static void fillFirstPresent(Page page, String value, String... selectors) {
        if (value == null) {
            return;
        }
        for (String selector : selectors) {
            Locator loc = page.locator(selector);
            if (loc.count() > 0 && loc.first().isVisible()) {
                loc.first().fill(value);
                return;
            }
        }
    }

    private static void checkFirstPresent(Page page, String... selectors) {
        for (String selector : selectors) {
            Locator loc = page.locator(selector);
            if (loc.count() > 0 && loc.first().isVisible()) {
                if (!loc.first().isChecked()) {
                    loc.first().check();
                }
                return;
            }
        }
    }

    private static void checkByLabelIfPresent(Page page, String label) {
        Locator cb = page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(label));
        if (cb.count() > 0 && cb.first().isVisible() && !cb.first().isChecked()) {
            cb.first().check();
        }
    }

    private static void selectByLabelIfPresent(Page page, String label, String... selectors) {
        for (String selector : selectors) {
            Locator loc = page.locator(selector);
            if (loc.count() > 0 && loc.first().isVisible()) {
                try {
                    loc.first().selectOption(new SelectOption().setLabel(label));
                } catch (RuntimeException ignored) {
                    // option absente : on conserve la valeur par defaut
                }
                return;
            }
        }
    }

    private static void submitModify(Page page) {
        Locator byName = page.locator("button[name='action_modifyForm']");
        if (byName.count() > 0 && byName.first().isVisible()) {
            byName.first().click();
            return;
        }
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Modifier le formulaire")).first().click();
    }

    private static boolean hasVisibleError(Page page) {
        Locator err = page.locator(".alert-danger, .has-error, .is-invalid");
        return err.count() > 0 && err.first().isVisible();
    }

    @Test
    @DisplayName("Configurer les options d'un formulaire (auto-provisionnement du formulaire)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        run(ctx, FormOptionsDataSet.defaults());
    }
}
