package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour le formulaire en front office (popup).
 */
public class FormsFrontOfficePage {

    private final Page page;
    private final String baseUrl;

    public FormsFrontOfficePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Ferme l'offcanvas s'il est present (backdrop bloquant les interactions).
     */
    public FormsFrontOfficePage dismissOffcanvasIfPresent() {
        page.waitForLoadState();
        Locator backdrop = page.locator(".offcanvas-backdrop");
        if (backdrop.count() > 0) {
            page.keyboard().press("Escape");
            backdrop.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
        }
        return this;
    }

    /**
     * Remplit un champ texte par son label.
     * Utilise plusieurs strategies pour trouver le champ.
     */
    public FormsFrontOfficePage fillTextField(String label, String value) {
        page.waitForLoadState();
        // Strategie 1: getByRole avec le label
        Locator byRole = page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName(label));
        if (byRole.count() > 0) {
            byRole.first().click();
            byRole.first().fill(value);
            return this;
        }
        // Strategie 2: getByLabel
        Locator byLabel = page.getByLabel(label);
        if (byLabel.count() > 0) {
            byLabel.first().click();
            byLabel.first().fill(value);
            return this;
        }
        // Strategie 3: locator par placeholder ou aria-label
        Locator byPlaceholder = page.locator("input[type='text'][placeholder*='" + label + "'], input[type='text'][aria-label*='" + label + "']");
        if (byPlaceholder.count() > 0) {
            byPlaceholder.first().click();
            byPlaceholder.first().fill(value);
            return this;
        }
        // Strategie 4: premier champ texte (sans :visible qui n'est pas valide en CSS)
        Locator allTextInputs = page.locator("input[type='text']");
        for (int i = 0; i < allTextInputs.count(); i++) {
            Locator input = allTextInputs.nth(i);
            if (input.isVisible()) {
                input.click();
                input.fill(value);
                return this;
            }
        }
        // Fallback: textarea si pas d'input text
        Locator textArea = page.locator("textarea");
        if (textArea.count() > 0 && textArea.first().isVisible()) {
            textArea.first().click();
            textArea.first().fill(value);
            return this;
        }
        throw new RuntimeException("Aucun champ texte trouve pour le label: " + label);
    }

    /**
     * Remplit un champ nombre par son label.
     * Utilise plusieurs strategies pour trouver le champ.
     */
    public FormsFrontOfficePage fillNumberField(String label, String value) {
        page.waitForLoadState();
        // Strategie 1: getByRole avec le label
        Locator byRole = page.getByRole(AriaRole.SPINBUTTON,
            new Page.GetByRoleOptions().setName(label));
        if (byRole.count() > 0) {
            byRole.first().click();
            byRole.first().fill(value);
            return this;
        }
        // Strategie 2: getByLabel
        Locator byLabel = page.getByLabel(label);
        if (byLabel.count() > 0) {
            byLabel.first().click();
            byLabel.first().fill(value);
            return this;
        }
        // Strategie 3: premier champ number visible
        Locator allNumberInputs = page.locator("input[type='number']");
        for (int i = 0; i < allNumberInputs.count(); i++) {
            Locator input = allNumberInputs.nth(i);
            if (input.isVisible()) {
                input.click();
                input.fill(value);
                return this;
            }
        }
        throw new RuntimeException("Aucun champ nombre trouve pour le label: " + label);
    }

    /**
     * Remplit un champ date (flatpickr) via JavaScript.
     */
    public FormsFrontOfficePage fillDateField(String value) {
        Locator input = page.locator("input.flatpickr-input");
        if (input.count() > 0) {
            input.first().evaluate("(el, date) => { if (el._flatpickr) { el._flatpickr.setDate(date, true); } else { el.value = date; el.dispatchEvent(new Event('change')); } }", value);
        }
        return this;
    }

    /**
     * Clique sur "Etape suivante" — SEULEMENT s'il est present.
     *
     * <p>Le bouton "Etape suivante" n'existe que s'il reste une etape apres l'etape courante. Sur un
     * formulaire mono-etape (ou sur la derniere etape), le FO propose directement "Voir le
     * recapitulatif" : on n'avance donc pas (no-op) et le flux enchaine sur {@link #clickViewSummary()}.
     * On attend d'abord que l'un des deux boutons de progression soit rendu pour eviter une detection
     * prematuree (evite le faux timeout observe sur les formulaires mono-etape).</p>
     */
    public FormsFrontOfficePage clickNextStep() {
        try {
            page.locator("button:has-text('Etape suivante'), button:has-text('Voir le récapitulatif')")
                .first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (RuntimeException ignored) {
            // aucun des deux boutons rendu : on laisse le flux se poursuivre (echec plus explicite en aval)
        }
        Locator next = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Etape suivante"));
        if (next.count() > 0 && next.first().isVisible()) {
            next.first().click();
            page.waitForLoadState();
        }
        return this;
    }

    /**
     * Clique sur "Voir le recapitulatif".
     */
    public FormsFrontOfficePage clickViewSummary() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Voir le récapitulatif")).click();
        return this;
    }

    /**
     * Clique sur "Valider le recapitulatif".
     */
    public void clickValidateSummary() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Valider le récapitulatif")).click();
    }

    /**
     * Retourne la page Playwright sous-jacente.
     */
    public Page getPage() {
        return page;
    }
}
