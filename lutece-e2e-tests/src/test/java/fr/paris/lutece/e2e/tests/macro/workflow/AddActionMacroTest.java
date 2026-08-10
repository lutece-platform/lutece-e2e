package fr.paris.lutece.e2e.tests.macro.workflow;

import fr.paris.lutece.e2e.pages.bo.WorkflowEditPage;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : ajouter une action reliant un etat source a un etat cible.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.states}. Ecrit : ajoute un
 * {@link WorkflowContext.ActionRef} a {@code ctx.actions}.</p>
 */
@Epic("Workflow")
@Feature("Actions")
@Story("Ajouter une action")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class AddActionMacroTest extends MacroTest {

    @Step("Ajouter une action")
    public static void run(WorkflowContext ctx, ActionDataSet data) {
        Assertions.assertTrue(ctx.workflowId > 0, "Un workflow doit exister (ctx.workflowId)");
        int maxIndex = Math.max(data.fromStateIndex(), data.toStateIndex());
        Assertions.assertTrue(ctx.states.size() > maxIndex,
            "Les etats source et cible doivent exister (ctx.states) avant de creer une action");

        String fromState = ctx.states.get(data.fromStateIndex()).name;
        String toState = ctx.states.get(data.toStateIndex()).name;

        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        new WorkflowEditPage(ctx.page, ctx.baseUrl).addAction(data.name(), data.description(), fromState, toState);

        // Succes = DoCreateAction a redirige hors de CreateAction (une erreur de validation resterait
        // sur CreateAction.jsp). Signal fiable, comme pour la creation d'etat.
        Assertions.assertFalse(ctx.page.url().contains("CreateAction.jsp"),
            "La creation de l'action '" + data.name() + "' aurait du rediriger hors de CreateAction ; url: "
                + ctx.page.url());
        ctx.actions.add(new WorkflowContext.ActionRef(data.name()));
    }

    @Test
    @DisplayName("Ajouter une action (auto-provisionnement workflow + 2 etats)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        AddStateMacroTest.run(ctx, StateDataSet.of("Etat final"));
        run(ctx, ActionDataSet.defaults());
    }
}
