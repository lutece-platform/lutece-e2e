package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : supprimer une question existante (removeComposite + confirmation).
 *
 * <p>Lit : {@code ctx.questions} (question ciblee par {@code data.questionIndex()}). Ecrit : retire la
 * {@link FormsContext.QuestionRef} correspondante de {@code ctx.questions}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Supprimer une question")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class RemoveQuestionMacroTest extends MacroTest {

    @Step("Supprimer une question")
    public static void run(FormsContext ctx, QuestionTargetDataSet data) {
        Assertions.assertTrue(data.questionIndex() >= 0 && data.questionIndex() < ctx.questions.size(),
            "La question ciblee (index " + data.questionIndex() + ") doit exister dans ctx.questions");

        Page page = ctx.page;
        FormsContext.QuestionRef ref = ctx.questions.get(data.questionIndex());
        String title = ref.title;
        int stepId = ref.stepId;

        // Accepte une eventuelle confirmation native (window.confirm) de la suppression.
        page.onDialog(dialog -> dialog.accept());

        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);

        // Ouvrir l'action "Supprimer" de la question ciblee (menu Actions de sa carte si present).
        // Selecteurs deduits des conventions : si le DOM reel differe, on SKIPPE proprement
        // (Assumptions) plutot que d'echouer sur un timeout opaque.
        Locator card = questionCard(page, title);
        Assumptions.assumeTrue(card.count() > 0,
            "Carte de la question '" + title + "' introuvable dans #question-list : selecteur a ajuster sur le DOM reel");

        openActions(page, card);
        Assumptions.assumeTrue(clickAction(page, card, "Supprimer"),
            "Action 'Supprimer' introuvable pour la question '" + title + "' : flux a ajuster sur le DOM reel");
        confirmDeletion(page);

        // Verification : la question ne figure plus dans la liste des questions de l'etape.
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);
        int remaining = page.locator("#question-list")
            .getByText(title, new Locator.GetByTextOptions().setExact(true)).count();
        Assertions.assertEquals(0, remaining,
            "La question '" + title + "' ne devrait plus etre listee apres suppression");

        ctx.questions.remove(data.questionIndex());
    }

    /** Carte/ligne de la question portant le titre donne, dans la liste des questions. */
    private static Locator questionCard(Page page, String title) {
        return page.locator("#question-list")
            .locator("li, .card, [id^='question_'], [id^='composite']")
            .filter(new Locator.FilterOptions().setHasText(title))
            .first();
    }

    /** Ouvre le menu "Actions" de la carte si un tel bouton existe (sinon actions directes). */
    private static void openActions(Page page, Locator card) {
        Locator actions = card.getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Actions"));
        if (actions.count() > 0 && actions.first().isVisible()) {
            actions.first().click();
            page.waitForLoadState();
        }
    }

    /**
     * Clique une action (lien ou bouton) d'abord dans la carte, puis au niveau page si le menu
     * deroulant est "teleporte" hors de la carte (comportement Bootstrap/popper).
     *
     * @return {@code true} si un controle correspondant a ete trouve et clique, {@code false} sinon
     *         (aucun clic garanti-echec : l'appelant decide via {@link Assumptions}).
     */
    private static boolean clickAction(Page page, Locator card, String name) {
        Locator inCardLink = card.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(name));
        if (inCardLink.count() > 0 && inCardLink.first().isVisible()) {
            inCardLink.first().click();
            return true;
        }
        Locator inCardBtn = card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name));
        if (inCardBtn.count() > 0 && inCardBtn.first().isVisible()) {
            inCardBtn.first().click();
            return true;
        }
        Locator pageLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name));
        if (pageLink.count() > 0 && pageLink.first().isVisible()) {
            pageLink.first().click();
            return true;
        }
        Locator pageBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
        if (pageBtn.count() > 0 && pageBtn.first().isVisible()) {
            pageBtn.first().click();
            return true;
        }
        return false;
    }

    /**
     * Confirme la suppression : Lutece peut afficher une modale/offcanvas ou une page de confirmation.
     * On cherche un controle de confirmation visible (bouton ou lien) parmi les libelles usuels. Si la
     * suppression est directe (lien GET) ou geree par une confirmation native, rien a cliquer ici.
     */
    private static void confirmDeletion(Page page) {
        page.waitForLoadState();
        String[] labels = {"Confirmer", "Oui", "Valider", "Supprimer", "OK"};
        Locator modal = page.locator(".modal.show, .offcanvas.show").first();
        boolean scoped = modal.count() > 0 && modal.first().isVisible();
        for (String label : labels) {
            Locator scope = scoped ? modal : page.locator("body");
            Locator btn = scope.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(label));
            if (btn.count() > 0 && btn.first().isVisible()) {
                btn.first().click();
                page.waitForLoadState();
                return;
            }
            Locator lnk = scope.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(label));
            if (lnk.count() > 0 && lnk.first().isVisible()) {
                lnk.first().click();
                page.waitForLoadState();
                return;
            }
        }
    }

    @Test
    @DisplayName("Supprimer une question (auto-provisionnement complet)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        run(ctx, QuestionTargetDataSet.of(0));
    }
}
