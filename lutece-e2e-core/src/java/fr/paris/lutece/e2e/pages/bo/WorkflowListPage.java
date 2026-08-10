package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour la liste des workflows.
 */
public class WorkflowListPage {

    private final Page page;
    private final String baseUrl;

    public WorkflowListPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Verifie que la page de gestion des workflows est affichee.
     */
    public boolean isDisplayed() {
        page.waitForLoadState();
        // Vérifier plusieurs indicateurs possibles
        return page.url().contains("ManageWorkflow") ||
               page.locator("a:has-text('Créer un workflow'), a:has-text('Creer un workflow')").first().isVisible() ||
               page.locator("text=Gestion des workflows").first().isVisible();
    }

    /**
     * Clique sur le lien pour creer un nouveau workflow.
     */
    public WorkflowCreationFormPage clickCreateWorkflow() {
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/CreateWorkflow.jsp");
        page.waitForLoadState();
        return new WorkflowCreationFormPage(page, baseUrl);
    }

    /**
     * Clique sur le lien pour activer le workflow identifie par son nom.
     * Le bouton vert (play) active le workflow.
     */
    public WorkflowListPage clickActivateWorkflow(String workflowName) {
        page.waitForLoadState();
        String idWorkflow = getWorkflowId(workflowName);
        // getWorkflowId() a navigue vers ManageWorkflow : le lien d'activation reel y porte le token
        // de securite (DoEnableWorkflow.jsp?id_workflow=..&token=..). Sans ce token, l'activation
        // n'est pas persistee. On lit donc le vrai href puis on navigue dessus.
        if (idWorkflow != null) {
            Locator enableLink = page.locator(
                "a[href*='DoEnableWorkflow.jsp?id_workflow=" + idWorkflow + "&']").first();
            if (enableLink.count() > 0) {
                String href = enableLink.getAttribute("href");
                if (href != null) {
                    String url = href.startsWith("http")
                        ? href
                        : baseUrl + "/" + href.replaceFirst("^/", "");
                    page.navigate(url);
                    page.waitForLoadState();
                }
            }
        }
        return this;
    }

    /**
     * Recupere l'identifiant du workflow a partir de son nom dans la liste.
     * Navigue vers la liste puis extrait id_workflow du lien de la ligne correspondante.
     */
    private String getWorkflowId(String workflowName) {
        page.navigate(baseUrl + "/jsp/admin/plugins/workflow/ManageWorkflow.jsp");
        page.waitForLoadState();
        Locator link = page.locator("a[href*='id_workflow=']:has-text('" + workflowName + "')").first();
        String href = link.getAttribute("href");
        if (href != null && href.contains("id_workflow=")) {
            return href.split("id_workflow=")[1].split("&")[0].split("#")[0];
        }
        return null;
    }
}
