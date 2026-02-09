package fr.paris.lutece.e2e.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.e2e.actions.AuthActions;
import fr.paris.lutece.e2e.actions.WorkflowActions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Tools LangChain4j pour la gestion des workflows.
 */
@ApplicationScoped
public class WorkflowTools {

    @Inject
    WorkflowActions workflowActions;

    @Inject
    AuthActions authActions;

    @Tool("Cree un nouveau workflow dans Lutece. " +
          "Un workflow definit le cycle de vie d'un formulaire. " +
          "Retourne l'ID du workflow cree.")
    public String createWorkflow(
            @P("Nom du workflow") String name,
            @P("Description du workflow") String description) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = workflowActions.createWorkflow(name, description);
        return result.toToolMessage();
    }

    @Tool("Ajoute un etat au workflow. " +
          "Un etat represente une etape dans le cycle de vie (ex: Brouillon, En validation, Publie). " +
          "Le premier etat ajoute doit etre marque comme initial.")
    public String addState(
            @P("Nom de l'etat") String stateName,
            @P("Description de l'etat") String description,
            @P("true si c'est l'etat initial, false sinon") boolean isInitial) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = workflowActions.addState(stateName, description, isInitial);
        return result.toToolMessage();
    }

    @Tool("Ajoute une action au workflow. " +
          "Une action permet de passer d'un etat a un autre (ex: Valider, Rejeter).")
    public String addAction(
            @P("Nom de l'action") String actionName,
            @P("Description de l'action") String description,
            @P("Nom de l'etat source") String fromState,
            @P("Nom de l'etat cible") String toState) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = workflowActions.addAction(actionName, description, fromState, toState);
        return result.toToolMessage();
    }

    @Tool("Configure une tache de publication/depublication automatique pour une action. " +
          "Cette tache permet de publier ou depublier automatiquement une reponse de formulaire " +
          "lorsque l'action est executee. " +
          "Note: Le changement d'etat workflow se fait automatiquement par l'action elle-meme.")
    public String configurePublicationTask(
            @P("Nom du workflow contenant l'action") String workflowName,
            @P("Nom de l'action a configurer") String actionName,
            @P("true pour publier la reponse, false pour la depublier") boolean published) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        // Sélectionner le workflow si spécifié
        if (workflowName != null && !workflowName.isEmpty()) {
            workflowActions.selectWorkflow(workflowName);
        }

        var result = workflowActions.configurePublicationStatusTask(actionName, published);
        return result.toToolMessage();
    }

    @Tool("(Deprecated) Configure une tache de mise a jour de statut. " +
          "Utilisez configurePublicationTask a la place.")
    public String configureStatusTask(
            @P("Nom du workflow contenant l'action") String workflowName,
            @P("Nom de l'action a configurer") String actionName,
            @P("Valeur ignoree - cette tache gere la publication, pas l'etat workflow") String targetState) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        // Sélectionner le workflow si spécifié
        if (workflowName != null && !workflowName.isEmpty()) {
            workflowActions.selectWorkflow(workflowName);
        }

        // Par defaut, on publie
        var result = workflowActions.configurePublicationStatusTask(actionName, true);
        return result.toToolMessage();
    }

    @Tool("Active un workflow pour qu'il soit utilisable par les formulaires.")
    public String activateWorkflow(
            @P("Nom du workflow a activer") String workflowName) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = workflowActions.activateWorkflow(workflowName);
        return result.toToolMessage();
    }

    @Tool("Desactive un workflow.")
    public String deactivateWorkflow(
            @P("Nom du workflow a desactiver") String workflowName) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = workflowActions.deactivateWorkflow(workflowName);
        return result.toToolMessage();
    }

    @Tool("Affiche la liste des workflows existants.")
    public String listWorkflows() {
        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = workflowActions.navigateToList();
        return result.toToolMessage();
    }

    @Tool("Cree un workflow complet avec 3 etats et 2 actions. " +
          "Configure automatiquement une tache de publication sur la premiere action et active le workflow. " +
          "Ideal pour creer rapidement un workflow de validation standard.")
    public String createCompleteWorkflow(
            @P("Nom du workflow") String workflowName,
            @P("Description du workflow") String description,
            @P("Nom du premier etat (etat initial)") String state1,
            @P("Nom du deuxieme etat (intermediaire)") String state2,
            @P("Nom du troisieme etat (final)") String state3,
            @P("Nom de la premiere action (state1 -> state2)") String action1,
            @P("Nom de la deuxieme action (state2 -> state3)") String action2) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        StringBuilder result = new StringBuilder();
        result.append("Creation du workflow complet '").append(workflowName).append("'...\n\n");

        // 1. Creer le workflow
        var createResult = workflowActions.createWorkflow(workflowName, description);
        if (!createResult.isSuccess()) {
            return "ERREUR lors de la creation du workflow: " + createResult.getMessage();
        }
        result.append("1. Workflow cree (ID: ").append(createResult.getData().id()).append(")\n");

        // 2. Ajouter le premier etat (initial)
        var state1Result = workflowActions.addState(state1, "Etat initial", true);
        if (!state1Result.isSuccess()) {
            return "ERREUR lors de l'ajout de l'etat initial: " + state1Result.getMessage();
        }
        result.append("2. Etat '").append(state1).append("' ajoute (initial)\n");

        // 3. Ajouter le deuxieme etat
        var state2Result = workflowActions.addState(state2, "Etat intermediaire", false);
        if (!state2Result.isSuccess()) {
            return "ERREUR lors de l'ajout de l'etat intermediaire: " + state2Result.getMessage();
        }
        result.append("3. Etat '").append(state2).append("' ajoute\n");

        // 4. Ajouter le troisieme etat
        var state3Result = workflowActions.addState(state3, "Etat final", false);
        if (!state3Result.isSuccess()) {
            return "ERREUR lors de l'ajout de l'etat final: " + state3Result.getMessage();
        }
        result.append("4. Etat '").append(state3).append("' ajoute\n");

        // 5. Ajouter la premiere action
        var action1Result = workflowActions.addAction(action1, "Transition de " + state1 + " vers " + state2, state1, state2);
        if (!action1Result.isSuccess()) {
            return "ERREUR lors de l'ajout de l'action 1: " + action1Result.getMessage();
        }
        result.append("5. Action '").append(action1).append("' ajoutee (").append(state1).append(" -> ").append(state2).append(")\n");

        // 6. Ajouter la deuxieme action
        var action2Result = workflowActions.addAction(action2, "Transition de " + state2 + " vers " + state3, state2, state3);
        if (!action2Result.isSuccess()) {
            return "ERREUR lors de l'ajout de l'action 2: " + action2Result.getMessage();
        }
        result.append("6. Action '").append(action2).append("' ajoutee (").append(state2).append(" -> ").append(state3).append(")\n");

        // 7. Configurer la tache de publication sur la premiere action
        var taskResult = workflowActions.configurePublicationStatusTask(action1, true);
        if (!taskResult.isSuccess()) {
            return "ERREUR lors de la configuration de la tache: " + taskResult.getMessage();
        }
        result.append("7. Tache de publication configuree sur '").append(action1).append("'\n");

        // 8. Activer le workflow
        var activateResult = workflowActions.activateWorkflow(workflowName);
        if (!activateResult.isSuccess()) {
            return "ERREUR lors de l'activation: " + activateResult.getMessage();
        }
        result.append("8. Workflow '").append(workflowName).append("' active\n");

        result.append("\n✓ Workflow complet cree et active avec succes!");
        return result.toString();
    }

    @Tool("Diagnostic: analyse la page de gestion des workflows. " +
          "Retourne les elements cliquables disponibles sur la page.")
    public String diagnoseWorkflowPage() {
        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        return workflowActions.diagnoseCurrentPage();
    }

    @Tool("Clique sur le bouton OK si present sur la page AdminMessage.")
    public String clickOkButton() {
        return workflowActions.clickOkIfPresent();
    }
}
