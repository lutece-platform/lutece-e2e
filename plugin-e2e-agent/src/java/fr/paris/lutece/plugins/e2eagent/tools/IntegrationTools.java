package fr.paris.lutece.plugins.e2eagent.tools;

import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.e2e.actions.AuthActions;
import fr.paris.lutece.e2e.actions.FormsActions;
import fr.paris.lutece.e2e.actions.WorkflowActions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Tool LangChain4j pour executer la suite d'integration complete en une seule operation.
 * Reproduit le comportement de ContainerIntegrationSuite (sans ContainerSetup et RbacConfigurationTest) :
 * 1. WorkflowCreationTest : creation workflow + etats + action + tache + activation
 * 2. FormsCreationTest : creation formulaire + etapes + questions + transition + publication
 * 3. FormsSubmissionTest : soumission du formulaire en front office
 */
@ApplicationScoped
public class IntegrationTools {

    @Inject
    AuthActions authActions;

    @Inject
    WorkflowActions workflowActions;

    @Inject
    FormsActions formsActions;

    @Tool("Execute la suite d'integration complete en une seule operation. " +
          "Cree un workflow (2 etats, 1 action, tache de publication, activation), " +
          "cree un formulaire avec ce workflow (2 etapes, questions texte/nombre/date + commentaire, transition), " +
          "publie le formulaire, puis le soumet en front office. " +
          "Equivalent de la suite de tests WorkflowCreation + FormsCreation + FormsSubmission.")
    public String runIntegrationSuite() {
        StringBuilder report = new StringBuilder();
        report.append("=== Suite d'integration ===\n\n");

        String suffix = String.valueOf(System.currentTimeMillis() % 100000);

        // Noms des entites (alignes sur les tests pipeline)
        String workflowName = "Test integration wkf " + suffix;
        String workflowDesc = "Test d'integration workflow";
        String stateInitial = "Etat initial";
        String stateFinal = "Etat final";
        String actionName = "Valider";
        String actionDesc = "Validation";
        String formTitle = "Forms Test integration " + suffix;
        String stepInitial = "Etape Initial";
        String stepFinal = "Etape finale";
        String questionText = "Test question text";
        String questionNumber = "Test nombre";
        String questionDate = "date";
        String commentText = "Commentaire Test";
        String submitText = "Text";
        String submitNumber = "22";
        String submitDate = "2026-02-04";

        // --- 1. Verification authentification ---
        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }
        report.append("0. Authentification OK\n\n");

        // --- 2. WorkflowCreationTest ---
        report.append("--- Creation du workflow ---\n");

        // 2.1 Creer le workflow
        var createWfResult = workflowActions.createWorkflow(workflowName, workflowDesc);
        if (!createWfResult.isSuccess()) {
            return report + "ECHEC creation workflow: " + createWfResult.getMessage();
        }
        report.append("1. Workflow '").append(workflowName).append("' cree (ID: ")
                .append(createWfResult.getData().id()).append(")\n");

        // 2.2 Ajouter l'etat initial
        var stateInitResult = workflowActions.addState(stateInitial, stateInitial, true);
        if (!stateInitResult.isSuccess()) {
            return report + "ECHEC ajout etat initial: " + stateInitResult.getMessage();
        }
        report.append("2. Etat '").append(stateInitial).append("' ajoute (initial)\n");

        // 2.3 Ajouter l'etat final
        var stateFinalResult = workflowActions.addState(stateFinal, stateFinal, false);
        if (!stateFinalResult.isSuccess()) {
            return report + "ECHEC ajout etat final: " + stateFinalResult.getMessage();
        }
        report.append("3. Etat '").append(stateFinal).append("' ajoute\n");

        // 2.4 Ajouter l'action
        var actionResult = workflowActions.addAction(actionName, actionDesc, stateInitial, stateFinal);
        if (!actionResult.isSuccess()) {
            return report + "ECHEC ajout action: " + actionResult.getMessage();
        }
        report.append("4. Action '").append(actionName).append("' ajoutee (")
                .append(stateInitial).append(" -> ").append(stateFinal).append(")\n");

        // 2.5 Configurer la tache de publication
        var taskResult = workflowActions.configurePublicationStatusTask(actionName, true);
        if (!taskResult.isSuccess()) {
            return report + "ECHEC configuration tache: " + taskResult.getMessage();
        }
        report.append("5. Tache de publication configuree sur '").append(actionName).append("'\n");

        // 2.6 Activer le workflow
        var activateResult = workflowActions.activateWorkflow(workflowName);
        if (!activateResult.isSuccess()) {
            return report + "ECHEC activation workflow: " + activateResult.getMessage();
        }
        report.append("6. Workflow '").append(workflowName).append("' active\n\n");

        // --- 3. FormsCreationTest ---
        report.append("--- Creation du formulaire ---\n");

        // 3.1 Creer le formulaire avec le workflow
        var formResult = formsActions.createForm(formTitle, workflowName);
        if (!formResult.isSuccess()) {
            return report + "ECHEC creation formulaire: " + formResult.getMessage();
        }
        report.append("7. Formulaire '").append(formTitle).append("' cree (ID: ")
                .append(formResult.getData().id()).append(")\n");

        // 3.2 Ajouter l'etape initiale (non finale)
        var step1Result = formsActions.addStep(stepInitial, false);
        if (!step1Result.isSuccess()) {
            return report + "ECHEC ajout etape initiale: " + step1Result.getMessage();
        }
        report.append("8. Etape '").append(stepInitial).append("' ajoutee\n");

        // 3.3 Ajouter l'etape finale
        var step2Result = formsActions.addStep(stepFinal, true);
        if (!step2Result.isSuccess()) {
            return report + "ECHEC ajout etape finale: " + step2Result.getMessage();
        }
        report.append("9. Etape finale '").append(stepFinal).append("' ajoutee\n");

        // 3.4 Ajouter question texte sur etape initiale
        var textQResult = formsActions.addTextQuestion(stepInitial, questionText);
        if (!textQResult.isSuccess()) {
            return report + "ECHEC ajout question texte: " + textQResult.getMessage();
        }
        report.append("10. Question texte '").append(questionText).append("' ajoutee sur '")
                .append(stepInitial).append("'\n");

        // 3.5 Ajouter question nombre sur etape initiale
        var numberQResult = formsActions.addNumberQuestion(stepInitial, questionNumber);
        if (!numberQResult.isSuccess()) {
            return report + "ECHEC ajout question nombre: " + numberQResult.getMessage();
        }
        report.append("11. Question nombre '").append(questionNumber).append("' ajoutee sur '")
                .append(stepInitial).append("'\n");

        // 3.6 Ajouter question date sur etape initiale
        var dateQResult = formsActions.addDateQuestion(stepInitial, questionDate);
        if (!dateQResult.isSuccess()) {
            return report + "ECHEC ajout question date: " + dateQResult.getMessage();
        }
        report.append("12. Question date '").append(questionDate).append("' ajoutee sur '")
                .append(stepInitial).append("'\n");

        // 3.7 Ajouter question commentaire sur etape finale
        var commentQResult = formsActions.addTextQuestion(stepFinal, commentText);
        if (!commentQResult.isSuccess()) {
            return report + "ECHEC ajout question commentaire: " + commentQResult.getMessage();
        }
        report.append("13. Question commentaire '").append(commentText).append("' ajoutee sur '")
                .append(stepFinal).append("'\n");

        // 3.8 Decocher Finale sur l'etape initiale
        var uncheckResult = formsActions.uncheckStepFinaleByName(stepInitial);
        if (!uncheckResult.isSuccess()) {
            return report + "ECHEC decochage Finale: " + uncheckResult.getMessage();
        }
        report.append("14. Etape '").append(stepInitial).append("' modifiee (non finale)\n");

        // 3.9 Configurer la transition
        var transitionResult = formsActions.configureStepTransition(stepInitial);
        if (!transitionResult.isSuccess()) {
            return report + "ECHEC configuration transition: " + transitionResult.getMessage();
        }
        report.append("15. Transition ").append(stepInitial).append(" -> ").append(stepFinal)
                .append(" configuree\n");

        // 3.10 Publier le formulaire
        var publishResult = formsActions.publishForm(formTitle, "today");
        if (!publishResult.isSuccess()) {
            return report + "ECHEC publication formulaire: " + publishResult.getMessage();
        }
        report.append("16. Formulaire '").append(formTitle).append("' publie\n\n");

        // --- 4. FormsSubmissionTest ---
        report.append("--- Soumission front office ---\n");

        var submitResult = formsActions.submitFormFrontOffice(
                formTitle, questionText, submitText, questionNumber, submitNumber, submitDate);
        if (!submitResult.isSuccess()) {
            return report + "ECHEC soumission FO: " + submitResult.getMessage();
        }
        report.append("17. Formulaire soumis en front office\n\n");

        report.append("=== Suite d'integration terminee avec succes! ===\n");
        report.append("Workflow: ").append(workflowName).append("\n");
        report.append("Formulaire: ").append(formTitle).append("\n");
        return report.toString();
    }
}
