package fr.paris.lutece.e2e.tests.bo.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour le menu d'administration Lutece.
 */
public class AdminMenuPage {

    private final Page page;
    private final String baseUrl;

    public AdminMenuPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Vérifie si on est bien connecté (menu visible).
     */
    public boolean isLoggedIn() {
        // Attendre que la page charge
        page.waitForLoadState();
        return page.url().contains("AdminMenu") ||
               page.locator(".lutece-admin-menu, #admin-menu, nav").isVisible();
    }

    /**
     * Clique sur le menu Système.
     */
    public AdminMenuPage clickSystemMenu() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(" Contenu")).click();
        return this;
    }

    /**
     * Clique sur le menu Gestionnaires.
     */
    public AdminMenuPage clickGestionnairesMenu() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(" Gestionnaires")).click();
        return this;
    }

    /**
     * Clique sur le menu Site.
     */
    public AdminMenuPage clickSiteMenu() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(" Site")).click();
        return this;
    }

    /**
     * Clique sur le menu Charte.
     */
    public AdminMenuPage clickCharteMenu() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(" Charte")).click();
        return this;
    }

    /**
     * Clique sur le menu Applications.
     */
    public AdminMenuPage clickApplicationsMenu() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(" Applications")).click();
        return this;
    }

    /**
     * Navigue vers la gestion des workflows.
     */
    public void goToWorkflowManagement() {
        // Navigation directe pour plus de fiabilité et rapidité
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
        page.waitForLoadState();
    }

    /**
     * Navigue vers la gestion des formulaires.
     */
    public void goToFormsManagement() {
        // Navigation directe pour plus de fiabilité et rapidité
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageForms.jsp");
        page.waitForLoadState();
    }

    /**
     * Accède à la gestion des propriétés du site.
     */
    public SitePropertiesPage goToSiteProperties() {
        clickSystemMenu();
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Gestion des propriétés du site")).first().click();
        return new SitePropertiesPage(page, baseUrl);
    }

    /**
     * Se déconnecte.
     */
    public LoginPage logout() {
        // Adapter selon l'UI de votre Lutece
        page.locator("a[href*='logout'], .logout-link, #logout").click();
        return new LoginPage(page, baseUrl);
    }

    /**
     * Retourne l'URL courante.
     */
    public String getCurrentUrl() {
        return page.url();
    }
}
