package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupMoveDataSet;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Brique macro : deplacer un groupe dans un autre groupe de la meme etape (groupes imbriques).
 *
 * <p>Chemin reel : bouton « Deplacer » de la carte du groupe ({@code #btn-move-<id_display>}, la question utilisant
 * {@code #btn-move-comp-<id_display>}, releve du 2026-11-10), qui ouvre en iframe l'assistant
 * {@code view=moveComposite&id_step=S&id_display=D} (form {@code #move_composite}) : etape ({@code validateStep}),
 * parent ({@code select[name=id_parent]} = id_display du groupe d'arrivee, {@code validateGroup}), position
 * ({@code displayOrder}) puis {@code action_moveComposite}. Effet prouve par {@link FormsNonRegSupport#compositesOfStep} :
 * le groupe, et lui seul, a pour parent le groupe d'arrivee.</p>
 *
 * <p>Le gabarit de la liste des questions referme mal son balisage avec des groupes imbriques : le contenu des
 * composants qui suivent sort de {@code #question-list}. Le standalone construit ce cas et verifie toute la structure
 * relue, garde-fou de la lecture en ordre de document de {@code compositesOfStep}.</p>
 *
 * <p>Lit : {@code ctx.groups} (ids d'affichage). Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Groupes")
@Story("Imbriquer un groupe dans un autre")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class MoveGroupMacroTest extends MacroTest {

    private static final String WIZARD = "form#move_composite";

    @Step("Deplacer le groupe {data.groupIndex} dans le groupe {data.targetGroupIndex}")
    public static void run(FormsContext ctx, GroupMoveDataSet data) {
        Assertions.assertTrue(ctx.groups.size() > Math.max(data.groupIndex(), data.targetGroupIndex()),
            "Les groupes deplace et d'arrivee doivent exister (ctx.groups)");
        FormsContext.GroupRef group = ctx.groups.get(data.groupIndex());
        FormsContext.GroupRef target = ctx.groups.get(data.targetGroupIndex());
        Assertions.assertEquals(group.stepId, target.stepId, "Les deux groupes doivent appartenir a la meme etape");
        Map<Integer, Integer> parentsBefore = FormsNonRegSupport.compositesOfStep(ctx, group.stepId).stream()
            .collect(Collectors.toMap(FormsNonRegSupport.Composite::display, FormsNonRegSupport.Composite::parent));

        Locator move = ctx.page.locator("#btn-move-" + group.id);
        Assertions.assertEquals(1, move.count(), "Bouton de deplacement du groupe '" + group.title + "' attendu");
        String url = move.getAttribute("data-lutece-load-content-url");
        Assertions.assertNotNull(url, "Le bouton de deplacement du groupe doit porter l'url de l'assistant");
        FormsNonRegSupport.follow(ctx, url.replace("&amp;", "&"));

        Locator wizard = ctx.page.locator(WIZARD + ":has(input[name='id_display'][value='" + group.id + "'])");
        Assertions.assertEquals(1, wizard.count(), "L'assistant de deplacement du groupe '" + group.title + "' doit etre affiche");
        wizard.locator("select[name='id_step']").selectOption(String.valueOf(group.stepId));
        wizard.locator("button[name='view_moveComposite'][value='validateStep']").click();
        ctx.page.waitForLoadState();
        Locator parents = ctx.page.locator(WIZARD + " select[name='id_parent']");
        Assertions.assertEquals(1, parents.locator("option[value='" + target.id + "']").count(),
            "Le groupe '" + target.title + "' doit etre propose comme parent ; options : " + parents.locator("option").allTextContents());
        parents.selectOption(String.valueOf(target.id));
        ctx.page.locator(WIZARD + " button[name='view_moveComposite'][value='validateGroup']").click();
        ctx.page.waitForLoadState();
        Locator positions = ctx.page.locator(WIZARD + " select[name='displayOrder'] option");
        Assertions.assertTrue(positions.count() > 0, "L'assistant doit proposer au moins une position");
        String position = data.position() > 0 ? String.valueOf(data.position()) : positions.last().getAttribute("value");
        ctx.page.locator(WIZARD + " select[name='displayOrder']").selectOption(position);
        ctx.page.locator(WIZARD + " button[name='action_moveComposite']").click();
        ctx.page.waitForLoadState();

        Map<Integer, Integer> parentsAfter = FormsNonRegSupport.compositesOfStep(ctx, group.stepId).stream()
            .collect(Collectors.toMap(FormsNonRegSupport.Composite::display, FormsNonRegSupport.Composite::parent));
        Assertions.assertEquals(target.id, parentsAfter.get(group.id),
            "Le groupe '" + group.title + "' doit avoir pour parent le groupe '" + target.title + "' : " + parentsAfter);
        parentsBefore.put(group.id, target.id);
        Assertions.assertEquals(parentsBefore, parentsAfter, "Seul le groupe deplace doit changer de parent");
    }

    @Test
    @DisplayName("G2 (avec Qa) place en tete de G1 (avec Q3, Q4, Q5) : structure imbriquee relue")
    void standaloneMoveGroup() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            for (String title : new String[] {"Qa", "Q3", "Q4", "Q5"}) {
                AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, title));
            }
            VerifyQuestionsListedMacroTest.run(ctx);
            CreateGroupMacroTest.run(ctx, GroupDataSet.of("G1"));
            CreateGroupMacroTest.run(ctx, GroupDataSet.of("G2"));
            MoveQuestionMacroTest.run(ctx, MoveQuestionDataSet.toGroup(1, 0, 0));
            MoveQuestionMacroTest.run(ctx, MoveQuestionDataSet.toGroup(2, 0, 0));
            MoveQuestionMacroTest.run(ctx, MoveQuestionDataSet.toGroup(3, 0, 0));
            MoveQuestionMacroTest.run(ctx, MoveQuestionDataSet.toGroup(0, 0, 1));
            run(ctx, GroupMoveDataSet.into(1, 0, 1));

            // Structure relue : G1 { G2 { Qa }, Q3, Q4, Q5 }, chaque composant avec son titre et son parent.
            int g1 = ctx.groups.get(0).id;
            int g2 = ctx.groups.get(1).id;
            List<String> read = FormsNonRegSupport.compositesOfStep(ctx, ctx.steps.get(0).id).stream()
                .map(c -> (c.group() ? "G " : "Q ") + c.title().replace(" " + ctx.runSuffix, "") + " < "
                    + (c.parent() == 0 ? "racine" : c.parent() == g1 ? "G1" : c.parent() == g2 ? "G2" : "?" + c.parent()))
                .collect(Collectors.toList());
            Assertions.assertEquals(List.of("G G1 < racine", "G G2 < G1", "Q Qa < G2", "Q Q3 < G1", "Q Q4 < G1", "Q Q5 < G1"), read,
                "Structure imbriquee relue par compositesOfStep");
            VerifyQuestionsListedMacroTest.run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
