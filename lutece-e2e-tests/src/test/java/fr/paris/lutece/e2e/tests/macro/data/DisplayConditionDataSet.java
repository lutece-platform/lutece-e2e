package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Condition d'affichage : la question cible ne s'affiche que si la question pilote (a choix) vaut {@code showValue}.
 *
 * @param pilotQuestionIndex  index de la question pilote dans {@code ctx.questions} (radio, liste, cases)
 * @param targetQuestionIndex index de la question cible dans {@code ctx.questions}
 * @param showValue           libelle de la valeur de la pilote qui affiche la cible
 * @param hideValue           libelle d'une autre valeur de la pilote, qui doit masquer la cible (verification FO)
 */
public record DisplayConditionDataSet(int pilotQuestionIndex, int targetQuestionIndex, String showValue, String hideValue) {

    public static DisplayConditionDataSet of(int pilotQuestionIndex, int targetQuestionIndex, String showValue, String hideValue) {
        return new DisplayConditionDataSet(pilotQuestionIndex, targetQuestionIndex, showValue, hideValue);
    }
}
