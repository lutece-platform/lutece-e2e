package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation / verification / suppression d'une transition entre etapes.
 *
 * <p>{@code fromStepIndex} et {@code toStepIndex} ciblent des etapes dans {@code ctx.steps} :
 * l'etape source (non finale) porte la transition vers l'etape cible ("Etape suivante").</p>
 */
public record TransitionDataSet(int fromStepIndex, int toStepIndex) {

    public static TransitionDataSet of(int fromStepIndex, int toStepIndex) {
        return new TransitionDataSet(fromStepIndex, toStepIndex);
    }

    /** Par defaut : transition de la premiere etape (index 0) vers la seconde (index 1). */
    public static TransitionDataSet defaults() {
        return new TransitionDataSet(0, 1);
    }
}
