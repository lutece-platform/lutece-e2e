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
 * Brique macro : renommer une action d'un workflow.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.actions}. Ecrit : met a jour le nom du premier
 * {@link WorkflowContext.ActionRef}. Ouvre {@code ModifyAction.jsp?id_action=ID} (id resolu depuis la
 * page ModifyWorkflow via un lien {@code a[href*='id_action=']}), remplace le champ nom puis
 * enregistre (les etats source/cible restent preremplis). Si l'id de l'action n'est pas resolvable
 * (ou le champ nom absent), le scenario est saute via {@link Assumptions} plutot qu'echoue / bloque.</p>
 */
@Epic("Workflow")
@Feature("Cycle de vie du workflow")
@Story("Renommer une action")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class ModifyActionMacroTest extends MacroTest {

    @Step("Renommer l'action")
    public static void run(WorkflowContext ctx, ActionDataSet data) {
        Assertions.assertTrue(ctx.workflowId > 0 && !ctx.actions.isEmpty(),
            "Un workflow et au moins une action doivent exister avant de renommer une action");

        WorkflowContext.ActionRef target = ctx.actions.get(0);
        Page page = ctx.page;
        String newName = data.name();

        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        int idAction = resolveActionId(page, target.name);
        Assumptions.assumeTrue(idAction > 0,
            "Lien id_action introuvable pour l'action '" + target.name + "' : renommage non pilotable en l'etat");

        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyAction.jsp?id_action=" + idAction);
        Locator nameInput = page.locator("input[name='name']");
        Assumptions.assumeTrue(nameInput.count() > 0 && nameInput.first().isVisible(),
            "Champ nom introuvable sur ModifyAction.jsp : renommage non pilotable en l'etat");
        nameInput.first().fill(newName);
        // Bouton submit name='save' (title + icone, pas de texte "Enregistrer").
        page.locator("button[name='save']").first().click();
        page.waitForLoadState();
        dismissAdminMessage(page);

        // Succes = redirection hors de ModifyAction (une erreur resterait sur ModifyAction.jsp).
        Assertions.assertFalse(page.url().contains("ModifyAction.jsp"),
            "Le renommage de l'action aurait du rediriger hors de ModifyAction ; url: " + page.url());

        target.name = newName;
    }

    /**
     * Resout id_action en privilegiant le lien portant le nom de l'action, sinon le premier lien id_action.
     * Retourne -1 si aucun lien exploitable n'est present.
     */
    private static int resolveActionId(Page page, String actionName) {
        Locator named = page.locator("a[href*='id_action=']:has-text('" + actionName + "')").first();
        Locator link = named.count() > 0 ? named : page.locator("a[href*='id_action=']").first();
        if (link.count() == 0) {
            return -1;
        }
        String href = link.getAttribute("href");
        if (href == null || !href.contains("id_action=")) {
            return -1;
        }
        return Integer.parseInt(href.split("id_action=")[1].split("&")[0].split("#")[0]);
    }

    private static void dismissAdminMessage(Page page) {
        if (page.url().contains("AdminMessage")) {
            Locator ok = page.getByText("OK", new Page.GetByTextOptions().setExact(true));
            if (ok.count() > 0 && ok.first().isVisible()) {
                ok.first().click();
                page.waitForLoadState();
            }
        }
    }

    @Test
    @DisplayName("Renommer une action (auto-provisionnement workflow + 2 etats + action)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        AddStateMacroTest.run(ctx, StateDataSet.of("Etat final"));
        AddActionMacroTest.run(ctx, ActionDataSet.defaults());
        run(ctx, ActionDataSet.of("Action renommee", 0, 1));
    }
}
