package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation d'un controle Forms.
 *
 * <p>Deux familles de controles sont couvertes :</p>
 * <ul>
 *   <li><b>conditionnel</b> ({@code CONDITIONAL}) : l'affichage d'une question cible
 *       ({@code targetQuestionIndex}) est pilote par la reponse d'une question pilote
 *       ({@code pilotQuestionIndex}) comparee a {@code value} ;</li>
 *   <li><b>validation</b> ({@code VALIDATION}) : une regle de validation est posee sur une
 *       question ({@code questionIndex}), avec une {@code value} optionnelle et un
 *       {@code errorMessage} affiche si la saisie est invalide.</li>
 * </ul>
 *
 * <p>Les index referencent la position de la question dans {@code ctx.questions} (ordre d'ajout).
 * Les champs non pertinents pour la famille choisie sont laisses a {@code null}.</p>
 */
public record ControlDataSet(
        Integer pilotQuestionIndex,
        Integer targetQuestionIndex,
        Integer questionIndex,
        String value,
        String errorMessage,
        String validatorName) {

    /** Validateur comparant la reponse a une valeur de liste : celui des questions a choix. */
    public static final String VALIDATEUR_VALEUR_DE_LISTE = "forms.listValueValidator";

    /**
     * Controle conditionnel : la question cible s'affiche selon la reponse de la question pilote.
     */
    public static ControlDataSet conditional(int pilotQuestionIndex, int targetQuestionIndex, String value) {
        return new ControlDataSet(pilotQuestionIndex, targetQuestionIndex, null, value, null, null);
    }

    /**
     * Controle conditionnel pilote par une question a choix.
     *
     * <p>Le type de controle doit etre designe explicitement. Laisse au defaut, le formulaire
     * retient le premier validateur de la liste, qui attend un nombre : la valeur attendue — le
     * libelle d'un choix — n'y entre pas, et le controle n'est pas cree.</p>
     *
     * @param pilotQuestionIndex  rang de la question qui commande l'affichage
     * @param targetQuestionIndex rang de la question affichee ou masquee
     * @param value               libelle du choix declenchant l'affichage
     * @return le jeu de donnees correspondant
     */
    public static ControlDataSet conditionalSurListe(int pilotQuestionIndex, int targetQuestionIndex,
        String value) {
        return new ControlDataSet(pilotQuestionIndex, targetQuestionIndex, null, value, null,
            VALIDATEUR_VALEUR_DE_LISTE);
    }

    /**
     * Controle de validation pose sur une question.
     */
    public static ControlDataSet validation(int questionIndex, String value, String errorMessage) {
        return new ControlDataSet(null, null, questionIndex, value, errorMessage, null);
    }

    /**
     * Defaut conditionnel : pilote = question 0, cible = question 1.
     */
    public static ControlDataSet conditionalDefaults() {
        return conditional(0, 1, "test");
    }

    /**
     * Defaut generique (mirroir des autres jeux de donnees) : un controle de validation sur la
     * premiere question.
     */
    public static ControlDataSet defaults() {
        return validationDefaults();
    }

    /**
     * Defaut validation : regle sur la question 0, avec un message d'erreur.
     *
     * <p>L'expression reguliere est designee explicitement par son libelle : sans cela le formulaire
     * retiendrait la premiere entree de la liste, qui depend du jeu de donnees de l'instance et rendrait
     * la verification front-office non deterministe. Les valeurs de {@code ValidationCheckDataSet} sont
     * calibrees sur cette expression.</p>
     */
    public static ControlDataSet validationDefaults() {
        return validation(0, "Email", "Saisie invalide");
    }
}
