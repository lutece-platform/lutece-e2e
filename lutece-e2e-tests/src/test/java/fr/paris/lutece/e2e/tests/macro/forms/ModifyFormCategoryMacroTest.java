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
 * Brique macro : modifier le nom et le code d'une categorie de formulaire (offcanvas « Modifier » de la liste =
 * iframe {@code view=modifyCategory&id_category=C}, bouton {@code action_modifyCategory}). Effet prouve sur la
 * liste : meme id, nouveau libelle.
 *
 * <p>Lit : {@code ctx.categoryIds} (derniere categorie creee). Dataset : nouveaux nom et code (suffixes).</p>
 */
@Epic("Forms")
@Feature("Categories")
@Story("Modifier une categorie")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ModifyFormCategoryMacroTest extends MacroTest {

    @Step("Modifier la derniere categorie de formulaire creee")
    public static void run(FormsContext ctx, FormCategoryDataSet data) {
        Assertions.assertFalse(ctx.categoryIds.isEmpty(), "Une categorie doit exister (ctx.categoryIds)");
        int id = ctx.categoryIds.get(ctx.categoryIds.size() - 1);
        String name = data.name() + " " + ctx.runSuffix;
        String code = data.code() + ctx.runSuffix;

        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageFormsCategories.jsp?view=modifyCategory&id_category=" + id);
        Assertions.assertEquals(1, ctx.page.locator("input[name='id_category'][value='" + id + "']").count(),
            "L'ecran de modification de la categorie " + id + " doit etre affiche");
        ctx.page.locator("#name").fill(name);
        ctx.page.locator("#code").fill(code);
        ctx.page.locator("button[name='action_modifyCategory']").click();
        ctx.page.waitForLoadState();

        String label = FormsNonRegSupport.formCategories(ctx).get(id);
        Assertions.assertNotNull(label, "La categorie " + id + " doit toujours etre listee");
        Assertions.assertTrue(label.contains(code) && label.contains(name), "Libelle relu de la categorie " + id + " : " + label);
    }

    @Test
    @DisplayName("Modifier une categorie de formulaire")
    void standaloneModifyFormCategory() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormCategoryMacroTest.run(ctx, FormCategoryDataSet.defaults());
            run(ctx, FormCategoryDataSet.of("Categorie modifiee", "catmod"));
        } finally {
            FormCategoryCleanup.removeAll(ctx);
        }
    }
}
