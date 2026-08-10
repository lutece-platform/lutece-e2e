package fr.paris.lutece.e2e.tests.macro.data;

import java.util.List;

/**
 * Jeu de donnees pour l'ajout d'une question.
 *
 * <p>{@code stepIndex} cible l'etape dans {@code ctx.steps} (null = premiere etape). Les champs
 * additionnels ne sont utilises que pour les types qui les exigent : {@code height} (TEXTAREA),
 * {@code fileMaxSize}/{@code maxFiles} (FILE, IMAGE), {@code commentCode}/{@code commentText}
 * (COMMENT), {@code choices} (RADIO, CHECKBOX, SELECT, SELECT_ORDER).</p>
 */
public record QuestionDataSet(
        QuestionType type,
        String title,
        Integer stepIndex,
        Integer height,
        Long fileMaxSize,
        Integer maxFiles,
        String commentCode,
        String commentText,
        List<String> choices) {

    public static QuestionDataSet of(QuestionType type, String title) {
        return new QuestionDataSet(type, title, null, 5, 10_485_760L, 1, "code", "Commentaire", List.of("Option 1", "Option 2"));
    }

    public static QuestionDataSet defaults() {
        return of(QuestionType.TEXT, "Question texte");
    }

    public QuestionDataSet onStep(int index) {
        return new QuestionDataSet(type, title, index, height, fileMaxSize, maxFiles, commentCode, commentText, choices);
    }
}
