package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees d'une categorie de formulaire (plugin forms, {@code ManageFormsCategories.jsp}). Distinct de
 * {@link CategoryDataSet}, qui decrit une categorie du plugin appointment.
 *
 * @param name nom ; la brique y ajoute le suffixe d'execution du contexte
 * @param code code ; la brique y ajoute le suffixe d'execution du contexte
 */
public record FormCategoryDataSet(String name, String code) {

    public static FormCategoryDataSet defaults() {
        return new FormCategoryDataSet("Categorie E2E", "cate2e");
    }

    public static FormCategoryDataSet of(String name, String code) {
        return new FormCategoryDataSet(name, code);
    }
}
