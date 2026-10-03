package fr.paris.lutece.e2e.tests.macro.data;

import java.util.List;

/**
 * Jeu de donnees pour controler l'etat d'une reponse apres le passage d'une action de workflow.
 *
 * @param expectedState    libelle de l'etat attendu sur le detail de la reponse
 * @param expectHistory    exiger au moins une entree dans l'historique des actions
 * @param expectedAction   libelle de l'action attendue dans l'historique, ou null
 * @param expectedTraces   fragments devant figurer dans l'historique : contenu d'une notification,
 *                         commentaire d'instruction, entite affectee
 */
public record ResponseStateDataSet(String expectedState, boolean expectHistory,
                                   String expectedAction, List<String> expectedTraces) {

    /**
     * Controle de l'etat attendu, historique compris.
     *
     * @param expectedState libelle de l'etat attendu
     * @return le jeu de donnees correspondant
     */
    public static ResponseStateDataSet of(String expectedState) {
        return new ResponseStateDataSet(expectedState, true, null, List.of());
    }

    /**
     * Controle de l'etat attendu et de la trace laissee par une action donnee.
     *
     * <p>Exiger le libelle de l'action est nettement plus sur qu'un historique simplement non vide :
     * une entree peut subsister d'un passage anterieur, alors que le libelle atteste que c'est bien
     * cette action-la qui s'est executee.</p>
     *
     * @param expectedState  libelle de l'etat attendu
     * @param expectedAction libelle de l'action attendue dans l'historique
     * @return le jeu de donnees correspondant
     */
    public static ResponseStateDataSet apres(String expectedState, String expectedAction) {
        return new ResponseStateDataSet(expectedState, true, expectedAction, List.of());
    }

    /**
     * Controle de l'etat, de l'action et de ce que ses taches ont depose dans l'historique.
     *
     * <p>C'est ce qui distingue une action reellement jouee d'une action jouee a vide : une tache
     * mal configuree n'empeche pas toujours la transition, mais elle ne laisse alors aucune trace.
     * Le contenu d'une notification destinee a l'usager, en particulier, n'est observable que la.</p>
     *
     * @param expectedState  libelle de l'etat attendu
     * @param expectedAction libelle de l'action attendue dans l'historique
     * @param traces         fragments attendus dans l'historique
     * @return le jeu de donnees correspondant
     */
    public static ResponseStateDataSet avecTraces(String expectedState, String expectedAction,
                                                  String... traces) {
        return new ResponseStateDataSet(expectedState, true, expectedAction, List.of(traces));
    }

    /**
     * Controle de l'etat attendu, sans exiger d'historique.
     *
     * <p>Utile juste apres la soumission, avant qu'aucune action n'ait ete jouee.</p>
     *
     * @param expectedState libelle de l'etat attendu
     * @return le jeu de donnees correspondant
     */
    public static ResponseStateDataSet sansHistorique(String expectedState) {
        return new ResponseStateDataSet(expectedState, false, null, List.of());
    }

    /**
     * Jeu de donnees par defaut.
     *
     * @return un controle sans etat impose ni historique exige
     */
    public static ResponseStateDataSet defaults() {
        return new ResponseStateDataSet(null, false, null, List.of());
    }
}
