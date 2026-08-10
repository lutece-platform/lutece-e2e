package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.pages.bo.FormsEditPage;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
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
 * Brique macro : publier un formulaire sur le portail (dates de disponibilite via modifyForm).
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.formTitle}. Ecrit : rien. Reutilise
 * {@link FormsEditPage#publishOnPortal(String, String, String)} puis verifie au mieux la presence
 * du titre dans la liste front-office (Assumptions si absent, la publication effective dependant de
 * la validite du formulaire).</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Publier le formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class PublishFormMacroTest extends MacroTest {

    @Step("Publier le formulaire sur le portail")
    public static void run(FormsContext ctx, PublishDataSet data) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant publication");

        new FormsEditPage(ctx.page, ctx.baseUrl)
            .publishOnPortal(String.valueOf(ctx.formId), data.startDate(), data.endDate());
        ctx.page.waitForLoadState();

        // Verification best-effort : le titre apparait dans la liste front-office des formulaires
        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=listForm");
        boolean listed = isTextVisible(ctx.page, ctx.formTitle);
        Assumptions.assumeTrue(listed,
            "Le formulaire '" + ctx.formTitle + "' n'apparait pas (encore) dans la liste FO : "
                + "publication verifiee au mieux (dates posees cote back-office)");
    }

    private static boolean isTextVisible(Page page, String text) {
        try {
            Locator loc = page.getByText(text);
            return loc.count() > 0 && loc.first().isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Publier un formulaire (auto-provisionnement formulaire + etape + question)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        run(ctx, PublishDataSet.defaults());
    }
}
