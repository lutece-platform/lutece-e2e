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

        // Cases a cocher : ciblees par leur ATTRIBUT name reel, releve sur le DOM de modifyForm.
        // L'ancienne version passait par getByRole(CHECKBOX, name=...) avec des libelles approximatifs
        // ("Sauvegarde", "Brouillon") : le nom accessible reel est "Activer la sauvegarde des reponses
        // incompletes", donc rien ne matchait et la case n'etait JAMAIS cochee, en silence (faux vert).
        boolean backupControl = setCheckboxByName(page, "backupEnabled", data.enableBackup());
        setCheckboxByName(page, "oneResponseByUser", data.oneResponsePerUser());
        setCheckboxByName(page, "displaySummary", data.displaySummary());
        setCheckboxByName(page, "authentificationNeeded", data.requireAuthentication());
        // Pas de case "fil d'Ariane" sur ce formulaire d'options : data.displayBreadcrumb() est sans effet.

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

        // Relecture : une option demandee et pilotable DOIT etre effectivement persistee. Sans ce
        // controle, un selecteur errone repassait en vert alors que rien n'avait ete coche, et l'echec
        // ne se manifestait que bien plus tard en front-office (brique de brouillon sans controle).
        if (data.enableBackup() && backupControl) {
            MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId);
            Assertions.assertTrue(
                page.locator("input[type='checkbox'][name='backupEnabled']").first().isChecked(),
                "L'option de sauvegarde des reponses incompletes devrait etre active apres enregistrement");
        }
    }

    /**
     * Positionne une case a cocher par son attribut {@code name} et retourne {@code false} si le
     * controle est absent du formulaire (option non pilotable sur cette version).
     */
    private static boolean setCheckboxByName(Page page, String name, boolean wanted) {
        Locator cb = page.locator("input[type='checkbox'][name='" + name + "']");
        if (cb.count() == 0) {
            return false;
        }
        boolean checked = cb.first().isChecked();
        if (wanted && !checked) {
            cb.first().check();
        } else if (!wanted && checked) {
            cb.first().uncheck();
        }
        return true;
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
