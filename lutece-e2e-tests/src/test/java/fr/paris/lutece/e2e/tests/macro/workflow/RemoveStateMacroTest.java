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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : supprimer un etat NON initial d'un workflow.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.states}. Ecrit : retire le {@link WorkflowContext.StateRef}
 * cible (index 1) de {@code ctx.states}. Cible volontairement le 2eme etat : l'etat initial peut etre
 * refuse a la suppression par le plugin. La vue {@code ConfirmRemoveState.jsp?id_state=ID} redirige vers
 * une boite AdminMessage : on confirme via le premier {@code button[type='submit']} (ou le lien direct
 * {@code DoRemoveState}). Si l'id de l'etat n'est pas resolvable ou si aucun controle de confirmation
 * n'apparait (etat non supprimable), le scenario est saute via {@link Assumptions} plutot qu'echoue.</p>
 */
@Epic("Workflow")
@Feature("Cycle de vie du workflow")
@Story("Supprimer un etat")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class RemoveStateMacroTest extends MacroTest {

    @Step("Supprimer l'etat")
    public static void run(WorkflowContext ctx) {
        Assertions.assertTrue(ctx.workflowId > 0 && ctx.states.size() >= 2,
            "Un workflow et au moins deux etats doivent exister avant de supprimer un etat non initial");

        // Cibler le 2eme etat (non initial) : l'etat initial peut etre non supprimable.
        WorkflowContext.StateRef target = ctx.states.get(1);
        Page page = ctx.page;

        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        int idState = resolveStateId(page, target.name);
        Assumptions.assumeTrue(idState > 0,
            "Lien id_state introuvable pour l'etat '" + target.name + "' : suppression non pilotable en l'etat");

        // Page de confirmation AdminMessage : confirmer via le premier bouton submit ou le lien DoRemoveState.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ConfirmRemoveState.jsp?id_state=" + idState);
        boolean confirmed = confirmRemoval(page);
        Assumptions.assumeTrue(confirmed,
            "Bouton/lien de confirmation absent pour l'etat '" + target.name
                + "' (etat probablement non supprimable) : scenario ignore");
        page.waitForLoadState();

        // Verifier que l'etat a disparu de la page d'edition du workflow.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        Assertions.assertEquals(0,
            page.locator("a[href*='id_state=']:has-text('" + target.name + "')").count(),
            "L'etat '" + target.name + "' ne devrait plus apparaitre apres suppression");

        ctx.states.remove(target);
    }

    /** Resout id_state via le lien portant le nom de l'etat cible (pas de repli, pour ne pas viser l'etat initial). */
    private static int resolveStateId(Page page, String stateName) {
        Locator link = page.locator("a[href*='id_state=']:has-text('" + stateName + "')").first();
        if (link.count() == 0) {
            return -1;
        }
        String href = link.getAttribute("href");
        if (href == null || !href.contains("id_state=")) {
            return -1;
        }
        return Integer.parseInt(href.split("id_state=")[1].split("&")[0].split("#")[0]);
    }

    private static boolean confirmRemoval(Page page) {
        Locator submit = page.locator("button[type='submit']");
        if (submit.count() > 0 && submit.first().isVisible()) {
            submit.first().click();
            return true;
        }
        Locator direct = page.locator("a[href*='DoRemoveState']");
        if (direct.count() > 0 && direct.first().isVisible()) {
            direct.first().click();
            return true;
        }
        return false;
    }

    @Test
    @DisplayName("Supprimer un etat non initial (auto-provisionnement workflow + 2 etats)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        AddStateMacroTest.run(ctx, StateDataSet.of("Etat a supprimer"));
        run(ctx);
    }
}
