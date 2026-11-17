package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupModifyDataSet;
import fr.paris.lutece.e2e.tests.macro.data.MoveQuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
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
 * Brique macro : deplacer une question vers une autre etape et/ou dans un groupe.
 *
 * <p>Chemin reel : bouton « Deplacer » du menu de la ligne ({@code #btn-move-comp-D}), qui ouvre en iframe
 * l'assistant {@code view=moveComposite&id_step=S&id_display=D} (form {@code #move_composite}) en trois temps :
 * etape ({@code select[name=id_step]} + {@code validateStep}), parent ({@code select[name=id_parent]}, 0 = racine
 * de l'etape, sinon id_display du groupe, + {@code validateGroup}), position ({@code select[name=displayOrder]})
 * puis {@code action_moveComposite} (POST, token).</p>
 *
 * <p>Effet prouve sur la liste des questions de l'etape d'arrivee : la question (meme id) y figure, avec le
 * parent attendu ; elle ne figure plus dans l'etape de depart si l'etape change.</p>
 *
 * <p>Remplace pour la non-regression {@code MoveQuestionIntoGroup}, qui envoie un GET {@code action=moveComposite}
 * sans token (et {@code display_order} au lieu de {@code displayOrder}) et se met en skip sur forms upstream
 * {@code develop} (constate le 2026-10-29). Prerequis : ids de questions resolus ({@code VerifyQuestionsListed}).
 * Ecrit : {@code stepId} de la question.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Deplacer une question")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class MoveQuestionMacroTest extends MacroTest {

    private static final String WIZARD = "form#move_composite";

    @Step("Deplacer la question {data.questionIndex} vers l'etape {data.targetStepIndex}")
    public static void run(FormsContext ctx, MoveQuestionDataSet data) {
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu (VerifyQuestionsListed)");
        FormsContext.StepRef target = ctx.steps.get(data.targetStepIndex());
        int parent = 0;
        if (data.targetGroupIndex() >= 0) {
            FormsContext.GroupRef group = ctx.groups.get(data.targetGroupIndex());
            Assertions.assertEquals(target.id, group.stepId, "Le groupe d'arrivee doit appartenir a l'etape d'arrivee");
            parent = group.id;
        }
        int sourceStep = question.stepId;

        Map<Integer, int[]> placement = FormsNonRegSupport.questionPlacement(ctx, sourceStep);
        Assertions.assertTrue(placement.containsKey(question.id), "La question " + question.id + " doit etre listee dans son etape");
        int display = placement.get(question.id)[0];
        String url = ctx.page.locator("#btn-move-comp-" + display).getAttribute("data-lutece-load-content-url");
        Assertions.assertNotNull(url, "Bouton de deplacement introuvable pour la question " + question.id);
        FormsNonRegSupport.follow(ctx, url.replace("&amp;", "&"));

        Locator wizard = ctx.page.locator(WIZARD + ":has(input[name='id_display'][value='" + display + "'])");
        Assertions.assertEquals(1, wizard.count(), "L'assistant de deplacement de la question " + question.id + " doit etre affiche");
        wizard.locator("select[name='id_step']").selectOption(String.valueOf(target.id));
        wizard.locator("button[name='view_moveComposite'][value='validateStep']").click();
        ctx.page.waitForLoadState();
        Locator parents = ctx.page.locator(WIZARD + " select[name='id_parent']");
        Assertions.assertEquals(1, parents.locator("option[value='" + parent + "']").count(),
            "Parent " + parent + " attendu dans l'assistant ; options : " + parents.locator("option").allTextContents());
        parents.selectOption(String.valueOf(parent));
        ctx.page.locator(WIZARD + " button[name='view_moveComposite'][value='validateGroup']").click();
        ctx.page.waitForLoadState();
        Locator positions = ctx.page.locator(WIZARD + " select[name='displayOrder'] option");
        Assertions.assertTrue(positions.count() > 0, "L'assistant doit proposer au moins une position");
        String position = data.position() > 0 ? String.valueOf(data.position()) : positions.last().getAttribute("value");
        ctx.page.locator(WIZARD + " select[name='displayOrder']").selectOption(position);
        ctx.page.locator(WIZARD + " button[name='action_moveComposite']").click();
        ctx.page.waitForLoadState();

        Map<Integer, int[]> arrival = FormsNonRegSupport.questionPlacement(ctx, target.id);
        Assertions.assertTrue(arrival.containsKey(question.id), "La question " + question.id + " doit etre listee dans l'etape " + target.id);
        Assertions.assertEquals(parent, arrival.get(question.id)[1], "Parent de la question " + question.id + " apres deplacement");
        if (sourceStep != target.id) {
            Assertions.assertFalse(FormsNonRegSupport.questionPlacement(ctx, sourceStep).containsKey(question.id),
                "La question " + question.id + " ne doit plus etre listee dans l'etape " + sourceStep);
        }
        question.stepId = target.id;
    }

    @Test
    @DisplayName("Deplacer une question de l'etape A dans un groupe de l'etape B")
    void standaloneMoveQuestion() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Q1"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Q2"));
            VerifyQuestionsListedMacroTest.run(ctx);
            CreateGroupMacroTest.run(ctx, GroupDataSet.of("Groupe B"));
            run(ctx, MoveQuestionDataSet.toGroup(0, 1, 0));
            run(ctx, MoveQuestionDataSet.toStep(1, 1));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
