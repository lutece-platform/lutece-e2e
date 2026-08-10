package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees ciblant un couple (groupe, question) par leur index dans le contexte.
 *
 * <p>{@code groupIndex} pointe une entree de {@code ctx.groups} et {@code questionIndex} une entree de
 * {@code ctx.questions}. Utilise par les briques de deplacement et de verification de hierarchie.</p>
 */
public record GroupTargetDataSet(int groupIndex, int questionIndex) {

    public static GroupTargetDataSet of(int groupIndex, int questionIndex) {
        return new GroupTargetDataSet(groupIndex, questionIndex);
    }

    public static GroupTargetDataSet defaults() {
        return new GroupTargetDataSet(0, 0);
    }
}
