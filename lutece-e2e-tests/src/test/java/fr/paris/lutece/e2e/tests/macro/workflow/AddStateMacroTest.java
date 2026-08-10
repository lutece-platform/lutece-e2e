package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowSupport;
import fr.paris.lutece.e2e.tests.macro.data.StateDataSet;
import fr.paris.lutece.e2e.tests.macro.data.WorkflowDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : ajouter un etat a un workflow.
 *
 * <p>Lit : {@code ctx.workflowId}. Ecrit : ajoute un {@link WorkflowContext.StateRef} a
 * {@code ctx.states}.</p>
 */
@Epic("Workflow")
@Feature("Etats")
@Story("Ajouter un etat")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class AddStateMacroTest extends MacroTest {

    @Step("Ajouter un etat")
    public static void run(WorkflowContext ctx, StateDataSet data) {
        Assertions.assertTrue(ctx.workflowId > 0,
            "Un workflow doit exister (ctx.workflowId) avant d'ajouter un etat");

        // Creation directe de l'etat (flux verifie) : CreateState.jsp?id_workflow= + saisie +
        // bouton submit name='save' (le libelle n'est qu'un title, pas du texte).
        Page page = ctx.page;
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "CreateState.jsp?id_workflow=" + ctx.workflowId);
        page.locator("input[name='name']").fill(data.name());
        Locator desc = page.locator("textarea[name='description']");
        if (desc.count() > 0) {
            desc.first().fill(data.description());
        }
        if (data.initial()) {
            page.locator("input#is_initial_state, input[name='is_initial_state']").first().check();
        }
        page.locator("button[name='save']").first().click();
        page.waitForLoadState();

        // Succes = DoCreateState a redirige hors de CreateState (vers ModifyWorkflow). Une erreur de
        // validation resterait sur CreateState.jsp. Signal fiable (le rendu de l'onglet Etats n'est
        // pas scannable de facon stable pour un workflow fraichement cree).
        Assertions.assertFalse(page.url().contains("CreateState.jsp"),
            "La creation de l'etat '" + data.name() + "' aurait du rediriger hors de CreateState ; url: "
                + page.url());
        ctx.states.add(new WorkflowContext.StateRef(data.name(), data.initial()));
    }

    @Test
    @DisplayName("Ajouter un etat (auto-provisionnement du workflow)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        run(ctx, StateDataSet.initial("Etat initial"));
    }
}
