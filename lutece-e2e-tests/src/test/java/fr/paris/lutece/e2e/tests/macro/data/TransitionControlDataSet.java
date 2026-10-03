package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour conditionner une transition entre deux etapes.
 *
 * <p>Une transition conditionnee n'est empruntee que si la reponse a une question de l'etape
 * source satisfait la condition : c'est ce qui rend une etape conditionnelle, le parcours
 * bifurquant selon ce que l'usager a saisi.</p>
 *
 * @param fromStepIndex       position, dans {@code ctx.steps}, de l'etape portant la transition
 * @param pilotQuestionIndex  position, dans {@code ctx.questions}, de la question qui conditionne
 * @param value               valeur attendue pour que la transition soit empruntee
 */
public record TransitionControlDataSet(int fromStepIndex, int pilotQuestionIndex, String value) {

    /**
     * Condition portant sur une question de l'etape source.
     *
     * @param fromStepIndex      position de l'etape portant la transition
     * @param pilotQuestionIndex position de la question qui conditionne
     * @param value              valeur attendue
     * @return le jeu de donnees correspondant
     */
    public static TransitionControlDataSet of(int fromStepIndex, int pilotQuestionIndex, String value) {
        return new TransitionControlDataSet(fromStepIndex, pilotQuestionIndex, value);
    }

    /**
     * Jeu de donnees par defaut : premiere etape, premiere question.
     *
     * @return le jeu de donnees par defaut
     */
    public static TransitionControlDataSet defaults() {
        return of(0, 0, "oui");
    }
}
