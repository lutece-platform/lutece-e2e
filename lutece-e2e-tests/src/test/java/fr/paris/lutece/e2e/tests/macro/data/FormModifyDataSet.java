package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees de modification d'un formulaire (onglet Description de {@code view=modifyForm}).
 *
 * @param newTitle       nouveau titre ; la brique y ajoute le suffixe d'execution du contexte
 * @param newDescription nouvelle description
 */
public record FormModifyDataSet(String newTitle, String newDescription) {

    public static FormModifyDataSet defaults() {
        return new FormModifyDataSet("Macro Form modifie", "Description modifiee E2E");
    }

    public static FormModifyDataSet of(String newTitle, String newDescription) {
        return new FormModifyDataSet(newTitle, newDescription);
    }
}
