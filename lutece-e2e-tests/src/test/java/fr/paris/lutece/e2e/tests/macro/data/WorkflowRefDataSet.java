package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour l'association d'un workflow a un formulaire.
 *
 * <p>{@code workflowName} = libelle de l'option du menu {@code #idWorkflow}. S'il est nul, la
 * brique {@code AssociateWorkflowMacroTest} selectionne le premier workflow non vide disponible
 * (ou saute via Assumptions si aucun workflow n'est propose).</p>
 */
public record WorkflowRefDataSet(String workflowName) {

    public static WorkflowRefDataSet defaults() {
        return new WorkflowRefDataSet(null);
    }

    public static WorkflowRefDataSet of(String workflowName) {
        return new WorkflowRefDataSet(workflowName);
    }
}
