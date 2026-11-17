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
 * Brique macro : creer une categorie de formulaire ({@code ManageFormsCategories.jsp}).
 *
 * <p>Chemin reel : le bouton « Ajouter » de la liste ouvre en iframe {@code view=createCategory} (champs
 * {@code #name}, {@code #code}, bouton {@code action_createCategory}). Effet prouve par difference d'ensembles des
 * ids de la liste (exactement une categorie nouvelle) et par son libelle (code et nom).</p>
 *
 * <p>Ecrit : ajoute l'id a {@code ctx.categoryIds}. Nom et code recoivent le suffixe d'execution.</p>
 */
@Epic("Forms")
@Feature("Categories")
@Story("Creer une categorie")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class CreateFormCategoryMacroTest extends MacroTest {

    @Step("Creer la categorie de formulaire {data.name}")
    public static void run(FormsContext ctx, FormCategoryDataSet data) {
        String name = data.name() + " " + ctx.runSuffix;
        String code = data.code() + ctx.runSuffix;
        Map<Integer, String> before = FormsNonRegSupport.formCategories(ctx);

        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageFormsCategories.jsp?view=createCategory");
        ctx.page.locator("#name").fill(name);
        ctx.page.locator("#code").fill(code);
        ctx.page.locator("button[name='action_createCategory']").click();
        ctx.page.waitForLoadState();

        Map<Integer, String> after = FormsNonRegSupport.formCategories(ctx);
        List<Integer> fresh = FormsNonRegSupport.newIds(after.keySet(), new ArrayList<>(before.keySet()));
        Assertions.assertEquals(1, fresh.size(), "Exactement une categorie nouvelle attendue ; apres : " + after);
        String label = after.get(fresh.get(0));
        Assertions.assertTrue(label.contains(code) && label.contains(name), "Libelle de la categorie creee : " + label);
        ctx.categoryIds.add(fresh.get(0));
    }

    @Test
    @DisplayName("Creer une categorie de formulaire")
    void standaloneCreateFormCategory() {
        FormsContext ctx = newLoggedInContext();
        try {
            run(ctx, FormCategoryDataSet.defaults());
        } finally {
            FormCategoryCleanup.removeAll(ctx);
        }
    }
}
