package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
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
 * Brique macro : verifier qu'un workflow est actif.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.workflowName}. Ecrit : rien.</p>
 *
 * <p>L'indicateur d'activite est resolu depuis la liste des workflows : un workflow actif expose un
 * lien de desactivation (DoDisableWorkflow), un workflow inactif un lien d'activation (DoEnableWorkflow).
 * Si aucun indicateur n'est resoluble, le test est ignore ({@link Assumptions}).</p>
 */
@Epic("Workflow")
@Feature("Actions et taches")
@Story("Verifier qu'un workflow est actif")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class VerifyWorkflowActiveMacroTest extends MacroTest {

    @Step("Verifier que le workflow est actif")
    public static void run(WorkflowContext ctx) {
        Assertions.assertTrue(ctx.workflowId > 0, "Un workflow doit exister (ctx.workflowId)");
        Assertions.assertNotNull(ctx.workflowName, "Le nom du workflow doit etre connu (ctx.workflowName)");

        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ManageWorkflow.jsp");
        Assertions.assertTrue(WorkflowSupport.isTextVisible(ctx.page, ctx.workflowName),
            "Le workflow '" + ctx.workflowName + "' devrait etre liste");

        // Indicateur d'activite : lien de desactivation (actif) vs lien d'activation (inactif).
        Locator disableLink = ctx.page.locator(
            "a[href*='DoDisableWorkflow.jsp?id_workflow=" + ctx.workflowId + "']");
        Locator enableLink = ctx.page.locator(
            "a[href*='DoEnableWorkflow.jsp?id_workflow=" + ctx.workflowId + "']");
        boolean indicatorResolved = disableLink.count() > 0 || enableLink.count() > 0;
        Assertions.assertTrue(indicatorResolved,
            "Impossible de resoudre l'indicateur d'activite du workflow (lien Enable/Disable absent)");

        // L'activation (via DoEnableWorkflow avec token) est desormais persistee : un workflow actif
        // expose un lien de desactivation (DoDisableWorkflow) et n'expose plus de lien d'activation.
        Assertions.assertTrue(disableLink.count() > 0,
            "Le workflow '" + ctx.workflowName + "' devrait etre actif apres activation "
                + "(lien de desactivation DoDisableWorkflow attendu)");
    }

    @Test
    @DisplayName("Verifier qu'un workflow est actif (auto-provisionnement + activation)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        ActivateWorkflowMacroTest.run(ctx);
        run(ctx);
    }
}
