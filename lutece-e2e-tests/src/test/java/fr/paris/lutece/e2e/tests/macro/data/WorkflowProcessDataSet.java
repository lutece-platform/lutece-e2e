package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Action de workflow a executer sur une reponse, et etat attendu ensuite.
 *
 * @param actionLabel   libelle de l'action (bouton du detail de la reponse)
 * @param expectedState libelle de l'etat d'arrivee, lu dans la multivue
 */
public record WorkflowProcessDataSet(String actionLabel, String expectedState) {

    public static WorkflowProcessDataSet of(String actionLabel, String expectedState) {
        return new WorkflowProcessDataSet(actionLabel, expectedState);
    }
}
