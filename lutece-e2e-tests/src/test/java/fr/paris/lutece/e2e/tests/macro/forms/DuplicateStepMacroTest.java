package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Brique macro : dupliquer une etape depuis la liste des etapes (menu Actions « Dupliquer l'etape »).
 *
 * <p>Chemin reel : menu Actions de la carte de l'etape, entree « Dupliquer l'etape » = formulaire POST
 * ({@code action=duplicateStep}, {@code id_step}, {@code _csrftoken}). Effet prouve par difference d'ensembles des ids d'etapes
 * (exactement une nouvelle carte), titre de la copie derive de l'original (« Copie de ... ») et questions de la copie (titres)
 * egales a celles de l'original.</p>
 *
 * <p>Historique : avant LUT-34002/LUT-34208 (upstream develop du 2026-10-18/22), l'entree etait un lien GET
 * porteur du token de {@code createStep}, refuse par {@code duplicateStep} (« Internal error »). La brique
 * garde l'oracle complet pour detecter un retour du defaut.</p>
 *
 * <p>Lit : {@code ctx.steps}. Ecrit : ajoute la copie a {@code ctx.steps}.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Dupliquer une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class DuplicateStepMacroTest extends MacroTest {

    @Step("Dupliquer l'etape d'index {data.stepIndex}")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.steps.size() > data.stepIndex(), "L'etape a dupliquer doit exister (ctx.steps)");
        FormsContext.StepRef source = ctx.steps.get(data.stepIndex());

        Map<Integer, String> before = FormsNonRegSupport.stepsOfForm(ctx);
        Assertions.assertTrue(before.containsKey(source.id), "L'etape " + source.id + " doit etre listee");
        List<String> sourceQuestions = new ArrayList<>(FormsNonRegSupport.questionsOfStep(ctx, source.id).values());

        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageSteps.jsp?view=manageSteps&id_form=" + ctx.formId);
        FormsNonRegSupport.clickInDropdown(ctx, ctx.page.locator("form:has(input[name='action'][value='duplicateStep'])"
            + ":has(input[name='id_step'][value='" + source.id + "']) button[type='submit']"), "Dupliquer l'etape " + source.id);
        Assertions.assertEquals(1, ctx.page.locator("form#importJsonModal").count(),
            "Apres duplication, la liste des etapes doit etre reaffichee (url " + ctx.page.url() + ", titre '"
                + ctx.page.title() + "', h1/h2 : " + ctx.page.locator("h1, h2, h3, .alert").allInnerTexts() + ")");

        Map<Integer, String> after = FormsNonRegSupport.stepsOfForm(ctx);
        List<Integer> fresh = FormsNonRegSupport.newIds(after.keySet(), new ArrayList<>(before.keySet()));
        Assertions.assertEquals(1, fresh.size(), "Exactement une etape nouvelle attendue ; avant " + before + ", apres " + after);
        int copyId = fresh.get(0);
        // Titre de la copie constate le 2026-10-27 : « Copie de <titre> » (prefixe i18n) ; seul le titre source est exige.
        Assertions.assertTrue(after.get(copyId).endsWith(source.title) && !after.get(copyId).equals(source.title),
            "Titre de l'etape copiee distinct et derive de '" + source.title + "' : '" + after.get(copyId) + "'");
        Assertions.assertEquals(sourceQuestions, new ArrayList<>(FormsNonRegSupport.questionsOfStep(ctx, copyId).values()),
            "La copie doit porter les memes questions que l'etape " + source.id);

        ctx.steps.add(new FormsContext.StepRef(copyId, after.get(copyId), false, false));
    }

    @Test
    @DisplayName("Dupliquer une etape portant deux questions")
    void standaloneDuplicateStep() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape source"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Choix"));
            run(ctx, StepTargetDataSet.of(0));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
