package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
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
 * Brique macro : valider l'etape courante de la saisie BO et arriver sur l'etape attendue
 * ({@code action_doSaveStep}). Meme formulaire et meme oracle qu'en FO ({@code input[name=id_step]} = etape
 * cible) : la brique verifie l'ecran BO avant et apres, et delegue le clic a {@code GoToNextStepFO}.
 */
@Epic("Forms")
@Feature("Saisie back-office")
@Story("Passer a l'etape suivante en BO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class GoToNextStepBOMacroTest extends MacroTest {

    @Step("Valider l'etape BO et arriver sur l'etape {data.stepIndex}")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertEquals(1, ctx.page.locator(OpenFormResponseBOMacroTest.BO_FORM).count(),
            "La saisie BO doit etre affichee (url " + ctx.page.url() + ")");
        GoToNextStepFOMacroTest.run(ctx, data);
        OpenFormResponseBOMacroTest.assertOnBoStep(ctx, ctx.steps.get(data.stepIndex()).id);
    }

    @Test
    @DisplayName("Passer de l'etape A a l'etape B en saisie BO")
    void standaloneGoToNextStepBO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            OpenFormResponseBOMacroTest.run(ctx);
            FillFieldBOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont BO"));
            run(ctx, StepTargetDataSet.of(1));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
