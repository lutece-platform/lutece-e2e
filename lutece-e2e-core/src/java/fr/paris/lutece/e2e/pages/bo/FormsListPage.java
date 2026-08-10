package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour la liste des formulaires.
 */
public class FormsListPage {

    private final Page page;
    private final String baseUrl;

    public FormsListPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Navigue vers la page de gestion des formulaires.
     */
    public FormsListPage navigateTo() {
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageForms.jsp?view=manageForms");
        page.waitForLoadState();
        return this;
    }

    /**
     * Recupere l'identifiant du formulaire a partir de son nom dans la liste.
     * Cherche le lien view=modifyForm&id_form=XXX associe au nom du formulaire.
     */
    public String getFormId(String formName) {
        Locator editLink = page.locator("a[href*='view=manageSteps'][title='" + formName + "']");
        String href = editLink.getAttribute("href");
        if (href != null && href.contains("id_form=")) {
            return href.split("id_form=")[1].split("&")[0].split("#")[0];
        }
        return null;
    }

    /**
     * Navigue vers la page de modification (etapes) du formulaire identifie par son nom.
     * Extrait l'id_form depuis la liste puis navigue vers ManageSteps.
     */
    public FormsEditPage clickEditForm(String formName) {
        String formId = getFormId(formName);
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageSteps.jsp?view=manageSteps&id_form=" + formId);
        page.waitForLoadState();
        return new FormsEditPage(page, baseUrl);
    }

    /**
     * Verifie que la page de gestion des formulaires est affichee.
     */
    public boolean isDisplayed() {
        page.waitForLoadState();
        return page.url().contains("ManageForms") ||
               page.getByRole(AriaRole.LINK,
                   new Page.GetByRoleOptions().setName("Ajouter un Formulaire")).first().isVisible();
    }

    /**
     * Navigue vers la page de creation d'un formulaire.
     */
    public FormsCreationPage clickAddForm() {
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageForms.jsp?view=createForm");
        page.waitForLoadState();
        return new FormsCreationPage(page, baseUrl);
    }

    /**
     * Accede au formulaire en front office identifie par son nom.
     * Extrait l'id_form depuis la liste puis navigue directement vers la vue FO.
     */
    public FormsFrontOfficePage clickAccessFrontOfficeForm(String formName) {
        String formId = getFormId(formName);
        page.navigate(baseUrl + "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + formId);
        page.waitForLoadState();
        return new FormsFrontOfficePage(page, baseUrl);
    }

    /**
     * Navigue vers la page multivue des reponses des formulaires.
     */
    public FormsResponsesPage clickViewResponses() {
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/MultiviewForms.jsp");
        page.waitForLoadState();
        return new FormsResponsesPage(page, baseUrl);
    }

    /**
     * Ouvre le menu dropdown d'actions du formulaire identifie par son nom.
     */
    public FormsListPage openActionsDropdown(String formName) {
        page.locator(".card")
            .filter(new Locator.FilterOptions().setHasText(formName))
            .locator(".form-actions .btn.dropdown-toggle").click();
        return this;
    }

    /**
     * Clique sur "Editer la publication du formulaire" dans le dropdown.
     */
    public FormsListPage clickEditPublication() {
        // TODO(url-refactor): confirm URL — the target ManageForms.jsp?view=modifyPublication&id_form=ID
        // needs an id_form which is not available in this signature (relies on the currently open
        // actions dropdown of a specific form). Kept as a click until an id is threaded through.
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Editer la publication du")).click();
        return this;
    }

    /**
     * Navigue vers la page d'accueil LUTECE.
     */
    public FormsListPage clickLuteceHome() {
        // TODO(url-refactor): confirm URL — the LUTECE admin home URL is a core page and is not
        // among the confirmed Forms/Workflow URL patterns. Kept as a click to avoid guessing.
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("LUTECE").setExact(true)).click();
        return this;
    }
}
