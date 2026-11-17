package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Reponse a cocher en front-office sur une question a choix (bouton radio, case a cocher).
 *
 * @param questionIndex index de la question dans {@code ctx.questions}
 * @param label         libelle exact de la valeur de liste a cocher
 */
public record ChoiceDataSet(int questionIndex, String label) {

    public static ChoiceDataSet of(int questionIndex, String label) {
        return new ChoiceDataSet(questionIndex, label);
    }
}
