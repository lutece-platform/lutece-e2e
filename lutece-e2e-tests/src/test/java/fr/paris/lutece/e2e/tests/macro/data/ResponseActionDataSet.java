package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour le declenchement d'une action de workflow sur une reponse.
 *
 * <p>{@code actionLabel} = libelle de l'action de workflow a declencher sur le detail d'une reponse
 * (ex: "Valider", "Rejeter"). Si l'action n'est pas presente (aucun workflow associe au formulaire,
 * ou libelle different), la brique {@code RunWorkflowActionOnResponseMacroTest} saute via Assumptions.</p>
 */
public record ResponseActionDataSet(String actionLabel) {

    public static ResponseActionDataSet of(String actionLabel) {
        return new ResponseActionDataSet(actionLabel);
    }

    public static ResponseActionDataSet defaults() {
        return new ResponseActionDataSet("Valider");
    }
}
