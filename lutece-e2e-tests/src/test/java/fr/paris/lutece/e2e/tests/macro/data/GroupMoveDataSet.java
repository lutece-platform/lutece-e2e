package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Deplacement d'un groupe dans un autre groupe de la meme etape.
 *
 * @param groupIndex       index du groupe a deplacer dans {@code ctx.groups}
 * @param targetGroupIndex index du groupe d'arrivee dans {@code ctx.groups}
 * @param position         position 1-based proposee par l'ecran, -1 pour la derniere
 */
public record GroupMoveDataSet(int groupIndex, int targetGroupIndex, int position) {

    /** Dans un autre groupe, a la position donnee (1 = en tete). */
    public static GroupMoveDataSet into(int groupIndex, int targetGroupIndex, int position) {
        return new GroupMoveDataSet(groupIndex, targetGroupIndex, position);
    }
}
