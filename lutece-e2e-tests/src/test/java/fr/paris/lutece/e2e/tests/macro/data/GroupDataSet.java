package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation d'un groupe (regroupement) dans une etape.
 *
 * <p>Le {@code title} est un titre de base : la brique y ajoute le suffixe unique du contexte pour
 * garantir l'unicite sur un run (comme {@link FormDataSet}).</p>
 *
 * <p>{@code iterationMax} regle la repetabilite du groupe cote back-office (champ {@code iterationMax}
 * du formulaire de creation) : {@code 1} = groupe simple non repetable (defaut, comportement historique),
 * {@code >= 2} = groupe repetable (les blocs d'iteration et les controles ajouter/retirer apparaissent en
 * front office).</p>
 */
public record GroupDataSet(String title, int iterationMax, Integer stepIndex) {

    public static GroupDataSet of(String title) {
        return new GroupDataSet(title, 1, null);
    }

    public static GroupDataSet defaults() {
        return new GroupDataSet("Macro Groupe", 1, null);
    }

    /**
     * Cible l'etape qui portera le groupe.
     *
     * <p>Sans cela, le groupe se pose sur la derniere etape creee. Un formulaire bati etape par
     * etape s'en accommode ; un formulaire dont toutes les etapes sont creees d'abord verrait tous
     * ses groupes s'empiler sur la derniere.</p>
     *
     * @param index rang de l'etape dans {@code ctx.steps}
     * @return le jeu de donnees cible sur cette etape
     */
    public GroupDataSet onStep(int index) {
        return new GroupDataSet(title, iterationMax, index);
    }

    /**
     * Groupe repetable : {@code iterationMax = 3} rend le groupe iterable en front office (bouton
     * "ajouter une iteration" present et suppression possible des que 2 iterations existent).
     */
    public static GroupDataSet repeatable(String title) {
        return new GroupDataSet(title, 3, null);
    }

    /**
     * Groupe repetable dont on fixe soi-meme le plafond d'iterations.
     *
     * <p>Utile pour transcrire un formulaire existant, dont les groupes repetables portent un
     * plafond metier precis que {@link #repeatable(String)} ne devinerait pas.</p>
     *
     * @param title        titre de base du groupe
     * @param iterationMax nombre maximum d'iterations, au moins 2 pour que le groupe soit repetable
     * @return le jeu de donnees correspondant
     */
    public static GroupDataSet repeatable(String title, int iterationMax) {
        return new GroupDataSet(title, iterationMax, null);
    }
}
