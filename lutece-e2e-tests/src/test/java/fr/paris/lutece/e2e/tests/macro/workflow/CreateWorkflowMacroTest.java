package fr.paris.lutece.e2e.tests.macro.workflow;

import fr.paris.lutece.e2e.pages.bo.WorkflowCreationFormPage;
import fr.paris.lutece.e2e.pages.bo.WorkflowListPage;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowSupport;
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
 * Brique macro : creer un workflow.
 *
 * <p>Lit : rien. Ecrit : {@code ctx.workflowId}, {@code ctx.workflowName}.</p>
 */
@Epic("Workflow")
@Feature("Cycle de vie du workflow")
@Story("Creer un workflow")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class CreateWorkflowMacroTest extends MacroTest {

    @Step("Creer le workflow")
    public static void run(WorkflowContext ctx, WorkflowDataSet data) {
        String name = data.name() + " " + ctx.runSuffix;

        WorkflowListPage list = new WorkflowListPage(ctx.page, ctx.baseUrl);
        WorkflowCreationFormPage form = list.clickCreateWorkflow();
        form.fillName(name).fillDescription(data.description());
        form.save();

        int id = WorkflowSupport.extractWorkflowId(ctx, name);
        Assertions.assertTrue(id > 0,
            "Le workflow '" + name + "' devrait apparaitre dans la liste avec un id_workflow");
        ctx.workflowId = id;
        ctx.workflowName = name;
    }

    @Test
    @DisplayName("Creer un workflow")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        run(ctx, WorkflowDataSet.defaults());
    }
}
