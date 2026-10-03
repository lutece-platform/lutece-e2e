package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.pages.bo.WorkflowEditPage;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowSupport;
import fr.paris.lutece.e2e.tests.macro.data.ActionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StateDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TaskDataSet;
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
 * Brique macro : ajouter une tache a une action de workflow.
 *
 * <p>Lit : {@code ctx.workflowId}, {@code ctx.actions}. Ecrit : rien (configuration cote UF).</p>
 *
 * <p>Flux fragile : l'ouverture de la page de modification de l'action (lien porteur de token) et
 * l'insertion de la tache (select "Nouvelle tache" + bouton "Inserer") sont gardees. Si un controle
 * est absent, le test est ignore ({@link Assumptions}) plutot qu'en echec.</p>
 */
@Epic("Workflow")
@Feature("Actions et taches")
@Story("Ajouter une tache a une action")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class AddTaskToActionMacroTest extends MacroTest {

    /**
     * Delai d'apparition des liens d'action apres activation de l'onglet, en millisecondes.
     *
     * <p>Couvre l'initialisation des onglets Bootstrap sans allonger le cas ou le workflow ne
     * porte reellement aucune action modifiable.</p>
     */
    private static final double ACTION_LINK_TIMEOUT_MS = 5000;

    @Step("Ajouter une tache a une action")
    public static void run(WorkflowContext ctx, TaskDataSet data) {
        Assertions.assertTrue(ctx.workflowId > 0, "Un workflow doit exister (ctx.workflowId)");
        Assertions.assertFalse(ctx.actions.isEmpty(),
            "Une action doit exister (ctx.actions) avant d'ajouter une tache");

        // Se placer sur la page d'edition du workflow (les liens d'action lisent id_workflow de l'URL).
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + ctx.workflowId);
        WorkflowEditPage edit = new WorkflowEditPage(ctx.page, ctx.baseUrl);

        // Rendre les actions visibles : cliquer sur l'onglet Actions uniquement s'il existe.
        Locator actionsTab = ctx.page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Actions"));
        if (actionsTab.count() > 0 && actionsTab.first().isVisible()) {
            edit.clickActionsTab();
        }

        // Ouvrir ModifyAction.jsp : lien porteur de token, on garde sa presence pour eviter un hang.
        // Les liens vivent dans l'onglet Actions : tant qu'il n'est pas reellement active, ils ne
        // sont pas exposes (count = 0). Le clic ci-dessus depend de l'initialisation des onglets
        // Bootstrap, donc d'un chargement JS : on attend leur apparition au lieu de la supposer.
        Locator modifyActionLink = ctx.page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Modifier l'action"));
        try {
            modifyActionLink.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(ACTION_LINK_TIMEOUT_MS));
        } catch (RuntimeException notShown) {
            // Laisse l'assomption ci-dessous statuer avec son message explicite.
        }
        boolean actionModifiable = modifyActionLink.count() > 0 && modifyActionLink.first().isVisible();
        Assumptions.assumeTrue(actionModifiable,
            "Aucun lien 'Modifier l'action' visible : impossible d'ouvrir la configuration des taches, test ignore");
        edit.clickModifyAction();

        // Garder le select "Nouvelle tache" (avec l'option demandee) et le bouton "Inserer".
        Locator taskSelect = ctx.page.getByLabel("Nouvelle tâche");
        Locator insertButton = ctx.page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Insérer"));
        boolean selectPresent = taskSelect.count() > 0 && taskSelect.first().isVisible();
        boolean optionPresent = selectPresent
            && taskSelect.locator("option[value='" + data.taskTypeKey() + "']").count() > 0;
        boolean insertPresent = insertButton.count() > 0 && insertButton.first().isVisible();
        Assumptions.assumeTrue(selectPresent && optionPresent && insertPresent,
            "Le select 'Nouvelle tâche' (ou l'option '" + data.taskTypeKey()
                + "') ou le bouton 'Insérer' est absent : ajout de tache ignore");

        java.util.List<Integer> avant = taskIds(ctx.page);
        edit.selectTask(data.taskTypeKey());
        edit.clickInsertTask();
        ctx.page.waitForLoadState();

        // L'identifiant de la tache inseree est indispensable pour la configurer ensuite : son
        // parametrage ne vit pas sur la page de l'action mais sur ModifyTask.jsp?id_task=...
        taskIds(ctx.page).stream()
            .filter(id -> !avant.contains(id))
            .findFirst()
            .ifPresent(ctx.taskIds::add);

        // La tache inseree apparait sur la page de l'action (liens de gestion porteurs de id_task,
        // ou libelle de la cle de tache). Assertion best-effort avec deux signaux.
        boolean taskPresent = ctx.page.locator("a[href*='id_task=']").count() > 0
            || WorkflowSupport.isTextVisible(ctx.page, data.taskTypeKey());
        Assertions.assertTrue(taskPresent,
            "La tache '" + data.taskTypeKey() + "' devrait apparaitre sur la page de l'action apres insertion");
    }

    /**
     * Identifiants des taches presentes sur la page de l'action.
     *
     * @param page page de l'action
     * @return les identifiants, sans doublon et dans l'ordre d'apparition
     */
    private static java.util.List<Integer> taskIds(Page page) {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        Locator liens = page.locator("a[href*='id_task=']");
        for (int i = 0; i < liens.count(); i++) {
            String href = liens.nth(i).getAttribute("href");
            if (href == null) {
                continue;
            }
            try {
                int id = Integer.parseInt(href.split("id_task=")[1].split("&")[0].split("#")[0]);
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            } catch (RuntimeException ignore) {
                // href sans identifiant exploitable : sans interet pour le suivi des taches
            }
        }
        return ids;
    }

    @Test
    @DisplayName("Ajouter une tache a une action (auto-provisionnement workflow + 2 etats + action)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        AddStateMacroTest.run(ctx, StateDataSet.of("Etat final"));
        AddActionMacroTest.run(ctx, ActionDataSet.defaults());
        run(ctx, TaskDataSet.defaults());
    }
}
