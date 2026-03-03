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
     * Clique sur le lien de modification du formulaire identifie par son nom.
     */
    public FormsEditPage clickEditForm(String formName) {
        page.locator("a[href*='view=manageSteps'][title='" + formName + "']").click();
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
     * Clique sur "Ajouter un Formulaire".
     */
    public FormsCreationPage clickAddForm() {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Ajouter un Formulaire")).first().click();
        return new FormsCreationPage(page, baseUrl);
    }

    /**
     * Accede au formulaire en front office via le menu dropdown du formulaire identifie par son nom.
     * Retourne la page popup ouverte.
     */
    public FormsFrontOfficePage clickAccessFrontOfficeForm(String formName) {
        openActionsDropdown(formName);
        Page popup = page.waitForPopup(() -> {
            page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Accéder au formulaire FO")).click();
        });
        return new FormsFrontOfficePage(popup, baseUrl);
    }

    /**
     * Clique sur "Voir les reponses" dans le dropdown ouvert.
     */
    public FormsResponsesPage clickViewResponses() {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Voir les réponses")).first().click();
        return new FormsResponsesPage(page, baseUrl);
    }

    /**
     * Ouvre le menu dropdown d'actions du formulaire identifie par son nom.
     */
    public FormsListPage openActionsDropdown(String formName) {
        page.locator(".list-group-item")
            .filter(new Locator.FilterOptions().setHasText(formName))
            .locator(".dropdown > .btn-action").click();
        return this;
    }

    /**
     * Clique sur "Editer la publication du formulaire" dans le dropdown.
     */
    public FormsListPage clickEditPublication() {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Editer la publication du")).click();
        return this;
    }

    /**
     * Navigue vers la page d'accueil LUTECE.
     */
    public FormsListPage clickLuteceHome() {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("LUTECE").setExact(true)).click();
        return this;
    }
}
