package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Cible un groupe du contexte, avec un nouveau titre pour la modification.
 *
 * @param groupIndex index du groupe dans {@code ctx.groups}
 * @param newTitle   nouveau titre (la brique y ajoute le suffixe d'execution) ; ignore pour la suppression
 */
public record GroupModifyDataSet(int groupIndex, String newTitle) {

    public static GroupModifyDataSet of(int groupIndex, String newTitle) {
        return new GroupModifyDataSet(groupIndex, newTitle);
    }

    public static GroupModifyDataSet target(int groupIndex) {
        return new GroupModifyDataSet(groupIndex, null);
    }
}
