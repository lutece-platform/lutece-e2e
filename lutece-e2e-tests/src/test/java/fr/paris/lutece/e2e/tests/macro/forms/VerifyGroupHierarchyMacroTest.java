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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : verifier qu'un groupe contient bien la question attendue.
 *
 * <p>Lit : {@code ctx.groups}, {@code ctx.questions}. N'ecrit rien. Assertion pure : la carte du
 * groupe (repere par son titre) doit contenir, dans son corps ({@code .card-body}), le titre de la
 * question ciblee.</p>
 */
@Epic("Forms")
@Feature("Groupes")
@Story("Verifier la hierarchie d'un groupe")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyGroupHierarchyMacroTest extends MacroTest {

    @Step("Verifier la hierarchie du groupe")
    public static void run(FormsContext ctx, GroupTargetDataSet data) {
        Assertions.assertTrue(!ctx.groups.isEmpty() && !ctx.questions.isEmpty(),
            "Au moins un groupe et une question doivent exister pour verifier la hierarchie");

        FormsContext.GroupRef group = ctx.groups.get(data.groupIndex());
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Page page = ctx.page;

        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=manageQuestions&id_step=" + group.stepId);

        Locator groupCard = page.locator("div.card-question")
            .filter(new Locator.FilterOptions().setHasText(group.title));
        Assertions.assertTrue(groupCard.count() > 0,
            "La carte du groupe '" + group.title + "' devrait etre presente sur l'etape");

        Locator nested = groupCard.first().locator(".card-body").getByText(question.title);
        Assertions.assertTrue(nested.count() > 0 && nested.first().isVisible(),
            "La question '" + question.title + "' devrait etre imbriquee dans le groupe '" + group.title + "'");
    }

    @Test
    @DisplayName("Verifier la hierarchie d'un groupe (auto-provisionnement complet)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question a grouper"));
        CreateGroupMacroTest.run(ctx, GroupDataSet.defaults());
        MoveQuestionIntoGroupMacroTest.run(ctx, GroupTargetDataSet.of(0, 0));
        run(ctx, GroupTargetDataSet.of(0, 0));
    }
}
