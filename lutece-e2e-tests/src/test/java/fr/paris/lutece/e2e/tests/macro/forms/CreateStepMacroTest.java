package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.pages.bo.FormsEditPage;
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
 * Brique macro : ajouter une etape a un formulaire.
 *
 * <p>Lit : {@code ctx.formId}. Ecrit : ajoute un {@link FormsContext.StepRef} a {@code ctx.steps}.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Ajouter une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class CreateStepMacroTest extends MacroTest {

    @Step("Ajouter une etape")
    public static void run(FormsContext ctx, StepDataSet data) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId) avant d'ajouter une etape");

        new FormsEditPage(ctx.page, ctx.baseUrl)
            .addStep(String.valueOf(ctx.formId), data.title(), data.isFinal());

        int stepId = MacroSupport.extractStepId(ctx, ctx.formId, data.title());
        Assertions.assertTrue(stepId > 0, "L'etape '" + data.title() + "' devrait exister apres creation");
        ctx.steps.add(new FormsContext.StepRef(stepId, data.title(), data.initial(), data.isFinal()));
    }

    @Test
    @DisplayName("Ajouter une etape (auto-provisionnement du formulaire)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        run(ctx, StepDataSet.finalStep("Etape unique"));
    }
}
