package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la verification d'un affichage conditionnel en front office.
 *
 * <p>{@code pilotLabel} : libelle de la question pilote a renseigner ; {@code pilotValue} : valeur a
 * saisir dans la pilote pour declencher (ou non) l'affichage de la cible ; {@code targetLabel} :
 * libelle de la question cible dont on verifie la visibilite ; {@code expectVisible} : visibilite
 * attendue de la cible une fois la pilote renseignee.</p>
 *
 * <p>Le defaut est aligne sur {@code AddConditionalControlMacroTest} (controle conditionnel par
 * defaut) : la cible "Question cible" doit s'afficher quand la pilote "Question pilote" vaut "test".</p>
 */
public record ConditionalCheckDataSet(
        String pilotLabel,
        String pilotValue,
        String targetLabel,
        boolean expectVisible) {

    public static ConditionalCheckDataSet of(String pilotLabel, String pilotValue,
            String targetLabel, boolean expectVisible) {
        return new ConditionalCheckDataSet(pilotLabel, pilotValue, targetLabel, expectVisible);
    }

    public static ConditionalCheckDataSet defaults() {
        return new ConditionalCheckDataSet("Question pilote", "test", "Question cible", true);
    }
}
