package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
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
 * Brique macro : dupliquer un workflow depuis la liste ManageWorkflow.
 *
 * <p>Lit : {@code ctx.workflowId}. Ecrit : rien. Le lien de copie porte le token de securite
 * ({@code DoCopyWorkflow.jsp?id_workflow=..&token=..}) : on lit son href sur la liste puis on navigue
 * dessus (best-effort). Verifie que le nombre de workflows augmente. Si le lien de copie est absent, le
 * scenario est saute via {@link Assumptions} plutot qu'echoue.</p>
 */
@Epic("Workflow")
@Feature("Cycle de vie du workflow")
@Story("Dupliquer le workflow")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class DuplicateWorkflowMacroTest extends MacroTest {

    @Step("Dupliquer le workflow")
    public static void run(WorkflowContext ctx) {
        Assertions.assertTrue(ctx.workflowId > 0,
            "Un workflow doit exister (ctx.workflowId) avant duplication");

        int before = countWorkflows(ctx);

        // La liste ManageWorkflow expose deux variantes de rendu selon le theme :
        //  - variante "menu"    : un lien direct DoCopyWorkflow.jsp?id_workflow=..&token=..
        //  - variante "boutons" : un lien ConfirmCopyWorkflow.jsp?id_workflow=.. (flux en 2 etapes,
        //    la page de confirmation portant alors le lien DoCopyWorkflow avec token).
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ManageWorkflow.jsp");

        Locator directCopy = ctx.page.locator(
            "a[href*='DoCopyWorkflow'][href*='id_workflow=" + ctx.workflowId + "&'][href*='token=']").first();
        if (directCopy.count() > 0) {
            navigateToHref(ctx, directCopy.getAttribute("href"));
        } else {
            Locator confirmLink = ctx.page.locator(
                "a[href*='ConfirmCopyWorkflow'][href$='id_workflow=" + ctx.workflowId + "']").first();
            Assertions.assertTrue(confirmLink.count() > 0,
                "Aucun lien de copie (ConfirmCopyWorkflow/DoCopyWorkflow) pour id_workflow=" + ctx.workflowId);
            navigateToHref(ctx, confirmLink.getAttribute("href"));

            // Page de confirmation : la validation est un FORMULAIRE POST dont l'action porte le token
            // (<form action="..DoCopyWorkflow.jsp?id_workflow=..&token=.." method="post"><button
            // type="submit" title="OK">). On soumet ce formulaire (clic sur le bouton OK) plutot que de
            // chercher un lien <a> (qui n'existe pas sur cette page).
            Locator confirmSubmit = ctx.page.locator(
                "form[action*='DoCopyWorkflow'] button[type='submit'], "
                + "form[action*='DoCopyWorkflow'] input[type='submit']").first();
            Assertions.assertTrue(confirmSubmit.count() > 0,
                "Bouton de validation (form DoCopyWorkflow) introuvable sur la page de confirmation");
            confirmSubmit.click();
            ctx.page.waitForLoadState();
        }

        int after = countWorkflows(ctx);
        Assertions.assertTrue(after > before,
            "Un workflow duplique devrait apparaitre (avant=" + before + ", apres=" + after + ")");
    }

    /** Navigue vers un href (absolu ou relatif a baseUrl) et attend le chargement. */
    private static void navigateToHref(WorkflowContext ctx, String href) {
        Assertions.assertNotNull(href, "href du lien de copie introuvable");
        String url = href.startsWith("http") ? href : ctx.baseUrl + "/" + href.replaceFirst("^/", "");
        ctx.page.navigate(url);
        ctx.page.waitForLoadState();
    }

    /** Navigue vers la liste et compte les workflows (ids id_workflow distincts sur la page). */
    private static int countWorkflows(WorkflowContext ctx) {
        // ManageWorkflow est pagine cote serveur (50/page) : sans items_per_page, la liste plafonne a
        // 50 ids et le comptage avant/apres reste identique quand il y a deja >= 50 workflows (ce qui
        // masquait la copie). On force une grande taille de page pour voir tous les workflows.
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ManageWorkflow.jsp?items_per_page=100000");
        Locator links = ctx.page.locator("a[href*='id_workflow=']");
        int n = links.count();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < n; i++) {
            String href = links.nth(i).getAttribute("href");
            if (href != null && href.contains("id_workflow=")) {
                ids.add(href.split("id_workflow=")[1].split("&")[0].split("#")[0]);
            }
        }
        return ids.size();
    }

    @Test
    @DisplayName("Dupliquer un workflow (auto-provisionnement du workflow)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        run(ctx);
    }
}
