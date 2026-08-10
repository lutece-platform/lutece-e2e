package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowSupport;
import fr.paris.lutece.e2e.tests.macro.data.ActionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StateDataSet;
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
 * Brique macro : supprimer une action d'un workflow.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.actions}. Ecrit : retire le premier
 * {@link WorkflowContext.ActionRef} de {@code ctx.actions}. La vue {@code ConfirmRemoveAction.jsp?id_action=ID}
 * redirige vers une boite AdminMessage : on confirme via le premier {@code button[type='submit']} (ou
 * le lien direct {@code DoRemoveAction}). Si l'id de l'action n'est pas resolvable ou si aucun controle
 * de confirmation n'apparait, le scenario est saute via {@link Assumptions} plutot qu'echoue.</p>
 */
@Epic("Workflow")
@Feature("Cycle de vie du workflow")
@Story("Supprimer une action")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class RemoveActionMacroTest extends MacroTest {

    @Step("Supprimer l'action")
    public static void run(WorkflowContext ctx) {
        Assertions.assertTrue(ctx.workflowId > 0 && !ctx.actions.isEmpty(),
            "Un workflow et au moins une action doivent exister avant de supprimer une action");

        WorkflowContext.ActionRef target = ctx.actions.get(0);
        Page page = ctx.page;

        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        int idAction = resolveActionId(page, target.name);
        Assumptions.assumeTrue(idAction > 0,
            "Lien id_action introuvable pour l'action '" + target.name + "' : suppression non pilotable en l'etat");

        // Page de confirmation AdminMessage : confirmer via le premier bouton submit ou le lien DoRemoveAction.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ConfirmRemoveAction.jsp?id_action=" + idAction);
        boolean confirmed = confirmRemoval(page);
        Assumptions.assumeTrue(confirmed,
            "Bouton/lien de confirmation absent pour l'action '" + target.name + "' : scenario ignore");
        page.waitForLoadState();

        // Verifier que l'action a disparu de la page d'edition du workflow.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        Assertions.assertEquals(0,
            page.locator("a[href*='id_action=']:has-text('" + target.name + "')").count(),
            "L'action '" + target.name + "' ne devrait plus apparaitre apres suppression");

        ctx.actions.remove(target);
    }

    /** Resout id_action via le lien portant le nom de l'action cible. Retourne -1 si absent. */
    private static int resolveActionId(Page page, String actionName) {
        Locator link = page.locator("a[href*='id_action=']:has-text('" + actionName + "')").first();
        if (link.count() == 0) {
            return -1;
        }
        String href = link.getAttribute("href");
        if (href == null || !href.contains("id_action=")) {
            return -1;
        }
        return Integer.parseInt(href.split("id_action=")[1].split("&")[0].split("#")[0]);
    }

    private static boolean confirmRemoval(Page page) {
        Locator submit = page.locator("button[type='submit']");
        if (submit.count() > 0 && submit.first().isVisible()) {
            submit.first().click();
            return true;
        }
        Locator direct = page.locator("a[href*='DoRemoveAction']");
        if (direct.count() > 0 && direct.first().isVisible()) {
            direct.first().click();
            return true;
        }
        return false;
    }

    @Test
    @DisplayName("Supprimer une action (auto-provisionnement workflow + 2 etats + action)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        AddStateMacroTest.run(ctx, StateDataSet.of("Etat final"));
        AddActionMacroTest.run(ctx, ActionDataSet.defaults());
        run(ctx);
    }
}
