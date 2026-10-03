package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour retenir un choix precis sur une question a liste, en front office.
 *
 * <p>Designer le choix, et non se contenter du premier venu, est ce qui permet de piloter un
 * parcours : c'est la reponse a cette question qui determine vers quelle etape le formulaire
 * bifurque.</p>
 *
 * @param questionLabel libelle de la question, tel qu'affiche en front office
 * @param choiceLabel   libelle du choix a retenir
 */
public record ChoiceSelectionDataSet(String questionLabel, String choiceLabel) {

    /**
     * Choix designe sur une question donnee.
     *
     * @param questionLabel libelle de la question
     * @param choiceLabel   libelle du choix
     * @return le jeu de donnees correspondant
     */
    public static ChoiceSelectionDataSet of(String questionLabel, String choiceLabel) {
        return new ChoiceSelectionDataSet(questionLabel, choiceLabel);
    }
}
