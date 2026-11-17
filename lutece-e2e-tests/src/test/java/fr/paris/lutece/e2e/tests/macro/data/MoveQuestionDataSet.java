package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Deplacement d'une question : etape d'arrivee, groupe d'arrivee eventuel, position.
 *
 * @param questionIndex    index de la question dans {@code ctx.questions}
 * @param targetStepIndex  index de l'etape d'arrivee dans {@code ctx.steps}
 * @param targetGroupIndex index du groupe d'arrivee dans {@code ctx.groups}, -1 pour la racine de l'etape
 * @param position         position 1-based proposee par l'ecran, -1 pour la derniere
 */
public record MoveQuestionDataSet(int questionIndex, int targetStepIndex, int targetGroupIndex, int position) {

    /** Vers la racine d'une etape, en derniere position. */
    public static MoveQuestionDataSet toStep(int questionIndex, int targetStepIndex) {
        return new MoveQuestionDataSet(questionIndex, targetStepIndex, -1, -1);
    }

    /** Dans un groupe (de son etape), en derniere position. */
    public static MoveQuestionDataSet toGroup(int questionIndex, int targetStepIndex, int targetGroupIndex) {
        return new MoveQuestionDataSet(questionIndex, targetStepIndex, targetGroupIndex, -1);
    }
}
