package fr.paris.lutece.e2e.pages;

import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.core.BrowserSession;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Page Object pour le menu d'administration Lutece.
 * Basé sur l'implémentation fonctionnelle de lutece-e2e-tests-bo3.
 */
@Dependent
public class AdminMenuPage extends BasePage {

    @Inject
    public AdminMenuPage(BrowserSession browser) {
        super(browser);
    }

    public boolean isLoggedIn() {
        waitForLoad();
        return page().url().contains("AdminMenu") ||
                page().locator(".lutece-admin-menu, #admin-menu, nav").isVisible();
    }

    public AdminMenuPage clickContentMenu() {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(" Contenu")).click();
        return this;
    }

    public AdminMenuPage clickGestionnairesMenu() {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(" Gestionnaires")).click();
        return this;
    }

    public AdminMenuPage clickSiteMenu() {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(" Site")).click();
        return this;
    }

    public AdminMenuPage clickApplicationsMenu() {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(" Applications")).click();
        return this;
    }

    public void goToWorkflowManagement() {
        navigate("/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
    }

    public void goToFormsManagement() {
        navigate("/jsp/admin/plugins/forms/ManageForms.jsp");
    }

    public void goToAdminMenu() {
        navigate("/jsp/admin/AdminMenu.jsp");
    }

    public void logout() {
        // Simple logout selector as in working implementation
        page().locator("a[href*='logout'], .logout-link, #logout").click();
    }

    public String getCurrentUrl() {
        return page().url();
    }
}
