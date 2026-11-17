package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Cible un element de la liste des questions d'une etape : une question ({@code ctx.questions}) ou un groupe
 * ({@code ctx.groups}), par index. Exactement un des deux index est renseigne.
 */
public record CompositeTargetDataSet(Integer questionIndex, Integer groupIndex) {

    public CompositeTargetDataSet {
        if ((questionIndex == null) == (groupIndex == null)) {
            throw new IllegalArgumentException("Renseigner soit questionIndex, soit groupIndex");
        }
    }

    public static CompositeTargetDataSet question(int questionIndex) {
        return new CompositeTargetDataSet(questionIndex, null);
    }

    public static CompositeTargetDataSet group(int groupIndex) {
        return new CompositeTargetDataSet(null, groupIndex);
    }

    public boolean isGroup() {
        return groupIndex != null;
    }
}
