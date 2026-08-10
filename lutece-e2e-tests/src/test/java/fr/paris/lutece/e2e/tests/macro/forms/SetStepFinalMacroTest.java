package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : marquer une etape comme finale.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}. Ecrit : passe {@code isFinal=true} sur le
 * {@link FormsContext.StepRef} cible. Ouvre la page de modification de l'etape
 * ({@code ManageSteps.jsp?view=modifyStep&id_step=ID}), coche la case "Finale" puis valide (OK).</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Marquer une etape comme finale")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class SetStepFinalMacroTest extends MacroTest {

    @Step("Marquer l'etape comme finale")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty(),
            "Un formulaire et au moins une etape doivent exister avant de configurer une etape");

        FormsContext.StepRef step = ctx.steps.get(data.stepIndex());
        Page page = ctx.page;

        // Page de modification de l'etape : cases a cocher Initiale / Finale + bouton OK
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=modifyStep&id_step=" + step.id);

        page.getByRole(AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName("Finale")).check();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("OK")).click();
        page.waitForLoadState();

        // Verification : le tag "Finale" apparait sur la carte de l'etape dans la liste
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=manageSteps&id_form=" + ctx.formId);
        Assertions.assertTrue(
            page.locator("#step_" + step.id).getByText("Finale").first().isVisible(),
            "L'etape '" + step.title + "' devrait etre marquee Finale dans la liste");

        step.isFinal = true;
    }

    @Test
    @DisplayName("Marquer une etape comme finale (auto-provisionnement formulaire + etape)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.of("Etape unique"));
        run(ctx, StepTargetDataSet.of(0));
    }
}
