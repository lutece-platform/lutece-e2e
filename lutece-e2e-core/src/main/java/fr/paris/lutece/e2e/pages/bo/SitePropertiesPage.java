package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour la page de gestion des propriétés du site.
 */
public class SitePropertiesPage {

    private final Page page;
    private final String baseUrl;

    public SitePropertiesPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Vérifie si la page est chargée.
     */
    public boolean isDisplayed() {
        page.waitForLoadState();
        return page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Propriétés par défaut du site")).isVisible();
    }

    /**
     * Clique sur l'onglet "Propriétés par défaut du site".
     */
    public SitePropertiesPage clickDefaultPropertiesTab() {
        page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Propriétés par défaut du site")).click();
        return this;
    }

    /**
     * Vérifie si l'onglet des propriétés par défaut est actif.
     */
    public boolean isDefaultPropertiesTabActive() {
        Locator tab = page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Propriétés par défaut du site"));
        String ariaSelected = tab.getAttribute("aria-selected");
        return "true".equals(ariaSelected);
    }

    /**
     * Récupère le titre de la page.
     */
    public String getPageTitle() {
        return page.title();
    }

    /**
     * Vérifie si un élément spécifique est présent sur la page.
     */
    public boolean hasElement(String selector) {
        return page.locator(selector).isVisible();
    }
}
