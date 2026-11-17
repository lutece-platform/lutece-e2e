package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Modification de l'etape d'arrivee d'une transition existante.
 *
 * @param fromStepIndex    index de l'etape source dans {@code ctx.steps}
 * @param currentToIndex   index de l'etape d'arrivee actuelle (identifie la transition)
 * @param newToIndex       index de la nouvelle etape d'arrivee
 */
public record TransitionModifyDataSet(int fromStepIndex, int currentToIndex, int newToIndex) {

    public static TransitionModifyDataSet of(int fromStepIndex, int currentToIndex, int newToIndex) {
        return new TransitionModifyDataSet(fromStepIndex, currentToIndex, newToIndex);
    }
}
