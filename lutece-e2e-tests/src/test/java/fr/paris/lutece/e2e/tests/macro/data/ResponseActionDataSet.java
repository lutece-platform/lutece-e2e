package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour le declenchement d'une action de workflow sur une reponse.
 *
 * <p>{@code actionLabel} est le libelle de l'action a declencher sur le detail d'une reponse. Si
 * l'action n'est pas presente — aucun workflow associe, ou libelle different —, la brique
 * {@code RunWorkflowActionOnResponseMacroTest} saute via Assumptions.</p>
 *
 * <p>{@code etatReponse} designe la reponse a instruire par l'etat dans lequel elle se trouve.
 * Des qu'un formulaire a plusieurs reponses, prendre la premiere venue revient a instruire un
 * dossier au hasard : deux actions enchainees pourraient porter sur des dossiers differents sans
 * que rien ne le signale.</p>
 *
 * @param actionLabel libelle de l'action de workflow
 * @param etatReponse etat de la reponse a ouvrir, ou null pour la premiere du formulaire
 */
public record ResponseActionDataSet(String actionLabel, String etatReponse) {

    /**
     * Action a declencher sur la premiere reponse du formulaire.
     *
     * @param actionLabel libelle de l'action
     * @return le jeu de donnees correspondant
     */
    public static ResponseActionDataSet of(String actionLabel) {
        return new ResponseActionDataSet(actionLabel, null);
    }

    /**
     * Action a declencher sur une reponse se trouvant dans l'etat indique.
     *
     * @param actionLabel libelle de l'action
     * @param etat        etat de la reponse a instruire
     * @return le jeu de donnees correspondant
     */
    public static ResponseActionDataSet surReponseDansEtat(String actionLabel, String etat) {
        return new ResponseActionDataSet(actionLabel, etat);
    }

    /**
     * Jeu de donnees par defaut.
     *
     * @return une action « Valider » sur la premiere reponse
     */
    public static ResponseActionDataSet defaults() {
        return new ResponseActionDataSet("Valider", null);
    }
}
