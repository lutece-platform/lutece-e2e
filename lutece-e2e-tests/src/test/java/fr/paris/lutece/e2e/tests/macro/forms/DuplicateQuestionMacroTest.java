package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
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
 * Brique macro : dupliquer une question depuis la liste des questions (menu Actions « Dupliquer la question »).
 *
 * <p>Chemin reel : menu Actions de la ligne, bouton « Dupliquer la question » qui soumet le formulaire POST cache
 * {@code #duplicate-question-Q} ({@code action=duplicateQuestion}, {@code id_step}, {@code id_target},
 * {@code _csrftoken}). Effet prouve par difference d'ensembles des ids de questions de
 * l'etape (exactement une nouvelle), titre de la copie derive de l'original (« Copie de ... »).</p>
 *
 * <p>Historique : avant LUT-34208 (upstream develop du 2026-10-22), l'entree etait un lien GET sans token,
 * refuse par le controleur (« Internal error »). La brique garde l'oracle complet pour detecter un retour du
 * defaut.</p>
 *
 * <p>Prerequis : ids de questions resolus ({@code VerifyQuestionsListed}). Lit : {@code ctx.questions}.
 * Ecrit : ajoute la copie a {@code ctx.questions}. Dataset : {@link QuestionTargetDataSet} (index seul).</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Dupliquer une question")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class DuplicateQuestionMacroTest extends MacroTest {

    @Step("Dupliquer la question d'index {data.questionIndex}")
    public static void run(FormsContext ctx, QuestionTargetDataSet data) {
        Assertions.assertTrue(ctx.questions.size() > data.questionIndex(), "La question doit exister (ctx.questions)");
        FormsContext.QuestionRef source = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(source.id > 0,
            "L'id de la question '" + source.title + "' doit etre resolu (VerifyQuestionsListed)");

        Map<Integer, String> before = FormsNonRegSupport.questionsOfStep(ctx, source.stepId);
        Assertions.assertTrue(before.containsKey(source.id), "La question " + source.id + " doit etre listee");
        FormsNonRegSupport.clickInDropdown(ctx,
            ctx.page.locator("button[form='duplicate-question-" + source.id + "']"), "Dupliquer la question " + source.id);
        Assertions.assertTrue(
            ctx.page.locator("form input[name='id_step'][value='" + source.stepId + "']").count() >= 1,
            "Apres duplication, la liste des questions doit etre reaffichee (url " + ctx.page.url() + ", h1/h2 : "
                + ctx.page.locator("h1, h2, h3, .alert").allInnerTexts() + ")");

        Map<Integer, String> after = FormsNonRegSupport.questionsOfStep(ctx, source.stepId);
        List<Integer> fresh = FormsNonRegSupport.newIds(after.keySet(), new ArrayList<>(before.keySet()));
        Assertions.assertEquals(1, fresh.size(), "Exactement une question nouvelle attendue ; avant " + before + ", apres " + after);
        int copyId = fresh.get(0);
        // Titre de la copie constate le 2026-10-27 : « Copie de <titre> » (prefixe i18n) ; seul le titre source est exige.
        Assertions.assertTrue(after.get(copyId).endsWith(source.title) && !after.get(copyId).equals(source.title),
            "Titre de la question copiee distinct et derive de '" + source.title + "' : '" + after.get(copyId) + "'");

        ctx.questions.add(new FormsContext.QuestionRef(copyId, after.get(copyId), source.type, source.stepId));
    }

    @Test
    @DisplayName("Dupliquer une question texte")
    void standaloneDuplicateQuestion() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            VerifyQuestionsListedMacroTest.run(ctx);
            run(ctx, QuestionTargetDataSet.of(0));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
