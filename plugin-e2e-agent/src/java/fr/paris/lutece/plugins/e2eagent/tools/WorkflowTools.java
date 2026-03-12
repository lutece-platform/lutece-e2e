package fr.paris.lutece.plugins.e2eagent.tools;

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

    @Tool("Cree un workflow complet avec N etats et N actions. " +
          "Configure automatiquement une tache de publication sur la premiere action et active le workflow. " +
          "Supporte un nombre variable d'etats et d'actions.")
    public String createCompleteWorkflow(
            @P("Nom du workflow") String workflowName,
            @P("Description du workflow") String description,
            @P("Liste des etats separes par des virgules. Le premier etat est l'etat initial. " +
               "Exemple: Brouillon, EnValidation, Valide, Archive") String states,
            @P("Liste des actions au format 'NomAction:EtatSource:EtatCible' separes par des virgules. " +
               "Exemple: Soumettre:Brouillon:EnValidation, Valider:EnValidation:Valide") String actions) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        // Parser les etats
        String[] stateNames = states.split(",");
        if (stateNames.length < 2) {
            return "ERREUR: Il faut au moins 2 etats (separes par des virgules)";
        }
        for (int i = 0; i < stateNames.length; i++) {
            stateNames[i] = stateNames[i].trim();
        }

        // Parser les actions
        String[] actionDefs = actions.split(",");
        if (actionDefs.length < 1) {
            return "ERREUR: Il faut au moins 1 action (format 'Nom:EtatSource:EtatCible')";
        }

        StringBuilder result = new StringBuilder();
        result.append("Creation du workflow complet '").append(workflowName).append("'...\n\n");
        int step = 1;

        // 1. Creer le workflow
        var createResult = workflowActions.createWorkflow(workflowName, description);
        if (!createResult.isSuccess()) {
            return "ERREUR lors de la creation du workflow: " + createResult.getMessage();
        }
        result.append(step++).append(". Workflow cree (ID: ").append(createResult.getData().id()).append(")\n");

        // 2. Ajouter les etats
        for (int i = 0; i < stateNames.length; i++) {
            boolean isInitial = (i == 0);
            String desc = isInitial ? "Etat initial" : "Etat " + (i + 1);
            var stateResult = workflowActions.addState(stateNames[i], desc, isInitial);
            if (!stateResult.isSuccess()) {
                return "ERREUR lors de l'ajout de l'etat '" + stateNames[i] + "': " + stateResult.getMessage();
            }
            result.append(step++).append(". Etat '").append(stateNames[i]).append("' ajoute")
                  .append(isInitial ? " (initial)" : "").append("\n");
        }

        // 3. Ajouter les actions
        String firstActionName = null;
        for (int i = 0; i < actionDefs.length; i++) {
            String[] parts = actionDefs[i].trim().split(":");
            if (parts.length != 3) {
                return "ERREUR: Format d'action invalide '" + actionDefs[i].trim() +
                       "'. Attendu: 'NomAction:EtatSource:EtatCible'";
            }
            String actionName = parts[0].trim();
            String fromState = parts[1].trim();
            String toState = parts[2].trim();

            if (i == 0) {
                firstActionName = actionName;
            }

            var actionResult = workflowActions.addAction(actionName,
                    "Transition de " + fromState + " vers " + toState, fromState, toState);
            if (!actionResult.isSuccess()) {
                return "ERREUR lors de l'ajout de l'action '" + actionName + "': " + actionResult.getMessage();
            }
            result.append(step++).append(". Action '").append(actionName)
                  .append("' ajoutee (").append(fromState).append(" -> ").append(toState).append(")\n");
        }

        // 4. Configurer la tache de publication sur la premiere action
        var taskResult = workflowActions.configurePublicationStatusTask(firstActionName, true);
        if (!taskResult.isSuccess()) {
            return "ERREUR lors de la configuration de la tache: " + taskResult.getMessage();
        }
        result.append(step++).append(". Tache de publication configuree sur '").append(firstActionName).append("'\n");

        // 5. Activer le workflow
        var activateResult = workflowActions.activateWorkflow(workflowName);
        if (!activateResult.isSuccess()) {
            return "ERREUR lors de l'activation: " + activateResult.getMessage();
        }
        result.append(step).append(". Workflow '").append(workflowName).append("' active\n");

        result.append("\n✓ Workflow complet cree et active avec succes!");
        return result.toString();
    }

}
