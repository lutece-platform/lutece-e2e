package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Etat d'affichage attendu d'une question sur l'etape front-office courante.
 *
 * @param questionIndex index de la question dans {@code ctx.questions}
 * @param shown         vrai si la question doit etre affichee, faux si elle doit etre rendue mais masquee
 */
public record QuestionShownDataSet(int questionIndex, boolean shown) {

    public static QuestionShownDataSet shown(int questionIndex) {
        return new QuestionShownDataSet(questionIndex, true);
    }

    public static QuestionShownDataSet hidden(int questionIndex) {
        return new QuestionShownDataSet(questionIndex, false);
    }
}
