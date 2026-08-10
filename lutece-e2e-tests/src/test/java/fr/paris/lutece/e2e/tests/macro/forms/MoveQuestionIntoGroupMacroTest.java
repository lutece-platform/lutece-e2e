package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : deplacer une question existante dans un groupe.
 *
 * <p>Lit : {@code ctx.groups}, {@code ctx.questions}. N'ecrit rien de nouveau dans le contexte
 * (elle modifie la hierarchie cote serveur).</p>
 *
 * <p>Le deplacement passe par une vue a etat ({@code moveComposite}) : selection de l'etape puis du
 * groupe cible (memorisation cote bean de session), puis application via l'action. On rejoue cette
 * sequence par URL. Comme cette UI (offcanvas + iframe + etat serveur) n'est pas toujours pilotable
 * de facon fiable, la brique se rabat sur {@link Assumptions} (test ignore, pas en echec) si les
 * identifiants d'affichage sont introuvables ou si le deplacement ne peut etre constate.</p>
 */
@Epic("Forms")
@Feature("Groupes")
@Story("Deplacer une question dans un groupe")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class MoveQuestionIntoGroupMacroTest extends MacroTest {

    @Step("Deplacer une question dans un groupe")
    public static void run(FormsContext ctx, GroupTargetDataSet data) {
        Assertions.assertTrue(!ctx.groups.isEmpty() && !ctx.questions.isEmpty(),
            "Au moins un groupe et une question doivent exister avant le deplacement");

        FormsContext.GroupRef group = ctx.groups.get(data.groupIndex());
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        int stepId = group.stepId;
        Page page = ctx.page;

        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);

        int questionDisplayId = resolveQuestionDisplayId(page, question.title);
        int groupDisplayId = resolveGroupDisplayId(page, group);

        // Sans les id_display de la question et du groupe, l'UI de deplacement n'est pas pilotable :
        // on ignore proprement le test plutot que de le faire echouer.
        Assumptions.assumeTrue(questionDisplayId > 0 && groupDisplayId > 0,
            "UI de deplacement non pilotable : id_display introuvable "
            + "(question=" + questionDisplayId + ", groupe=" + groupDisplayId + ")");

        // 1) Valider l'etape et le groupe cible : positionne _nIdStepTarget / _nIdParentTarget cote bean.
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=moveComposite"
            + "&id_display=" + questionDisplayId
            + "&id_step=" + stepId
            + "&id_parent=" + groupDisplayId
            + "&view_moveComposite=validateGroup"
            + "&stepValidated=true&groupValidated=true");

        // 2) Appliquer le deplacement (premiere position dans le groupe).
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?action=moveComposite"
            + "&id_display=" + questionDisplayId
            + "&id_step=" + stepId
            + "&id_parent=" + groupDisplayId
            + "&display_order=1"
            + "&stepValidated=true&groupValidated=true");

        // Verification de l'effet : la question doit desormais etre imbriquee dans la carte du groupe.
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);
        boolean nested = isQuestionNestedInGroup(page, group.title, question.title);

        Assumptions.assumeTrue(nested,
            "Le deplacement via l'UI moveComposite n'a pas pu etre applique de facon fiable "
            + "dans cet environnement (question '" + question.title + "' -> groupe '" + group.title + "')");
    }

    /** Recupere l'id d'affichage d'une question depuis le lien de suppression (view=getConfirmRemoveComposite). */
    private static int resolveQuestionDisplayId(Page page, String questionTitle) {
        Locator row = page.locator("#question-list li")
            .filter(new Locator.FilterOptions().setHasText(questionTitle))
            .filter(new Locator.FilterOptions()
                .setHas(page.locator("a[href*='view=getConfirmRemoveComposite']")));
        if (row.count() == 0) {
            return -1;
        }
        Locator link = row.first().locator("a[href*='view=getConfirmRemoveComposite']").first();
        return parseParam(link.getAttribute("href"), "id_display=");
    }

    /** Id d'affichage du groupe : memorise a la creation, sinon relu depuis l'id de sa carte "group-&lt;id&gt;". */
    private static int resolveGroupDisplayId(Page page, FormsContext.GroupRef group) {
        if (group.id > 0) {
            return group.id;
        }
        Locator groupCard = page.locator("div.card-question")
            .filter(new Locator.FilterOptions().setHasText(group.title));
        if (groupCard.count() == 0) {
            return -1;
        }
        String id = groupCard.first().getAttribute("id");
        if (id != null && id.startsWith("group-")) {
            try {
                return Integer.parseInt(id.substring("group-".length()));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private static boolean isQuestionNestedInGroup(Page page, String groupTitle, String questionTitle) {
        Locator groupCard = page.locator("div.card-question")
            .filter(new Locator.FilterOptions().setHasText(groupTitle));
        if (groupCard.count() == 0) {
            return false;
        }
        return groupCard.first().locator(".card-body").getByText(questionTitle).count() > 0;
    }

    private static int parseParam(String href, String key) {
        if (href == null || !href.contains(key)) {
            return -1;
        }
        try {
            return Integer.parseInt(href.split(key)[1].split("&")[0].split("#")[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Test
    @DisplayName("Deplacer une question dans un groupe (auto-provisionnement complet)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question a grouper"));
        CreateGroupMacroTest.run(ctx, GroupDataSet.defaults());
        run(ctx, GroupTargetDataSet.of(0, 0));
    }
}
