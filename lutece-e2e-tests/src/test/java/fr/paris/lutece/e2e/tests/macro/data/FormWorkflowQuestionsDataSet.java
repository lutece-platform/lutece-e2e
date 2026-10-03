package fr.paris.lutece.e2e.tests.macro.data;

import java.util.List;

/**
 * Questions du formulaire soumises a l'usager lors d'une demande de correction ou de complement.
 *
 * <p>Ces deux actions ne renvoient pas l'usager vers tout le formulaire : elles ne lui rouvrent que
 * les questions declarees ici. Sans cette declaration, l'ecran d'execution de l'action ne propose
 * aucune question, et l'usager recoit une demande qui ne porte sur rien.</p>
 *
 * @param pourCorrection titres des questions rouvertes lors d'une demande de correction
 * @param pourComplement titres des questions rouvertes lors d'une demande de complement
 */
public record FormWorkflowQuestionsDataSet(List<String> pourCorrection, List<String> pourComplement) {

    /**
     * Declare les memes questions pour la correction et pour le complement.
     *
     * @param titres titres des questions concernees
     * @return le jeu de donnees correspondant
     */
    public static FormWorkflowQuestionsDataSet memesQuestions(String... titres) {
        return new FormWorkflowQuestionsDataSet(List.of(titres), List.of(titres));
    }

    /**
     * Declare des questions distinctes pour chaque demande.
     *
     * @param pourCorrection titres des questions rouvertes en correction
     * @param pourComplement titres des questions rouvertes en complement
     * @return le jeu de donnees correspondant
     */
    public static FormWorkflowQuestionsDataSet of(List<String> pourCorrection, List<String> pourComplement) {
        return new FormWorkflowQuestionsDataSet(pourCorrection, pourComplement);
    }
}
