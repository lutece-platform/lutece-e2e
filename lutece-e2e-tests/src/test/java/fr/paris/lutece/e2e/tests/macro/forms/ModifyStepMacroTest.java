package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : renommer une etape.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}. Ecrit : met a jour le titre du premier
 * {@link FormsContext.StepRef} (index 0, convention identique au ciblage par defaut d'AddQuestion).
 * Ouvre la page de modification de l'etape ({@code ManageSteps.jsp?view=modifyStep&id_step=ID}),
 * remplace le champ titre puis valide (OK). Le nouveau titre provient du {@link StepDataSet}.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Renommer une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ModifyStepMacroTest extends MacroTest {

    @Step("Renommer l'etape")
    public static void run(FormsContext ctx, StepDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty(),
            "Un formulaire et au moins une etape doivent exister avant de renommer une etape");

        FormsContext.StepRef step = ctx.steps.get(0);
        Page page = ctx.page;
        String newTitle = data.title();

        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=modifyStep&id_step=" + step.id);

        // Le champ titre est prerempli avec l'ancien titre : fill() le remplace integralement
        page.locator("#step-title").fill(newTitle);
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("OK")).click();
        page.waitForLoadState();

        // Verification : le nouveau titre apparait sur la carte de l'etape dans la liste
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=manageSteps&id_form=" + ctx.formId);
        Assertions.assertTrue(
            page.locator("#step_" + step.id).getByText(newTitle).first().isVisible(),
            "L'etape renommee '" + newTitle + "' devrait apparaitre dans la liste");

        step.title = newTitle;
    }

    @Test
    @DisplayName("Renommer une etape (auto-provisionnement formulaire + etape)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.of("Etape a renommer"));
        run(ctx, StepDataSet.of("Etape renommee"));
    }
}
