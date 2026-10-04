package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Helpers Playwright partages par les briques macro Workflow (plugin en ancien style JSP-par-operation).
 */
public final class WorkflowSupport {

    public static final String WF = "/jsp/admin/plugins/workflow/";

    private WorkflowSupport() {}

    public static void navigate(WorkflowContext ctx, String relativeUrl) {
        ctx.page.navigate(ctx.baseUrl + relativeUrl);
        ctx.page.waitForLoadState();
    }

    /**
     * Navigue vers la liste des workflows et retourne l'id du workflow dont le nom correspond, ou -1.
     */
    public static int extractWorkflowId(WorkflowContext ctx, String workflowName) {
        // La liste est paginee cote serveur : sur un site ou les workflows s'accumulent, le
        // dernier cree se retrouve au-dela de la premiere page et parait introuvable. On demande
        // donc une page assez grande pour que tous soient dans le document.
        navigate(ctx, WF + "ManageWorkflow.jsp?items_per_page=100000");
        // Le libelle vient de l'appelant et peut contenir une apostrophe, qui refermerait la
        // chaine d'un selecteur ':has-text(...)' et casserait la resolution. Le filtre de
        // Playwright prend une chaine Java, jamais relue comme selecteur.
        Locator link = ctx.page.locator("a[href*='id_workflow=']")
            .filter(new Locator.FilterOptions().setHasText(workflowName)).first();
        try {
            link.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(10_000));
        } catch (RuntimeException notFound) {
            return -1;
        }
        String href = link.getAttribute("href");
        if (href == null || !href.contains("id_workflow=")) {
            return -1;
        }
        return Integer.parseInt(href.split("id_workflow=")[1].split("&")[0].split("#")[0]);
    }

    /** Vrai si le texte est visible sur la page (helper d'assertion). */
    public static boolean isTextVisible(Page page, String text) {
        try {
            Locator loc = page.getByText(text);
            return loc.count() > 0 && loc.first().isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
