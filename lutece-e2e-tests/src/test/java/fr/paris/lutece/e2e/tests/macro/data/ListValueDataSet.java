package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees d'une valeur de liste (reponse possible d'une question a choix : radio, cases, liste).
 *
 * @param questionIndex index de la question dans {@code ctx.questions}
 * @param valueIndex    index de la valeur dans l'ordre de creation des valeurs de cette question (modification,
 *                      suppression) ; ignore a la creation
 * @param title         libelle affiche
 * @param value         valeur enregistree
 */
public record ListValueDataSet(int questionIndex, int valueIndex, String title, String value) {

    /** Nouvelle valeur pour la question d'index donne. */
    public static ListValueDataSet create(int questionIndex, String title, String value) {
        return new ListValueDataSet(questionIndex, -1, title, value);
    }

    /** Valeur existante (n-ieme valeur creee sur la question), avec nouveau libelle et nouvelle valeur. */
    public static ListValueDataSet modify(int questionIndex, int valueIndex, String newTitle, String newValue) {
        return new ListValueDataSet(questionIndex, valueIndex, newTitle, newValue);
    }

    /** Valeur existante (n-ieme valeur creee sur la question), pour suppression. */
    public static ListValueDataSet target(int questionIndex, int valueIndex) {
        return new ListValueDataSet(questionIndex, valueIndex, null, null);
    }
}
