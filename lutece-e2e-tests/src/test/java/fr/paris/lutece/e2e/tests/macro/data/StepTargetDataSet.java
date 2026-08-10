package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees ciblant une etape existante dans {@code ctx.steps} par son index.
 *
 * <p>{@code stepIndex} designe la position de l'etape dans la liste {@code ctx.steps} (0 = premiere
 * etape creee). {@code newTitle} est optionnel (null si non utilise) et sert aux briques qui
 * renomment l'etape ciblee.</p>
 */
public record StepTargetDataSet(int stepIndex, String newTitle) {

    /** Cible l'etape a l'index donne, sans nouveau titre. */
    public static StepTargetDataSet of(int stepIndex) {
        return new StepTargetDataSet(stepIndex, null);
    }

    /** Cible l'etape a l'index donne et fournit un nouveau titre. */
    public static StepTargetDataSet of(int stepIndex, String newTitle) {
        return new StepTargetDataSet(stepIndex, newTitle);
    }

    /** Cible la premiere etape (index 0). */
    public static StepTargetDataSet first() {
        return new StepTargetDataSet(0, null);
    }
}
