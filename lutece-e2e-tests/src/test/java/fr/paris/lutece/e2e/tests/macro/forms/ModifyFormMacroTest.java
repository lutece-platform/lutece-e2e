package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormModifyDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : modifier le titre et la description d'un formulaire ({@code view=modifyForm}, onglet
 * Description, bouton {@code action_modifyForm}).
 *
 * <p>Effet prouve par relecture serveur : l'ecran {@code modifyForm} rouvert affiche le nouveau titre et la
 * nouvelle description, et la liste des formulaires resout le nouveau titre sur le meme id.</p>
 *
 * <p>Lit : {@code ctx.formId}. Ecrit : {@code ctx.formTitle}.</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Modifier le formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ModifyFormMacroTest extends MacroTest {

    @Step("Modifier le titre et la description du formulaire")
    public static void run(FormsContext ctx, FormModifyDataSet data) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        String title = data.newTitle() + " " + ctx.runSuffix;

        Locator form = openModifyForm(ctx);
        form.locator("#title").fill(title);
        form.locator("textarea#description").fill(data.newDescription());
        form.locator("button[name='action_modifyForm']").click();
        ctx.page.waitForLoadState();

        form = openModifyForm(ctx);
        Assertions.assertEquals(title, form.locator("#title").inputValue(), "Titre relu du formulaire " + ctx.formId);
        Assertions.assertEquals(data.newDescription(), form.locator("textarea#description").inputValue().trim(),
            "Description relue du formulaire " + ctx.formId);
        Assertions.assertEquals(ctx.formId, MacroSupport.extractFormId(ctx, title),
            "La liste des formulaires doit resoudre le nouveau titre sur le meme id");
        ctx.formTitle = title;
    }

    private static Locator openModifyForm(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId);
        Locator form = ctx.page.locator("form#modify_form:has(input[name='id_form'][value='" + ctx.formId + "'])");
        Assertions.assertEquals(1, form.count(), "L'ecran de modification du formulaire " + ctx.formId + " doit etre affiche");
        return form;
    }

    @Test
    @DisplayName("Modifier le titre et la description d'un formulaire")
    void standaloneModifyForm() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            run(ctx, FormModifyDataSet.defaults());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
