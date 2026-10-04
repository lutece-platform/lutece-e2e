package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la verification de la visibilite d'une question en front office.
 *
 * <p>{@code questionLabel} : libelle de la question observee ; {@code expectVisible} : visibilite
 * attendue sur l'etape actuellement ouverte.</p>
 *
 * <p>Rien n'est saisi ni navigue : le constat porte sur l'etat du parcours tel qu'il est. C'est a
 * l'appelant d'avoir amene le formulaire dans l'etat qu'il veut observer.</p>
 */
public record QuestionVisibilityDataSet(String questionLabel, boolean expectVisible) {

    /**
     * Question attendue affichee.
     *
     * @param questionLabel libelle de la question
     * @return le jeu de donnees correspondant
     */
    public static QuestionVisibilityDataSet affichee(String questionLabel) {
        return new QuestionVisibilityDataSet(questionLabel, true);
    }

    /**
     * Question attendue masquee.
     *
     * @param questionLabel libelle de la question
     * @return le jeu de donnees correspondant
     */
    public static QuestionVisibilityDataSet masquee(String questionLabel) {
        return new QuestionVisibilityDataSet(questionLabel, false);
    }

    public static QuestionVisibilityDataSet defaults() {
        return new QuestionVisibilityDataSet("Question cible", true);
    }
}
