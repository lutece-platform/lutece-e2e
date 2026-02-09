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
        page.locator("a:has-text('Créer un workflow'), a:has-text('Creer un workflow')").first().click();
        page.waitForLoadState();
        return new WorkflowCreationFormPage(page, baseUrl);
    }

    /**
     * Clique sur le lien pour activer le workflow identifie par son nom.
     * Le bouton vert (play) active le workflow.
     */
    public WorkflowListPage clickActivateWorkflow(String workflowName) {
        page.waitForLoadState();
        // Trouver la ligne contenant le workflow et cliquer sur le bouton vert (activer)
        // Les boutons sont dans l'ordre: bleu (modifier), bleu clair (télécharger), bleu (copier), rouge (supprimer), vert (activer)
        var workflowLink = page.locator("a:has-text('" + workflowName + "')").first();
        // Naviguer vers le parent et trouver le bouton vert
        workflowLink.locator("xpath=ancestor::*[contains(@class, 'row') or contains(@class, 'list-group-item')][1]")
            .locator("button.btn-success, a.btn-success, button:has(.fa-play), a:has(.fa-play)").first().click();
        page.waitForLoadState();
        return this;
    }
}
