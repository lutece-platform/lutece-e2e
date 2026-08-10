package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation / modification d'une unite (unittree).
 * {@code parentId} = id de l'unite parente (0 = racine du site).
 */
public record UnitDataSet(String code, String label, String description, int parentId) {

    public static UnitDataSet of(String label) {
        return new UnitDataSet("U", label, "Unite " + label, 0);
    }

    public static UnitDataSet underParent(String label, int parentId) {
        return new UnitDataSet("U", label, "Unite " + label, parentId);
    }

    public static UnitDataSet defaults() {
        return new UnitDataSet("U", "Service Macro", "Unite de test macro", 0);
    }

    public UnitDataSet withLabel(String newLabel) {
        return new UnitDataSet(code, newLabel, description, parentId);
    }
}
