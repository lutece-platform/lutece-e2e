package fr.paris.lutece.e2e.actions;

import fr.paris.lutece.e2e.core.ActionResult;
import fr.paris.lutece.e2e.core.BrowserSession;
import fr.paris.lutece.e2e.pages.WorkflowPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Actions de gestion des workflows.
 * Utilisable par les tests JUnit et les tools LangChain4j.
 */
@ApplicationScoped
public class WorkflowActions {

    private static final Logger LOG = LogManager.getLogger(WorkflowActions.class);

    @Inject
    BrowserSession browser;

    @Inject
    WorkflowPage workflowPage;

    /**
     * Informations sur un workflow cree.
     */
    public record WorkflowInfo(int id, String name, String description) {}

    /**
     * Informations sur un etat.
     */
    public record StateInfo(String name, boolean isInitial) {}

    /**
     * Informations sur une action.
     */
    public record ActionInfo(String name, String fromState, String toState) {}

    // Stocke l'ID du workflow en cours d'édition
    private int currentWorkflowId = -1;
    private String currentWorkflowName = null;

    /**
     * Cree un nouveau workflow.
     */
    public ActionResult<WorkflowInfo> createWorkflow(String name, String description) {
        LOG.info("Creation du workflow: {}", name);

        try {
            workflowPage.navigateToList()
                    .clickCreateWorkflow()
                    .fillName(name)
                    .fillDescription(description)
                    .submitCreate();

            int workflowId = workflowPage.extractWorkflowIdFromUrl();
            if (workflowId > 0) {
                currentWorkflowId = workflowId;
                currentWorkflowName = name;
                LOG.info("Workflow cree avec ID: {}", workflowId);
                // Ne pas naviguer vers la liste - rester sur la page d'édition
                return ActionResult.success(
                        new WorkflowInfo(workflowId, name, description),
                        "Workflow '" + name + "' cree avec succes (ID: " + workflowId + ")",
                        browser.screenshot("workflow-created-" + workflowId));
            }

            // Si l'ID n'est pas dans l'URL, chercher le workflow et naviguer vers son édition
            workflowPage.navigateToList();
            if (workflowPage.workflowExists(name)) {
                currentWorkflowName = name;
                // Cliquer sur le workflow pour aller sur sa page d'édition
                workflowPage.openWorkflowForEdit(name);
                currentWorkflowId = workflowPage.extractWorkflowIdFromUrl();
                return ActionResult.success(
                        new WorkflowInfo(currentWorkflowId, name, description),
                        "Workflow '" + name + "' cree (ID: " + currentWorkflowId + ")",
                        browser.screenshot("workflow-created"));
            }

            return ActionResult.failure("Workflow non cree",
                    browser.screenshot("workflow-creation-failed"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la creation du workflow", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("workflow-creation-error"));
        }
    }

    /**
     * Ajoute un etat au workflow.
     */
    public ActionResult<StateInfo> addState(String stateName, String description, boolean isInitial) {
        LOG.info("Ajout de l'etat: {} (initial: {})", stateName, isInitial);

        try {
            // S'assurer d'être sur la page d'édition du workflow
            if (!workflowPage.isOnWorkflowEditPage()) {
                if (currentWorkflowId > 0) {
                    LOG.info("Navigation vers la page d'edition du workflow {}", currentWorkflowId);
                    workflowPage.navigateToEdit(currentWorkflowId);
                } else if (currentWorkflowName != null) {
                    LOG.info("Recherche du workflow '{}' dans la liste", currentWorkflowName);
                    workflowPage.navigateToList();
                    workflowPage.openWorkflowForEdit(currentWorkflowName);
                    currentWorkflowId = workflowPage.extractWorkflowIdFromUrl();
                } else {
                    return ActionResult.failure(
                            "Aucun workflow en cours d'edition. Creez d'abord un workflow.",
                            browser.screenshot("no-workflow-context"));
                }
            }

            workflowPage.clickAddState()
                    .fillStateName(stateName)
                    .fillStateDescription(description)
                    .setInitialState(isInitial)
                    .submitCreateState();

            LOG.info("Etat '{}' ajoute", stateName);
            return ActionResult.success(
                    new StateInfo(stateName, isInitial),
                    "Etat '" + stateName + "' ajoute" + (isInitial ? " (initial)" : ""),
                    browser.screenshot("state-added-" + stateName.replace(" ", "-")));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de l'etat", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("state-add-error"));
        }
    }

    /**
     * Ajoute une action au workflow.
     */
    public ActionResult<ActionInfo> addAction(String actionName, String description,
                                               String fromState, String toState) {
        LOG.info("Ajout de l'action: {} ({} -> {})", actionName, fromState, toState);

        try {
            // S'assurer d'être sur la page d'édition du workflow
            if (!workflowPage.isOnWorkflowEditPage()) {
                if (currentWorkflowId > 0) {
                    LOG.info("Navigation vers la page d'edition du workflow {}", currentWorkflowId);
                    workflowPage.navigateToEdit(currentWorkflowId);
                } else if (currentWorkflowName != null) {
                    LOG.info("Recherche du workflow '{}' dans la liste", currentWorkflowName);
                    workflowPage.navigateToList();
                    workflowPage.openWorkflowForEdit(currentWorkflowName);
                    currentWorkflowId = workflowPage.extractWorkflowIdFromUrl();
                } else {
                    return ActionResult.failure(
                            "Aucun workflow en cours d'edition. Creez d'abord un workflow.",
                            browser.screenshot("no-workflow-context"));
                }
            }

            workflowPage.clickAddAction()
                    .fillActionName(actionName)
                    .fillActionDescription(description)
                    .selectInitialState(fromState)
                    .selectFinalState(toState)
                    .submitCreateAction();

            LOG.info("Action '{}' ajoutee", actionName);
            return ActionResult.success(
                    new ActionInfo(actionName, fromState, toState),
                    "Action '" + actionName + "' ajoutee (" + fromState + " -> " + toState + ")",
                    browser.screenshot("action-added-" + actionName.replace(" ", "-")));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de l'action", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("action-add-error"));
        }
    }

    /**
     * Configure une tache de mise a jour du statut de publication pour une action.
     * Cette tâche permet de publier ou dépublier automatiquement une réponse de formulaire.
     * @param actionName Nom de l'action à configurer
     * @param published true pour publier, false pour dépublier
     */
    public ActionResult<Void> configurePublicationStatusTask(String actionName, boolean published) {
        LOG.info("Configuration de la tache de MAJ statut publication pour '{}' - publié: {}", actionName, published);

        try {
            // TOUJOURS naviguer vers la page d'édition du workflow pour s'assurer d'être au bon endroit
            if (currentWorkflowId > 0) {
                LOG.info("Navigation forcée vers la page d'edition du workflow {}", currentWorkflowId);
                workflowPage.navigateToEdit(currentWorkflowId);
            } else if (currentWorkflowName != null) {
                LOG.info("Recherche du workflow '{}' dans la liste", currentWorkflowName);
                workflowPage.navigateToList();
                workflowPage.openWorkflowForEdit(currentWorkflowName);
                currentWorkflowId = workflowPage.extractWorkflowIdFromUrl();
            } else {
                return ActionResult.failure(
                        "Aucun workflow en cours d'edition. Creez ou selectionnez d'abord un workflow.",
                        browser.screenshot("no-workflow-context"));
            }

            workflowPage.clickConfigureAction(actionName);
            workflowPage.addTask("modifyUpdateStatusTask");
            workflowPage.configureTaskPublicationStatus(published);

            String statusText = published ? "Publié" : "Dépublié";
            return ActionResult.success(null,
                    "Tache de MAJ statut publication configuree pour '" + actionName + "' -> " + statusText,
                    browser.screenshot("task-configured"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la configuration de la tache", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("task-config-error"));
        }
    }

    /**
     * Active un workflow.
     */
    public ActionResult<Void> activateWorkflow(String workflowName) {
        LOG.info("Activation du workflow: {}", workflowName);

        try {
            workflowPage.navigateToList()
                    .activateWorkflow(workflowName);

            return ActionResult.success(null,
                    "Workflow '" + workflowName + "' active",
                    browser.screenshot("workflow-activated"));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'activation du workflow", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("workflow-activation-error"));
        }
    }

    /**
     * Desactive un workflow.
     */
    public ActionResult<Void> deactivateWorkflow(String workflowName) {
        LOG.info("Desactivation du workflow: {}", workflowName);

        try {
            workflowPage.navigateToList()
                    .deactivateWorkflow(workflowName);

            return ActionResult.success(null,
                    "Workflow '" + workflowName + "' desactive",
                    browser.screenshot("workflow-deactivated"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la desactivation du workflow", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("workflow-deactivation-error"));
        }
    }

    /**
     * Sélectionne un workflow par son nom et navigue vers sa page d'édition.
     */
    public void selectWorkflow(String workflowName) {
        LOG.info("Selection du workflow: {}", workflowName);
        workflowPage.navigateToList();
        workflowPage.openWorkflowForEdit(workflowName);
        currentWorkflowId = workflowPage.extractWorkflowIdFromUrl();
        currentWorkflowName = workflowName;
        LOG.info("Workflow selectionne - ID: {}, Nom: {}", currentWorkflowId, currentWorkflowName);
    }

    /**
     * Navigue vers la liste des workflows.
     */
    public ActionResult<Void> navigateToList() {
        try {
            workflowPage.navigateToList();
            if (workflowPage.isListDisplayed()) {
                return ActionResult.success(null, "Liste des workflows affichee",
                        browser.screenshot("workflow-list"));
            }
            return ActionResult.failure("Page liste non affichee",
                    browser.screenshot("workflow-list-error"));
        } catch (Exception e) {
            return ActionResult.failure("Erreur: " + e.getMessage());
        }
    }

}
