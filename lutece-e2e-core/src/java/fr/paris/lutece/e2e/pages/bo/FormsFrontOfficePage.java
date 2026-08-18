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
     * Avance d'etape en etape jusqu'a la derniere.
     *
     * <p>On cible le bouton par son action Lutece stable {@code action_doSaveStep} via son <b>id</b>
     * ({@code button#action_doSaveStep}) plutot que par son libelle francais. Motif : le libelle rendu
     * est "&Eacute;tape suivante" (avec accent) et le match par nom accessible de Playwright est
     * <i>insensible a la casse mais sensible aux accents</i> — l'ancien selecteur "Etape suivante" (sans
     * accent) ne matchait donc jamais (count == 0), le clic etait un no-op silencieux et le formulaire
     * restait bloque a l'etape 1 (le timeout surgissait plus loin, dans {@link #clickViewSummary()}).
     * L'id vise le bouton VISIBLE : le doublon cache (meme {@code name}, {@code aria-hidden}) n'a pas d'id.</p>
     *
     * <p>Sur la derniere etape, {@code action_doSaveStep} disparait au profit de "Voir le recapitulatif" :
     * la boucle s'arrete alors et le flux enchaine sur {@link #clickViewSummary()}. Boucle bornee
     * (garde-fou) pour supporter les formulaires a N etapes.</p>
     */
    public FormsFrontOfficePage clickNextStep() {
        Locator next = page.locator("button#action_doSaveStep");
        for (int step = 0; step < 10; step++) {
            try {
                next.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(8000));
            } catch (RuntimeException noMoreSteps) {
                // plus de bouton "Etape suivante" : derniere etape atteinte -> on sort
                break;
            }
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
