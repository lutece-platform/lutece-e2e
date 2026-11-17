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
 * Brique macro : rattacher le formulaire a la derniere categorie creee ({@code view=modifyForm}, liste
 * {@code #id_category}, bouton {@code action_modifyForm}). Effet prouve par relecture de l'ecran : la categorie
 * est selectionnee.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.categoryIds}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Categories")
@Story("Rattacher un formulaire a une categorie")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class AssignFormCategoryMacroTest extends MacroTest {

    @Step("Rattacher le formulaire a la derniere categorie creee")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.categoryIds.isEmpty(), "Un formulaire et une categorie doivent exister");
        String id = String.valueOf(ctx.categoryIds.get(ctx.categoryIds.size() - 1));
        String screen = MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId;

        MacroSupport.navigate(ctx, screen);
        ctx.page.locator("form#modify_form select#id_category").selectOption(id);
        ctx.page.locator("form#modify_form button[name='action_modifyForm']").click();
        ctx.page.waitForLoadState();

        MacroSupport.navigate(ctx, screen);
        Assertions.assertEquals(id, ctx.page.locator("form#modify_form select#id_category").inputValue(),
            "Categorie relue du formulaire " + ctx.formId);
    }

    @Test
    @DisplayName("Rattacher un formulaire a une categorie")
    void standaloneAssignFormCategory() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateFormCategoryMacroTest.run(ctx, FormCategoryDataSet.defaults());
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
            FormCategoryCleanup.removeAll(ctx);
        }
    }
}
