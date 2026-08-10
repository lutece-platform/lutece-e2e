package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation / configuration d'une etape.
 */
public record StepDataSet(
        String title,
        boolean initial,
        boolean isFinal,
        String description) {

    public static StepDataSet of(String title) {
        return new StepDataSet(title, false, false, "");
    }

    public static StepDataSet initial(String title) {
        return new StepDataSet(title, true, false, "");
    }

    public static StepDataSet finalStep(String title) {
        return new StepDataSet(title, false, true, "");
    }

    public static StepDataSet defaults() {
        return new StepDataSet("Étape initiale", true, true, "");
    }
}
