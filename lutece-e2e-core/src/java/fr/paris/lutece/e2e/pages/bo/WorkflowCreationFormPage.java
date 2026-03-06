package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour le formulaire de creation d'un workflow.
 */
public class WorkflowCreationFormPage {

    private final Page page;
    private final String baseUrl;

    public WorkflowCreationFormPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Remplit le champ nom du workflow.
     */
    public WorkflowCreationFormPage fillName(String name) {
        page.locator("input[name=\"name\"]").click();
        page.locator("input[name=\"name\"]").fill(name);
        return this;
    }

    /**
     * Remplit le champ description du workflow.
     */
    public WorkflowCreationFormPage fillDescription(String description) {
        page.locator("textarea[name=\"description\"]").click();
        page.locator("textarea[name=\"description\"]").fill(description);
        return this;
    }

    /**
     * Coche le radio "Publie".
     */
    public WorkflowCreationFormPage checkPublished() {
        page.getByRole(AriaRole.RADIO,
            new Page.GetByRoleOptions().setName("Publié").setExact(true)).check();
        return this;
    }

    /**
     * Clique sur Enregistrer et retourne à la liste des workflows.
     */
    public void save() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        page.waitForLoadState();

        // Gérer la page AdminMessage si elle apparaît
        if (page.url().contains("AdminMessage")) {
            page.locator("a:has-text('OK'), button:has-text('OK')").first().click();
            page.waitForLoadState();
        }

        // S'assurer qu'on retourne à la liste des workflows
        if (!page.url().contains("ManageWorkflow")) {
            page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
            page.waitForLoadState();
        }
    }
}
