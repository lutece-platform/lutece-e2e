package fr.paris.lutece.e2e.tests.macro.data;

/** Jeu de donnees pour la creation d'un workflow. */
public record WorkflowDataSet(String name, String description) {

    public static WorkflowDataSet defaults() {
        return new WorkflowDataSet("Macro Workflow", "Workflow macro E2E");
    }

    public WorkflowDataSet withName(String newName) {
        return new WorkflowDataSet(newName, description);
    }
}
