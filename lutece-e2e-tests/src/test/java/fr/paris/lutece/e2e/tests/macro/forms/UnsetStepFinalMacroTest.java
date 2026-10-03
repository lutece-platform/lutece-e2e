package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : retirer le caractere final d'une etape.
 *
 * <p>Symetrique de {@link SetStepFinalMacroTest}. Lit : {@code ctx.formId}, {@code ctx.steps}.
 * Ecrit : passe {@code isFinal=false} sur le {@link FormsContext.StepRef} cible.</p>
 *
 * <p><b>Pourquoi cette brique est necessaire.</b> Lutece force la premiere etape creee a etre
 * finale : tant qu'un formulaire n'a qu'une etape, elle est forcement terminale. Les etapes
 * suivantes ne retirent pas ce drapeau, si bien qu'un formulaire multi-etapes se retrouve avec une
 * etape initiale <i>et</i> finale. En front office, une etape finale n'affiche pas de bouton
 * "Etape suivante" : le parcours s'arrete des la premiere etape et les transitions declarees ne
 * sont jamais empruntees. Marquer la vraie etape terminale ne suffit donc pas, il faut aussi
 * liberer celles qui ne le sont plus.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Retirer le caractere final d'une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class UnsetStepFinalMacroTest extends MacroTest {

    @Step("Retirer le caractere final de l'etape")
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty(),
            "Un formulaire et au moins une etape doivent exister avant de configurer une etape");

        FormsContext.StepRef step = ctx.steps.get(data.stepIndex());
        Page page = ctx.page;

        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=modifyStep&id_step=" + step.id);

        page.getByRole(AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName("Finale")).uncheck();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("OK")).click();
        page.waitForLoadState();

        // Verification sur la source de verite : la case du formulaire de modification, et non le
        // tag de la liste, qu'une autre etape finale rendrait ambigu.
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=modifyStep&id_step=" + step.id);
        Assertions.assertFalse(
            page.getByRole(AriaRole.CHECKBOX,
                new Page.GetByRoleOptions().setName("Finale")).isChecked(),
            "L'etape '" + step.title + "' ne devrait plus etre marquee Finale");

        step.isFinal = false;
    }

    @Test
    @DisplayName("Retirer le caractere final d'une etape (auto-provisionnement formulaire + 2 etapes)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.of("Premiere etape"));
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Derniere etape"));
        SetStepFinalMacroTest.run(ctx, StepTargetDataSet.of(1));
        run(ctx, StepTargetDataSet.of(0));
    }
}
