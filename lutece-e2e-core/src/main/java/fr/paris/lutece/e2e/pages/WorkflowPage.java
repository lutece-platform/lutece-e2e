package fr.paris.lutece.e2e.pages;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.core.BrowserManager;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Page Object pour la gestion des workflows Lutece.
 * Basé sur l'implémentation fonctionnelle de lutece-e2e-tests-bo3.
 */
@Dependent
public class WorkflowPage extends BasePage {

    @Inject
    public WorkflowPage(BrowserManager browser) {
        super(browser);
    }

    // === Navigation ===

    public WorkflowPage navigateToList() {
        navigate("/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
        return this;
    }

    public boolean isListDisplayed() {
        waitForLoad();
        return page().url().contains("ManageWorkflow") ||
               page().locator("a:has-text('Créer un workflow'), a:has-text('Créer un workflow')").first().isVisible() ||
               page().locator("text=Gestion des workflows").first().isVisible();
    }

    // === Creation ===

    /**
     * Click the "Create workflow" button which opens an offcanvas panel in Lutece 8.
     * The button is a Bootstrap 5 offcanvas trigger, not a regular link.
     */
    public WorkflowPage clickCreateWorkflow() {
        // In Lutece 8, the create button is an offcanvas component with id 'workflow-create'
        // Try multiple selectors: offcanvas button, regular link, or text-based
        var createButton = page().locator(
            "button[data-bs-target='#workflow-create'], " +
            "[data-bs-toggle='offcanvas']:has-text('Créer'), " +
            "button:has-text('Créer un workflow'), " +
            "a:has-text('Créer un workflow'), " +
            "a:has-text('Creer un workflow')"
        ).first();

        createButton.click();

        // Wait for offcanvas to open and form to be loaded
        // The offcanvas has id 'workflow-create' and form content is in '#create_workflow_page'
        page().locator("#workflow-create.show, .offcanvas.show, input[name='name']").first()
              .waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(10000));

        waitForLoad();
        return this;
    }

    public WorkflowPage fillName(String name) {
        // Wait for form to be ready in offcanvas
        var nameInput = page().locator("input[name='name']");
        nameInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
        nameInput.click();
        nameInput.fill(name);
        return this;
    }

    public WorkflowPage fillDescription(String description) {
        var descInput = page().locator("textarea[name='description']");
        descInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
        descInput.click();
        descInput.fill(description);
        return this;
    }

    public WorkflowPage submitCreate() {
        // The save button has name='save' and text 'Enregistrer' in French
        var saveButton = page().locator("button[name='save'], button:has-text('Enregistrer')").first();
        saveButton.click();
        waitForLoad();

        // Handle AdminMessage page if it appears (success confirmation)
        if (page().url().contains("AdminMessage")) {
            page().locator("a:has-text('OK'), button:has-text('OK')").first().click();
            waitForLoad();
        }

        return this;
    }

    // === Etats ===

    /**
     * Click the "Add state" button. In Lutece 8, this is a regular link.
     * First clicks on the States tab if present.
     */
    public WorkflowPage clickAddState() {
        // D'abord, cliquer sur l'onglet États s'il existe
        var statesTab = page().locator(
            "a:has-text('États'), " +
            "a:has-text('Etats'), " +
            "button:has-text('États'), " +
            "[data-bs-toggle='tab']:has-text('États'), " +
            ".nav-link:has-text('États')"
        ).first();
        if (statesTab.isVisible()) {
            statesTab.click();
            waitForLoad();
        }

        // Puis cliquer sur le bouton "Ajouter un état"
        var addButton = page().locator(
            "a[href*='CreateState.jsp'], " +
            "a:has-text('Ajouter un état'), " +
            "a:has-text('Ajouter un etat')"
        ).first();
        addButton.click();
        waitForLoad();
        return this;
    }

    public WorkflowPage fillStateName(String name) {
        var nameInput = page().locator("input[name='name']");
        nameInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
        nameInput.click();
        nameInput.fill(name);
        return this;
    }

    public WorkflowPage fillStateDescription(String description) {
        var descInput = page().locator("textarea[name='description']");
        descInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
        descInput.click();
        descInput.fill(description);
        return this;
    }

    public WorkflowPage setInitialState(boolean initial) {
        // Try both checkbox name and ARIA role
        var checkbox = page().locator("input[name='is_initial_state'], input[type='checkbox']").first();
        if (initial) {
            checkbox.check();
        } else {
            checkbox.uncheck();
        }
        return this;
    }

    public WorkflowPage submitCreateState() {
        // Extraire l'ID du workflow avant de soumettre
        int workflowId = extractWorkflowIdFromUrl();

        var saveButton = page().locator("button[name='save'], button:has-text('Enregistrer')").first();
        saveButton.click();
        waitForLoad();

        if (page().url().contains("AdminMessage")) {
            page().locator("a:has-text('OK'), button:has-text('OK')").first().click();
            waitForLoad();
        }

        // Retourner sur la page d'édition du workflow si on est revenu à la liste
        if (!isOnWorkflowEditPage() && workflowId > 0) {
            navigateToEdit(workflowId);
        }

        return this;
    }

    // === Actions ===

    /**
     * Click the "Add action" button. In Lutece 8, this is a regular link.
     * First clicks on the Actions tab if present.
     */
    public WorkflowPage clickAddAction() {
        // D'abord, cliquer sur l'onglet Actions s'il existe
        var actionsTab = page().locator(
            "a:has-text('Actions'), " +
            "button:has-text('Actions'), " +
            "[data-bs-toggle='tab']:has-text('Actions'), " +
            ".nav-link:has-text('Actions')"
        ).first();
        if (actionsTab.isVisible()) {
            actionsTab.click();
            waitForLoad();
        }

        // Puis cliquer sur le bouton "Ajouter une action"
        // Utiliser un sélecteur spécifique pour éviter de cliquer sur "Ajouter un état"
        var addButton = page().locator(
            "a[href*='CreateAction.jsp'], " +
            "a:has-text('Ajouter une action')"
        ).first();
        addButton.click();
        waitForLoad();
        return this;
    }

    public WorkflowPage fillActionName(String name) {
        var nameInput = page().locator("input[name='name']");
        nameInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
        nameInput.click();
        nameInput.fill(name);
        return this;
    }

    public WorkflowPage fillActionDescription(String description) {
        var descInput = page().locator("textarea[name='description']");
        descInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
        descInput.click();
        descInput.fill(description);
        return this;
    }

    /**
     * Sélectionne l'état de départ pour l'action.
     * En Lutece 8, c'est un groupe de boutons radio avec labels.
     */
    public WorkflowPage selectInitialState(String stateName) {
        // Trouver le label contenant le nom de l'état dans la section "État de départ"
        // et cliquer dessus pour cocher le radio button associé
        var label = page().locator(
            ".form-check label:has-text('" + stateName + "'), " +
            "label.form-check-label:has-text('" + stateName + "')"
        ).first();

        if (label.isVisible()) {
            label.click();
        } else {
            // Fallback: essayer de cocher directement le radio via son texte adjacent
            page().getByText(stateName, new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).first().click();
        }
        return this;
    }

    /**
     * Sélectionne l'état d'arrivée pour l'action.
     * En Lutece 8, c'est un select dropdown.
     */
    public WorkflowPage selectFinalState(String stateName) {
        // Sélectionner dans le dropdown "État d'arrivée"
        var select = page().locator("select[name='id_state_after'], #id_state_after");
        if (select.isVisible()) {
            select.selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(stateName));
        }
        return this;
    }

    public WorkflowPage submitCreateAction() {
        // Extraire l'ID du workflow avant de soumettre
        int workflowId = extractWorkflowIdFromUrl();

        var saveButton = page().locator("button[name='save'], button:has-text('Enregistrer')").first();
        saveButton.click();
        waitForLoad();

        if (page().url().contains("AdminMessage")) {
            page().locator("a:has-text('OK'), button:has-text('OK')").first().click();
            waitForLoad();
        }

        // Retourner sur la page d'édition du workflow si on est revenu à la liste
        if (!isOnWorkflowEditPage() && workflowId > 0) {
            navigateToEdit(workflowId);
        }

        return this;
    }

    // === Configuration taches ===

    /**
     * Clique sur le bouton "Modifier l'action" pour une action donnée.
     * En Lutece 8, navigue vers l'onglet Actions, trouve l'action par son nom,
     * puis clique sur le lien de modification correspondant.
     */
    public WorkflowPage clickConfigureAction(String actionName) {
        // S'assurer d'être sur la page d'édition du workflow
        if (!isOnWorkflowEditPage()) {
            throw new RuntimeException("Pas sur la page d'edition du workflow. Naviguez d'abord vers le workflow.");
        }

        // Cliquer sur l'onglet Actions
        var actionsTab = page().getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Actions"));
        if (actionsTab.isVisible()) {
            actionsTab.click();
            waitForLoad();
        }

        // Stratégie 1: Trouver la ligne contenant le nom de l'action et cliquer sur "Modifier l'action"
        var actionRow = page().locator("tr:has-text('" + actionName + "'), .list-group-item:has-text('" + actionName + "'), .card:has-text('" + actionName + "')").first();

        if (actionRow.isVisible()) {
            var modifyLink = actionRow.locator("a:has-text('Modifier'), a[title*='Modifier'], .btn:has-text('Modifier')").first();
            if (modifyLink.isVisible()) {
                modifyLink.click();
                waitForLoad();
                return this;
            }
        }

        // Stratégie 2: Cliquer directement sur le lien "Modifier l'action" (premier trouvé)
        var modifyActionLink = page().getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Modifier l'action"));
        if (modifyActionLink.count() > 0) {
            modifyActionLink.first().click();
            waitForLoad();
            return this;
        }

        // Stratégie 3: Chercher un lien avec l'icône de modification ou le texte partiel
        var anyModifyLink = page().locator(
            "a[href*='ModifyAction'], " +
            "a[href*='ConfigureAction'], " +
            "a:has(.fa-edit), " +
            "a:has(.fa-cog), " +
            "a:has(.ti-settings)"
        ).first();

        if (anyModifyLink.isVisible()) {
            anyModifyLink.click();
            waitForLoad();
            return this;
        }

        throw new RuntimeException("Impossible de trouver le lien 'Modifier l'action' pour: " + actionName);
    }

    /**
     * Ajoute une tâche à l'action courante.
     * En Lutece 8, le select a le label "Nouvelle tâche" et le bouton est "Insérer".
     */
    public WorkflowPage addTask(String taskType) {
        page().getByLabel("Nouvelle tâche").selectOption(taskType);
        page().getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Insérer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Configure la tâche de mise à jour du statut de publication.
     * Cette tâche permet de définir si une réponse de formulaire sera publiée ou dépubliée.
     * Les valeurs possibles sont "Publié" ou "Dépublié".
     * Note: Le changement d'état workflow se fait automatiquement par l'action elle-même.
     */
    public WorkflowPage configureTaskPublicationStatus(boolean published) {
        // Cliquer sur "Modifier le workflow" pour accéder à la config de la tâche
        // Utiliser first() car il peut y avoir plusieurs tâches avec ce lien
        page().getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Modifier le workflow")).first().click();
        waitForLoad();

        // Sélectionner le statut de publication via radio button
        // Utiliser first() car il peut y avoir plusieurs radios avec le même nom (ex: si plusieurs tâches)
        String labelName = published ? "Publié" : "Dépublié";
        page().getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName(labelName)).first().check();

        // Enregistrer
        page().getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * @deprecated Utilisez configureTaskPublicationStatus(boolean published) à la place.
     * Le paramètre stateName était mal compris - cette tâche configure le statut de publication,
     * pas l'état workflow.
     */
    @Deprecated
    public WorkflowPage configureTaskStatusUpdate(String stateName) {
        // Mapping des anciens noms vers published/dépublié
        boolean published = !"Dépublié".equalsIgnoreCase(stateName) &&
                           !"Depublie".equalsIgnoreCase(stateName) &&
                           !"unpublished".equalsIgnoreCase(stateName);
        return configureTaskPublicationStatus(published);
    }

    // === Activation ===

    public WorkflowPage activateWorkflow(String workflowName) {
        waitForLoad();
        var workflowLink = page().locator("a:has-text('" + workflowName + "')").first();
        workflowLink.locator("xpath=ancestor::*[contains(@class, 'row') or contains(@class, 'list-group-item')][1]")
                .locator("button.btn-success, a.btn-success, button:has(.fa-play), a:has(.fa-play)")
                .first().click();
        waitForLoad();
        return this;
    }

    public WorkflowPage deactivateWorkflow(String workflowName) {
        waitForLoad();
        var workflowLink = page().locator("a:has-text('" + workflowName + "')").first();
        workflowLink.locator("xpath=ancestor::*[contains(@class, 'row') or contains(@class, 'list-group-item')][1]")
                .locator("button.btn-warning, a.btn-warning, button:has(.fa-pause), a:has(.fa-pause)")
                .first().click();
        waitForLoad();
        return this;
    }

    // === Utilitaires ===

    public int extractWorkflowIdFromUrl() {
        String url = page().url();
        if (url.contains("id_workflow=")) {
            return Integer.parseInt(url.split("id_workflow=")[1].split("&")[0].split("#")[0]);
        }
        return -1;
    }

    public boolean workflowExists(String workflowName) {
        return page().locator("a:has-text('" + workflowName + "')").count() > 0;
    }

    /**
     * Ouvre un workflow pour édition depuis la liste.
     */
    public WorkflowPage openWorkflowForEdit(String workflowName) {
        // Cliquer sur le nom du workflow pour ouvrir la page d'édition
        var workflowLink = page().locator("a:has-text('" + workflowName + "')").first();
        workflowLink.click();
        waitForLoad();
        return this;
    }

    /**
     * Vérifie si on est sur la page d'édition d'un workflow.
     */
    public boolean isOnWorkflowEditPage() {
        String url = page().url();
        return url.contains("ModifyWorkflow") || url.contains("id_workflow=");
    }

    /**
     * Navigue vers la page d'édition d'un workflow par son ID.
     */
    public WorkflowPage navigateToEdit(int workflowId) {
        navigate("/jsp/admin/plugins/workflow/ModifyWorkflow.jsp?id_workflow=" + workflowId);
        waitForLoad();
        return this;
    }
}
