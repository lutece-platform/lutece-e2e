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
 * Brique macro : supprimer un groupe et son contenu.
 *
 * <p>Chemin reel : bouton corbeille de la carte ({@code view=getConfirmRemoveComposite&id_display=D}), puis
 * confirmation AdminMessage {@code action=removeComposite}. Le serveur supprime le groupe <b>et ses
 * descendants</b> ({@code FormDisplayService.deleteDisplayAndDescendants}) ; il refuse si des reponses y sont
 * rattachees. Effet prouve : la carte {@code #group-D} a disparu, les questions qui y etaient aussi, les
 * autres questions de l'etape sont intactes.</p>
 *
 * <p>Ecrit : retire le groupe de {@code ctx.groups} et ses questions de {@code ctx.questions}.</p>
 */
@Epic("Forms")
@Feature("Groupes")
@Story("Supprimer un groupe")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class RemoveGroupMacroTest extends MacroTest {

    @Step("Supprimer le groupe {data.groupIndex} et son contenu")
    public static void run(FormsContext ctx, GroupModifyDataSet data) {
        FormsContext.GroupRef group = ctx.groups.get(data.groupIndex());
        Map<Integer, int[]> before = FormsNonRegSupport.questionPlacement(ctx, group.stepId);
        List<Integer> inside = new ArrayList<>();
        before.forEach((id, place) -> { if (place[1] == group.id) { inside.add(id); } });

        String href = FormsNonRegSupport.linkHref(ctx, "view=getConfirmRemoveComposite", "id_display", group.id);
        Assertions.assertNotNull(href, "Bouton de suppression introuvable pour le groupe " + group.id);
        FormsNonRegSupport.follow(ctx, href);
        FormsNonRegSupport.confirmAdminMessage(ctx, "removeComposite");

        Assertions.assertFalse(FormsNonRegSupport.groupsOfStep(ctx, group.stepId).containsKey(group.id),
            "Le groupe " + group.id + " ne doit plus etre liste");
        Map<Integer, int[]> after = FormsNonRegSupport.questionPlacement(ctx, group.stepId);
        List<Integer> expected = new ArrayList<>(before.keySet());
        expected.removeAll(inside);
        Assertions.assertEquals(expected, new ArrayList<>(after.keySet()),
            "Seules les questions du groupe (" + inside + ") doivent avoir disparu");
        ctx.groups.remove(group);
        ctx.questions.removeIf(q -> inside.contains(q.id));
    }

    @Test
    @DisplayName("Supprimer un groupe contenant une question, l'autre question de l'etape reste")
    void standaloneRemoveGroup() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Hors groupe"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Dans le groupe"));
            VerifyQuestionsListedMacroTest.run(ctx);
            CreateGroupMacroTest.run(ctx, GroupDataSet.of("Groupe a supprimer"));
            MoveQuestionMacroTest.run(ctx, MoveQuestionDataSet.toGroup(1, 0, 0));
            run(ctx, GroupModifyDataSet.target(0));
            Assertions.assertEquals(1, ctx.questions.size(), "Il reste la question hors groupe");
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
