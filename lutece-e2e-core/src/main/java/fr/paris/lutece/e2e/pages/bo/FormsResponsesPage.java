package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour la page des reponses d'un formulaire (MultiviewForms).
 */
public class FormsResponsesPage {

    private final Page page;
    private final String baseUrl;

    public FormsResponsesPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Clique sur la reponse identifiee par le nom du formulaire dans le tableau.
     */
    public FormsResponsesPage clickFirstResponse(String formName) {
        page.getByRole(AriaRole.CELL,
            new Page.GetByRoleOptions().setName(formName)).first().click();
        return this;
    }

    /**
     * Clique sur l'action workflow (ex: "Valider Valider").
     */
    public FormsResponsesPage clickWorkflowAction(String actionLabel) {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName(actionLabel)).click();
        return this;
    }

    /**
     * Confirme l'action du workflow.
     */
    public void confirmAction() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Valider")).click();
    }

    /**
     * Verifie que la page des reponses est affichee.
     */
    public boolean isDisplayed() {
        page.waitForLoadState();
        return page.url().contains("forms") || page.url().contains("Forms");
    }
}
