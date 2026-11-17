package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees de verification des indicateurs d'une etape.
 *
 * <p>{@code stepIndex} cible l'etape dans {@code ctx.steps} ; {@code initial} et {@code isFinal} sont les
 * etats attendus des badges « Initiale » et « Finale » de la liste des etapes.</p>
 */
public record StepFlagsDataSet(int stepIndex, boolean initial, boolean isFinal) {

    public static StepFlagsDataSet of(int stepIndex, boolean initial, boolean isFinal) {
        return new StepFlagsDataSet(stepIndex, initial, isFinal);
    }

    /** Etape initiale non finale : premiere etape d'un formulaire multi-etapes. */
    public static StepFlagsDataSet initialOnly(int stepIndex) {
        return new StepFlagsDataSet(stepIndex, true, false);
    }

    /** Etape finale non initiale : derniere etape d'un formulaire multi-etapes. */
    public static StepFlagsDataSet finalOnly(int stepIndex) {
        return new StepFlagsDataSet(stepIndex, false, true);
    }
}
