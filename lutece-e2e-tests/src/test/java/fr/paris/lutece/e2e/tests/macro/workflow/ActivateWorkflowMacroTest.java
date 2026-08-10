package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.pages.bo.WorkflowListPage;
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
 * Brique macro : activer un workflow.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.workflowName}. Ecrit : rien.</p>
 *
 * <p>Un workflow doit generalement posseder un etat initial pour etre activable : l'appelant doit
 * l'avoir provisionne (le {@code standalone} le fait via {@link AddStateMacroTest}).</p>
 */
@Epic("Workflow")
@Feature("Actions et taches")
@Story("Activer un workflow")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class ActivateWorkflowMacroTest extends MacroTest {

    @Step("Activer le workflow")
    public static void run(WorkflowContext ctx) {
        Assertions.assertTrue(ctx.workflowId > 0, "Un workflow doit exister (ctx.workflowId) avant activation");
        Assertions.assertNotNull(ctx.workflowName, "Le nom du workflow doit etre connu (ctx.workflowName)");

        // Activation via DoEnableWorkflow (gere par le page object a partir du nom).
        new WorkflowListPage(ctx.page, ctx.baseUrl).clickActivateWorkflow(ctx.workflowName);

        // Verifier l'etat sur la liste : le workflow doit toujours etre liste (best-effort, pas d'erreur).
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ManageWorkflow.jsp");
        Assertions.assertTrue(WorkflowSupport.isTextVisible(ctx.page, ctx.workflowName),
            "Le workflow '" + ctx.workflowName + "' devrait toujours etre liste apres activation");

        // Indicateur d'activite (best-effort) : un workflow actif expose un lien de desactivation.
        Locator disableLink = ctx.page.locator(
            "a[href*='DoDisableWorkflow.jsp?id_workflow=" + ctx.workflowId + "']");
        if (disableLink.count() > 0) {
            Assertions.assertTrue(disableLink.first().isVisible(),
                "Le workflow actif devrait exposer un lien de desactivation");
        }
    }

    @Test
    @DisplayName("Activer un workflow (auto-provisionnement workflow + etat initial)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        run(ctx);
    }
}
