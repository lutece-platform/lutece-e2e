package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour conditionner une transition entre deux etapes.
 *
 * <p>Une transition conditionnee n'est empruntee que si la reponse a une question de l'etape
 * source satisfait la condition : c'est ce qui rend une etape conditionnelle, le parcours
 * bifurquant selon ce que l'usager a saisi.</p>
 *
 * <p>Lorsqu'une etape porte plusieurs transitions, {@code toStepIndex} designe celle qui est
 * conditionnee. Lutece les examine dans l'ordre de leur priorite et emprunte la premiere dont les
 * conditions sont satisfaites : une derniere transition laissee sans condition joue donc le role
 * de sortie par defaut.</p>
 *
 * @param fromStepIndex      position, dans {@code ctx.steps}, de l'etape portant la transition
 * @param toStepIndex        position de l'etape d'arrivee, ou null pour la premiere transition
 * @param pilotQuestionIndex position, dans {@code ctx.questions}, de la question qui conditionne
 * @param validatorName      type de controle, tel qu'expose par l'ecran une fois la question
 *                           validee (ex. {@code forms.patternValidator})
 * @param value              valeur attendue pour que la transition soit empruntee
 */
public record TransitionControlDataSet(int fromStepIndex, Integer toStepIndex,
                                       int pilotQuestionIndex, String validatorName, String value) {

    /** Controle par expression reguliere : la reponse doit correspondre au motif. */
    public static final String VALIDATEUR_EXPRESSION = "forms.patternValidator";

    /** Controle d'unicite : la reponse doit etre unique parmi celles deja enregistrees. */
    public static final String VALIDATEUR_UNICITE = "forms.uniqueValidator";

    /** Controle sur la valeur choisie : la reponse doit etre le choix designe. */
    public static final String VALIDATEUR_VALEUR_LISTE = "forms.listValueValidator";

    /**
     * Condition par expression reguliere sur une question de l'etape source.
     *
     * <p>La valeur designe une expression du referentiel des expressions regulieres, par son
     * identifiant ou par son libelle : l'ecran n'offre pas de saisie libre mais la liste de celles
     * qui y sont declarees.</p>
     *
     * @param fromStepIndex      position de l'etape portant la transition
     * @param pilotQuestionIndex position de la question qui conditionne
     * @param expression         identifiant ou libelle de l'expression attendue
     * @return le jeu de donnees correspondant
     */
    public static TransitionControlDataSet expression(int fromStepIndex, int pilotQuestionIndex,
                                                      String expression) {
        return new TransitionControlDataSet(fromStepIndex, null, pilotQuestionIndex,
            VALIDATEUR_EXPRESSION, expression);
    }

    /**
     * Condition d'unicite : la transition n'est empruntee que si la reponse est unique.
     *
     * <p>Toujours proposee, quel que soit le type de question, la ou les expressions regulieres
     * dependent du referentiel du site.</p>
     *
     * @param fromStepIndex      position de l'etape portant la transition
     * @param pilotQuestionIndex position de la question qui conditionne
     * @param unique             valeur attendue du controle d'unicite
     * @return le jeu de donnees correspondant
     */
    public static TransitionControlDataSet unicite(int fromStepIndex, int pilotQuestionIndex,
                                                   boolean unique) {
        return new TransitionControlDataSet(fromStepIndex, null, pilotQuestionIndex,
            VALIDATEUR_UNICITE, Boolean.toString(unique));
    }

    /**
     * Condition sur le choix retenu : la transition n'est empruntee que si la question porte la
     * reponse designee.
     *
     * <p>C'est la condition qui permet a un parcours de bifurquer selon la saisie de l'usager. Elle
     * exige une question a liste reellement pourvue de choix : la liste des valeurs proposees par
     * l'ecran est construite a partir d'eux, et reste vide sans eux.</p>
     *
     * @param fromStepIndex      position de l'etape portant la transition
     * @param toStepIndex        position de l'etape d'arrivee de la transition conditionnee
     * @param pilotQuestionIndex position de la question qui conditionne
     * @param choix              libelle du choix attendu
     * @return le jeu de donnees correspondant
     */
    public static TransitionControlDataSet valeurChoisie(int fromStepIndex, int toStepIndex,
                                                         int pilotQuestionIndex, String choix) {
        return new TransitionControlDataSet(fromStepIndex, toStepIndex, pilotQuestionIndex,
            VALIDATEUR_VALEUR_LISTE, choix);
    }

    /**
     * Condition quelconque, pour un type de controle designe explicitement.
     *
     * @param fromStepIndex      position de l'etape portant la transition
     * @param pilotQuestionIndex position de la question qui conditionne
     * @param validatorName      identifiant du type de controle
     * @param value              valeur attendue
     * @return le jeu de donnees correspondant
     */
    public static TransitionControlDataSet of(int fromStepIndex, int pilotQuestionIndex,
                                              String validatorName, String value) {
        return new TransitionControlDataSet(fromStepIndex, null, pilotQuestionIndex,
            validatorName, value);
    }

    /**
     * Jeu de donnees par defaut : premiere etape, premiere question, controle d'unicite.
     *
     * @return le jeu de donnees par defaut
     */
    public static TransitionControlDataSet defaults() {
        return unicite(0, 0, true);
    }
}
