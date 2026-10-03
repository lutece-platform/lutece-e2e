package fr.paris.lutece.e2e.tests.macro.data;

import java.util.List;

/**
 * Choix proposes par une question de type liste (bouton radio, case a cocher, liste deroulante).
 *
 * <p>Une question a liste creee sans choix n'affiche rien en front office et ne peut servir de
 * condition : le validateur de transition qui porte sur une valeur de reponse construit sa liste a
 * partir des choix de la question, et reste donc vide.</p>
 *
 * @param questionTitle titre de la question a completer
 * @param choices       libelles des choix, dans l'ordre d'affichage
 */
public record QuestionChoicesDataSet(String questionTitle, List<String> choices) {

    /**
     * Choix d'une question designee par son titre.
     *
     * @param questionTitle titre de la question
     * @param choices       libelles des choix
     * @return le jeu de donnees correspondant
     */
    public static QuestionChoicesDataSet of(String questionTitle, String... choices) {
        return new QuestionChoicesDataSet(questionTitle, List.of(choices));
    }

    /**
     * Jeu de donnees par defaut : deux choix sur une question nommee.
     *
     * @param questionTitle titre de la question
     * @return le jeu de donnees correspondant
     */
    public static QuestionChoicesDataSet defaults(String questionTitle) {
        return of(questionTitle, "Oui", "Non");
    }
}
