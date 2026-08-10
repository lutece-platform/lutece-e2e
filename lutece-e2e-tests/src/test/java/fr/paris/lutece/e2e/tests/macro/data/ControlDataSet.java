package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation d'un controle Forms.
 *
 * <p>Deux familles de controles sont couvertes :</p>
 * <ul>
 *   <li><b>conditionnel</b> ({@code CONDITIONAL}) : l'affichage d'une question cible
 *       ({@code targetQuestionIndex}) est pilote par la reponse d'une question pilote
 *       ({@code pilotQuestionIndex}) comparee a {@code value} ;</li>
 *   <li><b>validation</b> ({@code VALIDATION}) : une regle de validation est posee sur une
 *       question ({@code questionIndex}), avec une {@code value} optionnelle et un
 *       {@code errorMessage} affiche si la saisie est invalide.</li>
 * </ul>
 *
 * <p>Les index referencent la position de la question dans {@code ctx.questions} (ordre d'ajout).
 * Les champs non pertinents pour la famille choisie sont laisses a {@code null}.</p>
 */
public record ControlDataSet(
        Integer pilotQuestionIndex,
        Integer targetQuestionIndex,
        Integer questionIndex,
        String value,
        String errorMessage) {

    /**
     * Controle conditionnel : la question cible s'affiche selon la reponse de la question pilote.
     */
    public static ControlDataSet conditional(int pilotQuestionIndex, int targetQuestionIndex, String value) {
        return new ControlDataSet(pilotQuestionIndex, targetQuestionIndex, null, value, null);
    }

    /**
     * Controle de validation pose sur une question.
     */
    public static ControlDataSet validation(int questionIndex, String value, String errorMessage) {
        return new ControlDataSet(null, null, questionIndex, value, errorMessage);
    }

    /**
     * Defaut conditionnel : pilote = question 0, cible = question 1.
     */
    public static ControlDataSet conditionalDefaults() {
        return conditional(0, 1, "test");
    }

    /**
     * Defaut generique (mirroir des autres jeux de donnees) : un controle de validation sur la
     * premiere question.
     */
    public static ControlDataSet defaults() {
        return validationDefaults();
    }

    /**
     * Defaut validation : regle sur la question 0, avec un message d'erreur.
     */
    public static ControlDataSet validationDefaults() {
        return validation(0, "", "Saisie invalide");
    }
}
