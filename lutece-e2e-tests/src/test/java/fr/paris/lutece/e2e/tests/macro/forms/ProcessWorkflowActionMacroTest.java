package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.ActionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StateDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.WorkflowDataSet;
import fr.paris.lutece.e2e.tests.macro.data.WorkflowProcessDataSet;
import fr.paris.lutece.e2e.tests.macro.data.WorkflowRefDataSet;
import fr.paris.lutece.e2e.tests.macro.workflow.ActivateWorkflowMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.AddActionMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.AddStateMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.CreateWorkflowMacroTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

/**
 * Brique macro : executer une action de workflow sur la derniere reponse connue, depuis son detail.
 *
 * <p>Chemin reel : ligne de la reponse dans la multivue ({@code data-url} vers
 * {@code ManageDirectoryFormResponseDetails.jsp?view=view_form_response_details&...&id_form_response=N}), puis
 * bouton de l'action = formulaire POST {@code action=doProcessAction}, {@code id_action}, {@code id_form_response},
 * {@code _csrftoken} (LUT-34208). Effet prouve sur le detail relu (action historisee, plus proposee) puis dans la
 * multivue : la ligne de la reponse porte l'etat attendu. La multivue lit l'index Lucene : l'etat n'y apparait
 * qu'apres indexation (constate le 2026-10-29), d'ou l'indexation incrementale et la relecture bornee.</p>
 *
 * <p>Complement de {@code RunWorkflowActionOnResponse}, qui cherche un lien {@code doProcessAction} et se met en
 * skip sur forms upstream {@code develop} (constate le 2026-10-29). Lit : {@code ctx.lastResponseId}
 * ({@code VerifyResponseRecorded}).</p>
 */
@Epic("Forms")
@Feature("Reponses")
@Story("Executer une action de workflow")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ProcessWorkflowActionMacroTest extends MacroTest {

    @Step("Executer l'action {data.actionLabel} sur la reponse, etat attendu {data.expectedState}")
    public static void run(FormsContext ctx, WorkflowProcessDataSet data) {
        Assertions.assertNotNull(ctx.lastResponseId, "Une reponse doit etre connue (VerifyResponseRecorded)");
        int response = ctx.lastResponseId;
        String before = FormsNonRegSupport.multiviewRowText(ctx, response);
        Assertions.assertNotNull(before, "La reponse " + response + " doit etre listee dans la multivue");
        Assertions.assertFalse(before.contains(data.expectedState()), "Etat avant action deja egal a l'attendu : " + before);

        String url = (String) ctx.page.evaluate("id => { const e = [...document.querySelectorAll(\"[data-url*='id_form_response=']\")]"
            + ".find(x => new URLSearchParams(x.getAttribute('data-url').split('?')[1]).get('id_form_response') === String(id));"
            + " return e ? e.getAttribute('data-url') : null; }", response);
        FormsNonRegSupport.follow(ctx, url.replace("&amp;", "&"));
        assertOnDetail(ctx, response);
        long historyBefore = historyEntries(ctx, data.actionLabel());
        Locator button = ctx.page.locator("form:has(input[name='action'][value='doProcessAction'])"
            + ":has(input[name='id_form_response'][value='" + response + "']) button[type='submit']")
            .filter(new Locator.FilterOptions().setHasText(data.actionLabel()));
        Assertions.assertEquals(1, button.count(), "Action « " + data.actionLabel() + " » attendue sur le detail de la reponse "
            + response + " ; actions : " + ctx.page.locator("form:has(input[value='doProcessAction']) button").allInnerTexts());
        button.click();
        ctx.page.waitForLoadState();

        // 1) Detail relu (marqueur d'ecran d'abord) : une entree « Action <libelle> » de plus dans l'historique, et
        //    l'action n'est plus proposee (etat d'arrivee sans cette action). Revue du 2026-10-30, constat 11.
        FormsNonRegSupport.follow(ctx, url.replace("&amp;", "&"));
        assertOnDetail(ctx, response);
        Assertions.assertEquals(historyBefore + 1, historyEntries(ctx, data.actionLabel()),
            "L'historique de la reponse doit compter une entree « Action " + data.actionLabel() + " » de plus : "
                + ctx.page.locator("h1, h2, h3, h4, h5").allInnerTexts());
        Assertions.assertEquals(0, button.count(), "L'action « " + data.actionLabel() + " » ne doit plus etre proposee");

        // 2) Multivue (index Lucene, mis a jour par l'indexation incrementale) : etat d'arrivee.
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        String after;
        do {
            FormsNonRegSupport.triggerIncrementalIndexing(ctx);
            after = FormsNonRegSupport.multiviewRowText(ctx, response);
            if (after != null && after.contains(data.expectedState())) {
                return;
            }
            ctx.page.waitForTimeout(5_000);
        } while (System.currentTimeMillis() < deadline);
        Assertions.fail("Etat attendu '" + data.expectedState() + "' sur la reponse " + response + " dans la multivue apres "
            + POLL_TIMEOUT_MS / 1000 + " s ; ligne : " + after);
    }

    private static final long POLL_TIMEOUT_MS = 120_000;

    /** Ecran de detail de la reponse : bloc {@code #form-response-details} et fil d'Ariane « n° <id> ». */
    private static void assertOnDetail(FormsContext ctx, int response) {
        Assertions.assertEquals(1, ctx.page.locator("#form-response-details").count(),
            "Le detail de la reponse " + response + " doit etre affiche (url " + ctx.page.url() + ")");
        String breadcrumb = ctx.page.locator(".breadcrumb").innerText().replaceAll("\\s+", " ");
        Assertions.assertTrue(Pattern.compile("n° " + response + "(?!\\d)").matcher(breadcrumb).find(),
            "Le fil d'Ariane doit nommer la reponse " + response + " : " + breadcrumb);
    }

    /** Entrees de l'historique de la reponse pour l'action : titres « Action <libelle> <date> » (releve du 2026-10-29). */
    private static long historyEntries(FormsContext ctx, String actionLabel) {
        return ctx.page.locator("h1, h2, h3, h4, h5").allInnerTexts().stream()
            .map(t -> t.replaceAll("\\s+", " ").trim()).filter(t -> t.startsWith("Action " + actionLabel + " ")).count();
    }

    @Test
    @DisplayName("Traiter une reponse saisie en BO avec un workflow a deux etats")
    void standaloneProcessWorkflowAction() {
        FormsContext ctx = newLoggedInContext();
        WorkflowContext wf = new WorkflowContext(ctx.page, ctx.baseUrl, ctx.runSuffix);
        try {
            CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults());
            AddStateMacroTest.run(wf, StateDataSet.initial("A traiter"));
            AddStateMacroTest.run(wf, StateDataSet.of("Traite"));
            AddActionMacroTest.run(wf, ActionDataSet.of("Traiter", 0, 1));
            ActivateWorkflowMacroTest.run(wf);
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Lieu"));
            AssociateWorkflowMacroTest.run(ctx, WorkflowRefDataSet.of(wf.workflowName));
            OpenFormResponseBOMacroTest.run(ctx);
            FillFieldBOMacroTest.run(ctx, FieldValueDataSet.text("Lieu", "Square du Temple"));
            ValidateResponseBOMacroTest.run(ctx);
            VerifyResponseRecordedMacroTest.run(ctx, ResponseCheckDataSet.defaults());
            run(ctx, WorkflowProcessDataSet.of("Traiter", "Traite"));
        } finally {
            FormsNonRegSupport.cleanup(ctx, wf);
        }
    }
}
