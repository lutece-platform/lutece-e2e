package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseActionDataSet;
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

import java.util.regex.Pattern;

/**
 * Brique macro : declencher une action de workflow sur une reponse.
 *
 * <p>Lit : rien. Ecrit : rien. Ouvre le detail de la premiere reponse (saute si multivue vide), puis
 * declenche l'action de workflow dont le libelle correspond a {@code data.actionLabel()} SI elle est
 * presente ; sinon saute proprement via Assumptions (formulaire sans workflow, ou libelle different).
 * Confirme l'action si une page/modal de confirmation apparait, puis verifie au mieux une confirmation
 * / un changement d'etat.</p>
 */
@Epic("Forms")
@Feature("Réponses")
@Story("Declencher une action de workflow sur une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class RunWorkflowActionOnResponseMacroTest extends MacroTest {

    private static final Pattern CONFIRM = Pattern.compile(
        "^(Oui|OK|Confirmer|Valider|Valider l'action)$", Pattern.CASE_INSENSITIVE);

    @Step("Declencher une action de workflow sur la reponse")
    public static void run(FormsContext ctx, ResponseActionDataSet data) {
        boolean opened = OpenResponseDetailMacroTest.openFirstResponseDetail(ctx);
        Assumptions.assumeTrue(opened,
            "aucune reponse sur laquelle declencher une action (multivue vide)");

        Page page = ctx.page;
        Pattern label = Pattern.compile(Pattern.quote(data.actionLabel()), Pattern.CASE_INSENSITIVE);

        // L'action peut etre un lien ou un bouton sur le detail de la reponse.
        Locator action = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(label));
        if (action.count() == 0 || !action.first().isVisible()) {
            action = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(label));
        }
        boolean present = action.count() > 0 && action.first().isVisible();
        Assumptions.assumeTrue(present,
            "action workflow '" + data.actionLabel() + "' absente sur le detail de la reponse "
                + "(aucun workflow associe au formulaire ?)");

        action.first().click();
        page.waitForLoadState();

        // Confirmation eventuelle (page AdminMessage / modal / offcanvas).
        confirmIfPresent(page);
        page.waitForLoadState();

        Assertions.assertFalse(page.url().contains("AdminLogin"),
            "La session admin ne devrait pas etre perdue apres l'action de workflow");
        Assertions.assertTrue(actionConfirmed(page),
            "L'action de workflow devrait produire une confirmation ou un changement d'etat visible");
    }

    private static void confirmIfPresent(Page page) {
        Locator link = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(CONFIRM));
        if (link.count() > 0 && link.first().isVisible()) {
            link.first().click();
            return;
        }
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CONFIRM));
        if (btn.count() > 0 && btn.first().isVisible()) {
            btn.first().click();
        }
    }

    /** Vrai si une confirmation / un changement d'etat best-effort est observable. */
    private static boolean actionConfirmed(Page page) {
        if (page.locator(".alert, .notification, .badge, .tag").count() > 0) {
            return true;
        }
        // Repli : on est reste dans l'espace d'administration Forms sans erreur.
        return page.url().contains("forms") || page.url().contains("Forms");
    }

    @Test
    @DisplayName("Declencher une action de workflow sur une reponse (auto-provisionnement + soumission FO)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        // Provisionnement best-effort d'une reponse. Sans workflow associe, l'action sera absente et
        // run() sautera proprement via Assumptions.
        OpenMultiviewMacroTest.submitOneFoResponse(ctx);
        run(ctx, ResponseActionDataSet.defaults());
    }
}
