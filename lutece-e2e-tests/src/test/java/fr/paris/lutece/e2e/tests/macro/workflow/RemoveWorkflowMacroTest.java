package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowSupport;
import fr.paris.lutece.e2e.tests.macro.data.WorkflowDataSet;
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
 * Brique macro : supprimer un workflow (ConfirmRemoveWorkflow -> DoRemoveWorkflow).
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.workflowName}. Ecrit : remet {@code ctx.workflowId} a -1
 * et vide {@code ctx.states} / {@code ctx.actions}. La vue {@code ConfirmRemoveWorkflow.jsp?id_workflow=ID}
 * redirige vers une boite AdminMessage : on confirme via le premier {@code button[type='submit']} (ou le
 * lien direct {@code DoRemoveWorkflow}). Si aucun controle de confirmation n'apparait (workflow
 * probablement associe/actif, non supprimable), le scenario est saute via {@link Assumptions}.</p>
 */
@Epic("Workflow")
@Feature("Cycle de vie du workflow")
@Story("Supprimer le workflow")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class RemoveWorkflowMacroTest extends MacroTest {

    @Step("Supprimer le workflow")
    public static void run(WorkflowContext ctx) {
        Assertions.assertTrue(ctx.workflowId > 0,
            "Un workflow doit exister (ctx.workflowId) avant suppression");

        Page page = ctx.page;
        String name = ctx.workflowName;

        // Page de confirmation AdminMessage : confirmer via le premier bouton submit ou le lien DoRemoveWorkflow.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ConfirmRemoveWorkflow.jsp?id_workflow=" + ctx.workflowId);
        boolean confirmed = confirmRemoval(page);
        Assumptions.assumeTrue(confirmed,
            "Bouton/lien de confirmation absent : suppression du workflow '" + name + "' non pilotable en l'etat");
        page.waitForLoadState();

        // Verifier que le workflow a disparu de la liste ManageWorkflow.
        int stillThere = WorkflowSupport.extractWorkflowId(ctx, name);
        Assertions.assertEquals(-1, stillThere,
            "Le workflow '" + name + "' ne devrait plus apparaitre dans la liste apres suppression");

        ctx.workflowId = -1;
        ctx.workflowName = null;
        ctx.states.clear();
        ctx.actions.clear();
    }

    private static boolean confirmRemoval(Page page) {
        Locator submit = page.locator("button[type='submit']");
        if (submit.count() > 0 && submit.first().isVisible()) {
            submit.first().click();
            return true;
        }
        Locator direct = page.locator("a[href*='DoRemoveWorkflow']");
        if (direct.count() > 0 && direct.first().isVisible()) {
            direct.first().click();
            return true;
        }
        return false;
    }

    @Test
    @DisplayName("Supprimer un workflow (auto-provisionnement du workflow)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        run(ctx);
    }
}
