package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepFlagsDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionTargetsDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro FO : valider l'etape courante et arriver sur l'etape attendue.
 *
 * <p>Complement de {@code NextStepFO}, qui cherche le libelle « Etape suivante » sans accent et se met en
 * skip sur le bouton reel « Étape suivante » (constate le 2026-10-27, brique non modifiee). Ici le bouton est
 * vise par son nom d'action {@code action_doSaveStep}, en excluant le doublon {@code aria-hidden} que le
 * template place avant lui. Effet prouve par l'etat du formulaire FO : le champ cache {@code id_step} vaut
 * l'id de l'etape cible (une erreur de validation laisse l'etape courante).</p>
 *
 * <p>Lit : {@code ctx.steps}. Dataset : {@link StepTargetDataSet} = etape d'arrivee attendue.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Passer a l'etape suivante")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class GoToNextStepFOMacroTest extends MacroTest {

    private static final String FO_FORM = "form#form-validate";

    @Step("Valider l'etape courante en FO et arriver sur l'etape attendue")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.steps.size() > data.stepIndex(), "L'etape d'arrivee doit exister (ctx.steps)");
        FormsContext.StepRef expected = ctx.steps.get(data.stepIndex());
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);

        Locator form = page.locator(FO_FORM);
        Assertions.assertEquals(1, form.count(), "Le formulaire FO #form-validate doit etre affiche");
        String before = form.locator("input[name='id_step']").inputValue();
        Locator next = form.locator("button[name='action_doSaveStep']:not([aria-hidden='true'])");
        Assertions.assertEquals(1, next.count(),
            "L'etape FO " + before + " doit proposer exactement un bouton visible action_doSaveStep");
        next.click();
        page.waitForLoadState();

        Assertions.assertEquals(1, page.locator(FO_FORM).count(),
            "Apres validation de l'etape, le formulaire FO doit etre reaffiche");
        String after = page.locator(FO_FORM + " input[name='id_step']").inputValue();
        Assertions.assertEquals(String.valueOf(expected.id), after,
            "Etape FO attendue '" + expected.title + "' (id " + expected.id + ") apres l'etape " + before);
    }

    @Test
    @DisplayName("Passer de l'etape A a l'etape B en FO")
    void standaloneGoToNextStepFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            VerifyStepFlagsMacroTest.run(ctx, StepFlagsDataSet.initialOnly(0));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            // A -> B existe deja par la liaison automatique a la creation de B (FORMS-546) : oracle exact.
            VerifyTransitionTargetsMacroTest.run(ctx, TransitionTargetsDataSet.of(0, 1));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont"));
            run(ctx, StepTargetDataSet.of(1));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
