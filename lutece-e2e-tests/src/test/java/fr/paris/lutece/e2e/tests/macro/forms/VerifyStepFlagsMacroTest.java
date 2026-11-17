package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepFlagsDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Brique de verification : les indicateurs « Initiale » / « Finale » d'une etape valent ceux attendus.
 *
 * <p>Se place derriere {@code SetStepInitial}, {@code SetStepFinal}, {@code UnsetStepFinal} ou une
 * duplication / un import : ces briques ne prouvent pas l'etat des <i>autres</i> etapes. Lecture exacte des
 * badges de la carte {@code #step_<id>} ; met a jour le {@link FormsContext.StepRef} avec l'etat relu.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Verifier les indicateurs d'une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyStepFlagsMacroTest extends MacroTest {

    @Step("Verifier les indicateurs de l'etape")
    public static void run(FormsContext ctx, StepFlagsDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && ctx.steps.size() > data.stepIndex(),
            "Le formulaire et l'etape ciblee doivent exister avant de verifier ses indicateurs");
        FormsContext.StepRef step = ctx.steps.get(data.stepIndex());

        Set<String> badges = FormsNonRegSupport.stepBadges(ctx, step.id);
        Assertions.assertAll("Indicateurs de l'etape '" + step.title + "' (badges lus : " + badges + ")",
            () -> Assertions.assertEquals(data.initial(), badges.contains("Initiale"), "Initiale"),
            () -> Assertions.assertEquals(data.isFinal(), badges.contains("Finale"), "Finale"));
        step.initial = data.initial();
        step.isFinal = data.isFinal();
    }

    @Test
    @DisplayName("Verifier les indicateurs d'un formulaire a deux etapes (A initiale, B finale)")
    void standaloneVerifyStepFlags() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            SetStepInitialMacroTest.run(ctx, StepTargetDataSet.of(0));
            SetStepFinalMacroTest.run(ctx, StepTargetDataSet.of(1));
            run(ctx, StepFlagsDataSet.initialOnly(0));
            run(ctx, StepFlagsDataSet.finalOnly(1));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
