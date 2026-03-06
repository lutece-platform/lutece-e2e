package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour l'edition d'un workflow (etats, actions, taches).
 */
public class WorkflowEditPage {

    private final Page page;
    private final String baseUrl;

    public WorkflowEditPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Clique sur "Modifier le workflow" pour le workflow identifie par son nom
     * dans la liste des workflows.
     */
    public WorkflowEditPage clickModifyWorkflow(String workflowName) {
        page.waitForLoadState();

        // Cliquer directement sur le nom du workflow pour ouvrir la page d'édition
        page.locator("a:has-text('" + workflowName + "')").first().click();
        page.waitForLoadState();

        // Debug: capture d'écran après navigation
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target/screenshots"));
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/debug-workflow-edit.png"))
                .setFullPage(true));
        } catch (Exception e) {
            // ignore
        }

        return this;
    }

    /**
     * Clique sur "Modifier le workflow" depuis une page de detail (action, tache).
     * Utilise le lien unique present sur la page.
     */
    public WorkflowEditPage clickModifyWorkflowLink() {
        page.locator("a:has-text('Modifier le workflow'), a:has-text('Retour Modification du workflow')").first().click();
        return this;
    }

    /**
     * Navigue vers la page d'edition du workflow si necessaire.
     * Navigue d'abord vers la liste si pas déjà sur une page workflow, puis clique sur modifier.
     */
    public WorkflowEditPage ensureOnEditPage(String workflowName) {
        page.waitForLoadState();

        // Si on est sur la page de liste, cliquer sur modifier
        if (page.url().contains("ManageWorkflow.jsp") && !page.url().contains("id_workflow")) {
            clickModifyWorkflow(workflowName);
        }
        // Si on n'est pas sur une page workflow du tout, naviguer vers la liste d'abord
        else if (!page.url().contains("workflow")) {
            page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
            page.waitForLoadState();
            clickModifyWorkflow(workflowName);
        }
        // Sinon on est déjà sur la page d'édition
        return this;
    }

    /**
     * Ajoute un etat au workflow.
     */
    public WorkflowEditPage addState(String name, String description, boolean isInitial) {
        // C'est un lien, pas un bouton
        page.locator("a:has-text('Ajouter un état')").click();
        page.waitForLoadState();

        // Debug screenshot
        try {
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/debug-add-state.png"))
                .setFullPage(true));
        } catch (Exception e) {}

        page.locator("input[name='name']").fill(name);
        page.locator("textarea[name='description']").fill(description);
        if (isInitial) {
            // La checkbox a id="is_initial_state" et name="is_initial_state"
            page.locator("input#is_initial_state, input[name='is_initial_state']").first().check();
        }
        page.locator("button:has-text('Enregistrer'), input[value='Enregistrer']").first().click();
        page.waitForLoadState();
        dismissAdminMessage();
        return this;
    }

    /**
     * Clique sur l'onglet Actions.
     */
    public WorkflowEditPage clickActionsTab() {
        page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Actions")).click();
        return this;
    }

    /**
     * Ajoute une action au workflow.
     */
    public WorkflowEditPage addAction(String name, String description,
            String linkedStateName, String stateAfterName) {
        // C'est un lien, pas un bouton
        page.locator("a:has-text('Ajouter une action')").click();
        page.locator("input[name=\"name\"]").click();
        page.locator("input[name=\"name\"]").fill(name);
        page.locator("textarea[name=\"description\"]").click();
        page.locator("textarea[name=\"description\"]").fill(description);
        page.getByRole(AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName(linkedStateName)).check();
        // Selectionner l'etat d'arrivee par son label (pas par ID)
        page.locator("#id_state_after").selectOption(
            new SelectOption().setLabel(stateAfterName));
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        page.waitForLoadState();
        dismissAdminMessage();
        return this;
    }

    /**
     * Clique sur "Modifier l'action" pour acceder a la configuration de la tache.
     */
    public WorkflowEditPage clickModifyAction() {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Modifier l'action")).click();
        return this;
    }

    /**
     * Selectionne un type de tache dans le dropdown.
     */
    public WorkflowEditPage selectTask(String taskType) {
        page.getByLabel("Nouvelle tâche").selectOption(taskType);
        return this;
    }

    /**
     * Clique sur le bouton Inserer pour ajouter la tache.
     */
    public WorkflowEditPage clickInsertTask() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Insérer")).click();
        return this;
    }

    /**
     * Publie le workflow en cochant le radio Publie et en sauvegardant.
     */
    public void publishWorkflow() {
        page.getByRole(AriaRole.RADIO,
            new Page.GetByRoleOptions().setName("Publié").setExact(true)).check();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        page.waitForLoadState();
        dismissAdminMessage();
    }

    /**
     * Retourne a la liste des workflows.
     */
    public WorkflowListPage goBackToList() {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Gestion des workflows")).click();
        return new WorkflowListPage(page, baseUrl);
    }

    /**
     * Gere la page AdminMessage de Lutece si elle apparait apres une operation.
     * Clique sur le bouton OK pour revenir a la page precedente.
     */
    private void dismissAdminMessage() {
        if (page.url().contains("AdminMessage")) {
            page.getByText("OK", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForLoadState();
        }
    }
}
