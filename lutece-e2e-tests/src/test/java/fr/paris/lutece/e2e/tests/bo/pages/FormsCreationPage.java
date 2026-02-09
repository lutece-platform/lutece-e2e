package fr.paris.lutece.e2e.tests.bo.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour la creation d'un formulaire.
 */
public class FormsCreationPage {

    private final Page page;
    private final String baseUrl;

    public FormsCreationPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Remplit le titre du formulaire.
     */
    public FormsCreationPage fillTitle(String title) {
        page.locator("input[name=\"title\"]").click();
        page.locator("input[name=\"title\"]").fill(title);
        return this;
    }

    /**
     * Definit la date de debut de disponibilite via flatpickr.
     * Utilise page.evaluate() pour contourner les problemes avec flatpickr.
     */
    public FormsCreationPage setStartDate(String date) {
        page.evaluate("(date) => {\n"
            + "  const inputs = document.querySelectorAll('input.flatpickr-input');\n"
            + "  if (inputs[0] && inputs[0]._flatpickr) {\n"
            + "    inputs[0]._flatpickr.setDate(date, true);\n"
            + "  }\n"
            + "}", date);
        return this;
    }

    /**
     * Definit la date de fin de disponibilite via flatpickr.
     */
    public FormsCreationPage setEndDate(String date) {
        page.evaluate("(date) => {\n"
            + "  const inputs = document.querySelectorAll('input.flatpickr-input');\n"
            + "  if (inputs[1] && inputs[1]._flatpickr) {\n"
            + "    inputs[1]._flatpickr.setDate(date, true);\n"
            + "  }\n"
            + "}", date);
        return this;
    }

    /**
     * Selectionne le workflow dans le dropdown par son libelle.
     */
    public FormsCreationPage selectWorkflow(String workflowName) {
        page.locator("#idWorkflow").selectOption(
            new SelectOption().setLabel(workflowName));
        return this;
    }

    /**
     * Clique sur "Creer le formulaire".
     */
    public FormsEditPage clickCreateForm() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Créer le formulaire")).click();
        return new FormsEditPage(page, baseUrl);
    }
}
