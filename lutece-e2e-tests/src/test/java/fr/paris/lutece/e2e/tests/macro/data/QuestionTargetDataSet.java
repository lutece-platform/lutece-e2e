package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour cibler une question existante dans {@code ctx.questions} (par index) et,
 * le cas echeant, lui appliquer un nouveau titre.
 *
 * <p>{@code questionIndex} designe la position de la question dans {@code ctx.questions} (0 = premiere
 * question ajoutee). {@code newTitle} n'est utilise que par les briques de modification.</p>
 */
public record QuestionTargetDataSet(int questionIndex, String newTitle) {

    public static QuestionTargetDataSet of(int questionIndex) {
        return new QuestionTargetDataSet(questionIndex, "Question modifiee");
    }

    public static QuestionTargetDataSet of(int questionIndex, String newTitle) {
        return new QuestionTargetDataSet(questionIndex, newTitle);
    }

    public static QuestionTargetDataSet defaults() {
        return of(0);
    }
}
