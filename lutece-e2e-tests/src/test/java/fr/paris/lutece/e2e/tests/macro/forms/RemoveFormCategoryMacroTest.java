package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormCategoryDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Brique macro : supprimer une categorie de formulaire (bouton corbeille de la liste =
 * {@code view=confirmRemoveCategory&id_category=C}, puis confirmation AdminMessage {@code action=removeCategory}).
 * Effet prouve par l'absence de l'id dans la liste.
 *
 * <p>Constat du 2026-10-29 : la suppression d'une categorie utilisee par un formulaire est acceptee sans
 * avertissement et laisse {@code forms_form.id_category} pointer sur la categorie supprimee
 * ({@code FormCategoriesJspBean.doRemoveCategory} ne verifie pas l'usage).</p>
 *
 * <p>Lit et ecrit : {@code ctx.categoryIds} (retire la derniere categorie).</p>
 */
@Epic("Forms")
@Feature("Categories")
@Story("Supprimer une categorie")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class RemoveFormCategoryMacroTest extends MacroTest {

    @Step("Supprimer la derniere categorie de formulaire creee")
    public static void run(FormsContext ctx) {
        Assertions.assertFalse(ctx.categoryIds.isEmpty(), "Une categorie doit exister (ctx.categoryIds)");
        int id = ctx.categoryIds.get(ctx.categoryIds.size() - 1);

        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageFormsCategories.jsp?view=manageCategories");
        String href = FormsNonRegSupport.linkHref(ctx, "view=confirmRemoveCategory", "id_category", id);
        Assertions.assertNotNull(href, "Bouton de suppression introuvable pour la categorie " + id);
        FormsNonRegSupport.follow(ctx, href);
        FormsNonRegSupport.confirmAdminMessage(ctx, "removeCategory");

        Map<Integer, String> after = FormsNonRegSupport.formCategories(ctx);
        Assertions.assertFalse(after.containsKey(id), "La categorie " + id + " ne doit plus etre listee : " + after);
        ctx.categoryIds.remove(Integer.valueOf(id));
    }

    @Test
    @DisplayName("Supprimer une categorie de formulaire")
    void standaloneRemoveFormCategory() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormCategoryMacroTest.run(ctx, FormCategoryDataSet.defaults());
            run(ctx);
        } finally {
            FormCategoryCleanup.removeAll(ctx);
        }
    }
}
