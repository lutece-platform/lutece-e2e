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
public record GroupDataSet(String title, int iterationMax) {

    public static GroupDataSet of(String title) {
        return new GroupDataSet(title, 1);
    }

    public static GroupDataSet defaults() {
        return new GroupDataSet("Macro Groupe", 1);
    }

    /**
     * Groupe repetable : {@code iterationMax = 3} rend le groupe iterable en front office (bouton
     * "ajouter une iteration" present et suppression possible des que 2 iterations existent).
     */
    public static GroupDataSet repeatable(String title) {
        return new GroupDataSet(title, 3);
    }
}
