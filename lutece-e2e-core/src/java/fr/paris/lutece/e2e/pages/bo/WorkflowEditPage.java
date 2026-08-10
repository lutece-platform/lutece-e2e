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

        // Naviguer directement vers la page d'édition du workflow
        String idWorkflow = getWorkflowId(workflowName);
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ModifyWorkflow.jsp?id_workflow=" + idWorkflow);
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
        Locator link = page.locator("a:has-text('Modifier le workflow'), a:has-text('Retour Modification du workflow')").first();
        String href = link.getAttribute("href");
        if (href != null && href.contains("id_workflow=")) {
            String idWorkflow = href.split("id_workflow=")[1].split("&")[0].split("#")[0];
            page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ModifyWorkflow.jsp?id_workflow=" + idWorkflow);
            page.waitForLoadState();
        } else {
            link.click();
            page.waitForLoadState();
        }
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
        // Ouvrir le formulaire de creation d'un etat
        String idWorkflow = currentWorkflowId();
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/CreateState.jsp?id_workflow=" + idWorkflow);
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
        // Le bouton de validation est <button name='save'> avec icone + title (pas de texte
        // "Enregistrer") : cibler l'attribut name (le libelle-based ne matche pas le contenu).
        page.locator("button[name='save']").first().click();
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
        // Ouvrir le formulaire de creation d'une action
        String idWorkflow = currentWorkflowId();
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/CreateAction.jsp?id_workflow=" + idWorkflow);
        page.waitForLoadState();
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
        Locator link = page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Modifier l'action")).first();
        String href = link.getAttribute("href");
        if (href != null && href.contains("id_action=")) {
            String idAction = href.split("id_action=")[1].split("&")[0].split("#")[0];
            page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ModifyAction.jsp?id_action=" + idAction);
            page.waitForLoadState();
        } else {
            link.click();
        }
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
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
        page.waitForLoadState();
        return new WorkflowListPage(page, baseUrl);
    }

    /**
     * Recupere l'identifiant du workflow a partir de son nom dans la liste.
     * Navigue vers la liste puis extrait id_workflow du lien de la ligne correspondante.
     */
    private String getWorkflowId(String workflowName) {
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
        page.waitForLoadState();
        Locator link = page.locator("a[href*='id_workflow=']:has-text('" + workflowName + "')").first();
        String href = link.getAttribute("href");
        if (href != null && href.contains("id_workflow=")) {
            return href.split("id_workflow=")[1].split("&")[0].split("#")[0];
        }
        return null;
    }

    /**
     * Extrait id_workflow de l'URL courante (page d'edition ModifyWorkflow.jsp).
     */
    private String currentWorkflowId() {
        String url = page.url();
        if (url.contains("id_workflow=")) {
            return url.split("id_workflow=")[1].split("&")[0].split("#")[0];
        }
        return null;
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
