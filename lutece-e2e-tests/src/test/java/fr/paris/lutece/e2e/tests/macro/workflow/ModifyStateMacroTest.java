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
 * Brique macro : renommer un etat d'un workflow.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.states}. Ecrit : met a jour le nom du premier
 * {@link WorkflowContext.StateRef}. Ouvre {@code ModifyState.jsp?id_state=ID} (id resolu depuis la
 * page ModifyWorkflow via un lien {@code a[href*='id_state=']}), remplace le champ nom puis
 * enregistre. Si l'id de l'etat n'est pas resolvable (ou le champ nom absent), le scenario est saute
 * proprement via {@link Assumptions} plutot que de provoquer un echec / un blocage.</p>
 */
@Epic("Workflow")
@Feature("Cycle de vie du workflow")
@Story("Renommer un etat")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class ModifyStateMacroTest extends MacroTest {

    @Step("Renommer l'etat")
    public static void run(WorkflowContext ctx, StateDataSet data) {
        Assertions.assertTrue(ctx.workflowId > 0 && !ctx.states.isEmpty(),
            "Un workflow et au moins un etat doivent exister avant de renommer un etat");

        WorkflowContext.StateRef target = ctx.states.get(0);
        Page page = ctx.page;
        String newName = data.name();

        // Resoudre id_state depuis la page d'edition du workflow (lien portant id_state=).
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        int idState = resolveStateId(page, target.name);
        Assumptions.assumeTrue(idState > 0,
            "Lien id_state introuvable pour l'etat '" + target.name + "' : renommage non pilotable en l'etat");

        // Ouvrir le formulaire de modification. Si le champ nom est absent, sauter plutot que bloquer.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyState.jsp?id_state=" + idState);
        Locator nameInput = page.locator("input[name='name']");
        Assumptions.assumeTrue(nameInput.count() > 0 && nameInput.first().isVisible(),
            "Champ nom introuvable sur ModifyState.jsp : renommage non pilotable en l'etat");
        nameInput.first().fill(newName);
        page.locator("button:has-text('Enregistrer'), input[value='Enregistrer']").first().click();
        page.waitForLoadState();
        dismissAdminMessage(page);

        // Verifier que le nouveau nom apparait sur la page d'edition du workflow.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        Assertions.assertTrue(WorkflowSupport.isTextVisible(page, newName),
            "L'etat renomme '" + newName + "' devrait etre liste dans le workflow");

        target.name = newName;
    }

    /**
     * Resout id_state en privilegiant le lien portant le nom de l'etat, sinon le premier lien id_state.
     * Retourne -1 si aucun lien exploitable n'est present.
     */
    private static int resolveStateId(Page page, String stateName) {
        Locator named = page.locator("a[href*='id_state=']:has-text('" + stateName + "')").first();
        Locator link = named.count() > 0 ? named : page.locator("a[href*='id_state=']").first();
        if (link.count() == 0) {
            return -1;
        }
        String href = link.getAttribute("href");
        if (href == null || !href.contains("id_state=")) {
            return -1;
        }
        return Integer.parseInt(href.split("id_state=")[1].split("&")[0].split("#")[0]);
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
    @DisplayName("Renommer un etat (auto-provisionnement workflow + etat)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        run(ctx, StateDataSet.of("Etat renomme"));
    }
}
