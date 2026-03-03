package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour l'edition d'un formulaire (etapes, questions, transitions, publication).
 */
public class FormsEditPage {

    private final Page page;
    private final String baseUrl;

    public FormsEditPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Clique sur l'onglet Etapes.
     */
    public FormsEditPage clickStepsTab() {
        page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Etapes")).click();
        return this;
    }

    /**
     * Clique sur l'onglet Parametres.
     */
    public FormsEditPage clickParametersTab() {
        page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Paramètres")).click();
        return this;
    }

    /**
     * Ajoute une etape via la page ManageSteps (navigation directe).
     */
    public FormsEditPage addStep(String formId, String title, boolean isFinal) {
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageSteps.jsp?view=createStep&id_form=" + formId);
        page.waitForLoadState();
        page.locator("#step-title").click();
        page.locator("#step-title").fill(title);
        if (isFinal) {
            page.getByRole(AriaRole.CHECKBOX,
                new Page.GetByRoleOptions().setName("Finale")).check();
        }
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("OK")).click();
        return this;
    }

    /**
     * Navigue vers la page de gestion des questions d'une etape.
     * Extrait l'id_step depuis le lien a.searchable contenant le nom de l'etape,
     * puis navigue vers ManageQuestions.jsp?view=manageQuestions&id_step=...
     */
    public FormsEditPage navigateToStepQuestions(String formId, String stepName) {
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageSteps.jsp?view=manageSteps&id_form=" + formId);
        page.waitForLoadState();
        Locator stepLink = page.locator("a.searchable",
            new Page.LocatorOptions().setHasText(stepName)).last();
        String href = stepLink.getAttribute("href");
        String stepId = href.split("id_step=")[1].split("&")[0];
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);
        page.waitForLoadState();
        return this;
    }

    /**
     * Ouvre l'edition d'une etape par son nom (clic sur le lien du nom).
     */
    public FormsEditPage openStepEditByName(String stepName) {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName(stepName).setExact(true)).last().click();
        return this;
    }

    /**
     * Clique sur "Modifier l'etape" (lien direct).
     */
    public FormsEditPage clickModifyStep() {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Modifier l'étape")).last().click();
        return this;
    }

    /**
     * Clique sur l'onglet "Liste des Questions".
     */
    public FormsEditPage clickQuestionsTab() {
        page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Liste des Questions")).click();
        return this;
    }

    /**
     * Clique sur l'onglet "Parametres de l'etape".
     */
    public FormsEditPage clickStepParametersTab() {
        page.getByRole(AriaRole.TAB,
            new Page.GetByRoleOptions().setName("Paramètres de l'étape")).click();
        return this;
    }

    /**
     * Ajoute une question de type texte court.
     */
    public FormsEditPage addTextQuestion(String title) {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Ajouter une question")).click();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Texte court")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).fill(title);
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        return this;
    }

    /**
     * Ajoute une question de type nombre.
     */
    public FormsEditPage addNumberQuestion(String title) {
        page.locator("#question-list").getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Actions")).first().click();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Ajouter une question")).click();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Nombre")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).fill(title);
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        return this;
    }

    /**
     * Ajoute une question de type date.
     */
    public FormsEditPage addDateQuestion(String title) {
        page.locator("#question-list").getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Actions")).first().click();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Ajouter une question")).click();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Date")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).fill(title);
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        return this;
    }

    /**
     * Ajoute une question de type commentaire avec texte riche.
     */
    public FormsEditPage addCommentQuestion(String customCode, String commentText) {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Ajouter une question")).click();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Commentaire")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Code personnalisé")).click();
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Code personnalisé")).fill(customCode);
        // Saisir le texte dans l'iframe Rich Text Area
        FrameLocator richTextIframe = page.frameLocator("iframe[title=\"Rich Text Area\"]");
        richTextIframe.locator("html").click();
        richTextIframe.getByLabel("Zone de texte riche. Appuyez").fill(commentText);
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).click();
        return this;
    }

    /**
     * Decoche la case "Finale" d'une etape et valide.
     */
    public FormsEditPage uncheckFinalAndSave() {
        page.getByRole(AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName("Finale")).uncheck();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("OK")).click();
        return this;
    }

    /**
     * Configure la transition d'une etape via navigation directe.
     * Extrait l'id_step depuis le lien a.searchable contenant le nom de l'etape,
     * puis navigue vers createTransition.
     */
    public FormsEditPage configureStepTransition(String formId, String stepName) {
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageSteps.jsp?view=manageSteps&id_form=" + formId);
        page.waitForLoadState();
        Locator stepLink = page.locator("a.searchable",
            new Page.LocatorOptions().setHasText(stepName)).last();
        String href = stepLink.getAttribute("href");
        String stepId = href.split("id_step=")[1].split("&")[0];
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageTransitions.jsp?view=createTransition&id_step=" + stepId);
        page.waitForLoadState();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("OK")).click();
        return this;
    }

    /**
     * Publie le formulaire en modifiant les dates de disponibilite
     * sur la page modifyForm.
     */
    public FormsEditPage publishOnPortal(String formId, String startDate, String endDate) {
        page.navigate(baseUrl + "/jsp/admin/plugins/forms/ManageForms.jsp?view=modifyForm&id_form=" + formId);
        page.waitForLoadState();

        // Definir la date de debut via flatpickr (#availabilityStartDate)
        page.evaluate("(date) => {\n"
            + "  const input = document.querySelector('#availabilityStartDate');\n"
            + "  if (input && input._flatpickr) {\n"
            + "    input._flatpickr.setDate(date, true);\n"
            + "  }\n"
            + "}", startDate);

        // Definir la date de fin via flatpickr (#availabilityEndDate)
        page.evaluate("(date) => {\n"
            + "  const input = document.querySelector('#availabilityEndDate');\n"
            + "  if (input && input._flatpickr) {\n"
            + "    input._flatpickr.setDate(date, true);\n"
            + "  }\n"
            + "}", endDate);

        // Cliquer sur le bouton "Modifier le formulaire" par son name
        page.locator("button[name='action_modifyForm']").click();
        page.waitForLoadState();
        return this;
    }

    /**
     * Affiche les etapes du formulaire.
     */
    public FormsEditPage clickShowSteps() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(" Afficher les étapes")).click();
        return this;
    }

    /**
     * Clique sur un formulaire par son nom (dernier element correspondant).
     */
    public FormsEditPage clickFormByName(String formName) {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName(formName)).last().click();
        return this;
    }

    /**
     * Clique sur une etape par son nom (dernier element correspondant).
     */
    public FormsEditPage clickStepByName(String stepName) {
        page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName(stepName).setExact(true)).last().click();
        return this;
    }

}
