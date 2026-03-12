package fr.paris.lutece.e2e.pages;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.core.BrowserSession;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Page Object pour la gestion des formulaires Lutece.
 */
@Dependent
public class FormsPage extends BasePage {

    @Inject
    public FormsPage(BrowserSession browser) {
        super(browser);
    }

    // === Navigation ===

    public FormsPage navigateToList() {
        navigate("/jsp/admin/plugins/forms/ManageForms.jsp");
        return this;
    }

    public boolean isListDisplayed() {
        waitForLoad();
        return page().url().contains("ManageForms") ||
                page().locator("a:has-text('Ajouter')").isVisible();
    }

    /**
     * Extrait la liste des formulaires affichés sur la page.
     * @return Liste des noms de formulaires avec leurs informations
     */
    public java.util.List<FormListItem> getFormsList() {
        waitForLoad();
        java.util.List<FormListItem> forms = new java.util.ArrayList<>();

        // Les formulaires sont dans des .list-group-item
        var formItems = page().locator(".list-group-item").all();

        for (var item : formItems) {
            try {
                // Extraire le titre (dans .lead ou premier lien)
                String title = "";
                var leadEl = item.locator(".lead, h5, .fw-bold").first();
                if (leadEl.isVisible()) {
                    title = leadEl.textContent().trim();
                } else {
                    var linkEl = item.locator("a").first();
                    if (linkEl.isVisible()) {
                        title = linkEl.textContent().trim();
                    }
                }

                if (title.isEmpty()) continue;

                // Extraire l'ID si possible (depuis un lien href)
                int id = -1;
                var links = item.locator("a[href*='id_form=']").all();
                if (!links.isEmpty()) {
                    String href = links.get(0).getAttribute("href");
                    if (href != null && href.contains("id_form=")) {
                        String idStr = href.split("id_form=")[1].split("&")[0].split("#")[0];
                        try {
                            id = Integer.parseInt(idStr);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                // Extraire le statut (badge ou texte)
                String status = "";
                var badgeEl = item.locator(".badge, .status").first();
                if (badgeEl.isVisible()) {
                    status = badgeEl.textContent().trim();
                }

                forms.add(new FormListItem(id, title, status));
            } catch (Exception e) {
                // Ignorer les éléments mal formés
            }
        }

        return forms;
    }

    /**
     * Information sur un formulaire dans la liste.
     */
    public record FormListItem(int id, String title, String status) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (id > 0) {
                sb.append("[ID:").append(id).append("] ");
            }
            sb.append(title);
            if (status != null && !status.isEmpty()) {
                sb.append(" (").append(status).append(")");
            }
            return sb.toString();
        }
    }

    // === Creation ===

    public FormsPage clickAddForm() {
        page().locator("a:has-text('Ajouter')").first().click();
        waitForLoad();
        return this;
    }

    public FormsPage fillTitle(String title) {
        page().locator("input[name=\"title\"]").fill(title);
        return this;
    }

    public FormsPage setStartDate(String date) {
        page().evaluate("(date) => {\n"
                + "  const inputs = document.querySelectorAll('input.flatpickr-input');\n"
                + "  if (inputs[0] && inputs[0]._flatpickr) {\n"
                + "    inputs[0]._flatpickr.setDate(date, true);\n"
                + "  }\n"
                + "}", date);
        return this;
    }

    public FormsPage setEndDate(String date) {
        page().evaluate("(date) => {\n"
                + "  const inputs = document.querySelectorAll('input.flatpickr-input');\n"
                + "  if (inputs[1] && inputs[1]._flatpickr) {\n"
                + "    inputs[1]._flatpickr.setDate(date, true);\n"
                + "  }\n"
                + "}", date);
        return this;
    }

    public FormsPage selectWorkflow(String workflowName) {
        var select = page().locator("#idWorkflow");
        select.waitFor(new Locator.WaitForOptions().setTimeout(5000));

        // Vérifier si le workflow existe dans les options
        var options = select.locator("option").allTextContents();
        String matchedOption = null;

        for (String option : options) {
            if (option.equalsIgnoreCase(workflowName) || option.contains(workflowName)) {
                matchedOption = option;
                break;
            }
        }

        if (matchedOption != null) {
            select.selectOption(new SelectOption().setLabel(matchedOption));
        } else {
            // Log disponible options pour debug
            throw new RuntimeException("Workflow '" + workflowName + "' non trouvé. Options disponibles: " + options);
        }
        return this;
    }

    public FormsPage clickCreateForm() {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Créer le formulaire")).click();
        waitForLoad();
        return this;
    }

    /**
     * Vérifie si on est sur la page d'édition d'un formulaire.
     */
    public boolean isOnFormEditPage() {
        String url = page().url();
        return url.contains("ModifyForm") || url.contains("id_form=") || url.contains("ManageFormSteps");
    }

    /**
     * Navigue vers la page d'édition des étapes d'un formulaire par son ID.
     * Navigation directe par URL pour fiabilité.
     */
    public FormsPage navigateToFormEdit(int formId) {
        navigate("/jsp/admin/plugins/forms/ManageSteps.jsp?view=manageSteps&id_form=" + formId);
        waitForLoad();
        return this;
    }

    /**
     * Ouvre un formulaire depuis la liste par son nom pour modifier les étapes.
     * Trouve le formulaire par son nom et clique sur son lien "Modifier l'étape".
     */
    public FormsPage openFormByName(String formName) {
        navigateToList();
        waitForLoad();

        // Trouver la ligne du formulaire par son nom (dans .list-group-item ou dans une cellule de tableau)
        var formRow = page().locator(".list-group-item:has-text('" + formName + "')").first();

        if (formRow.isVisible()) {
            // Cliquer sur "Modifier l'étape" dans cette ligne spécifique
            formRow.getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName("Modifier l'étape")).click();
        } else {
            // Fallback: chercher dans un tableau
            var tableRow = page().locator("tr:has-text('" + formName + "')").first();
            if (tableRow.isVisible()) {
                tableRow.getByRole(AriaRole.LINK,
                        new Locator.GetByRoleOptions().setName("Modifier l'étape")).click();
            } else {
                // Dernier fallback: chercher le lien qui contient le nom du formulaire
                page().locator("a:has-text('" + formName + "')").first().click();
            }
        }
        waitForLoad();
        return this;
    }

    /**
     * Ouvre les paramètres d'un formulaire depuis la liste.
     * Basé sur le script Playwright: page.getByRole(AriaRole.LINK, "Modifier les paramètres du").first().click()
     */
    public FormsPage openFormParameters(String formName) {
        navigateToList();
        waitForLoad();

        // Essayer plusieurs sélecteurs pour trouver le lien
        var paramLink = page().locator("a[title*='Modifier les paramètres'], a:has-text('Modifier les paramètres')").first();

        if (!paramLink.isVisible()) {
            // Fallback: chercher par ARIA role
            paramLink = page().getByRole(AriaRole.LINK,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Modifier les paramètres du")).first();
        }

        paramLink.waitFor();
        paramLink.click();
        waitForLoad();
        return this;
    }

    // === Etapes (navigation directe par URL — comme les tests pipeline) ===

    /**
     * Ajoute une etape au formulaire via navigation directe (pas d'iframe).
     * Inspiré du POJO FormsEditPage.addStep() qui fonctionne dans les tests pipeline.
     */
    public FormsPage addStepDirect(int formId, String title, boolean isFinal) {
        navigate("/jsp/admin/plugins/forms/ManageSteps.jsp?view=createStep&id_form=" + formId);
        waitForLoad();

        // Verifier qu'on n'a pas ete redirige vers la page de login
        String url = page().url();
        if (url.contains("AdminLogin")) {
            throw new RuntimeException("Session expiree: redirige vers la page de login. URL: " + url);
        }

        // Verifier que le champ step-title existe (on est bien sur la page createStep)
        var stepTitleField = page().locator("#step-title");
        if (!stepTitleField.isVisible()) {
            throw new RuntimeException("Champ #step-title non trouve sur la page. URL actuelle: " + url
                    + " | Titre page: " + page().title());
        }

        stepTitleField.click();
        stepTitleField.fill(title);
        if (isFinal) {
            page().getByRole(AriaRole.CHECKBOX,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Finale")).check();
        }
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
        waitForLoad();
        return this;
    }

    /**
     * Extrait l'ID d'une etape par son nom depuis la page de gestion des etapes.
     * Inspiré du POJO FormsEditPage.navigateToStepQuestions().
     */
    public int extractStepId(int formId, String stepName) {
        navigate("/jsp/admin/plugins/forms/ManageSteps.jsp?view=manageSteps&id_form=" + formId);
        waitForLoad();
        Locator stepLink = page().locator("a.searchable",
                new com.microsoft.playwright.Page.LocatorOptions().setHasText(stepName)).last();
        String href = stepLink.getAttribute("href");
        return Integer.parseInt(href.split("id_step=")[1].split("&")[0]);
    }

    /**
     * Navigue vers la page de gestion des questions d'une etape (navigation directe).
     * Inspiré du POJO FormsEditPage.navigateToStepQuestions().
     */
    public FormsPage navigateToStepQuestions(int formId, String stepName) {
        int stepId = extractStepId(formId, stepName);
        navigate("/jsp/admin/plugins/forms/ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);
        waitForLoad();
        return this;
    }

    /**
     * Configure la transition d'une etape via navigation directe.
     * Inspiré du POJO FormsEditPage.configureStepTransition().
     */
    public FormsPage configureStepTransitionDirect(int formId, String stepName) {
        int stepId = extractStepId(formId, stepName);
        navigate("/jsp/admin/plugins/forms/ManageTransitions.jsp?view=createTransition&id_step=" + stepId);
        waitForLoad();
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
        waitForLoad();
        return this;
    }

    /**
     * Decoche "Finale" sur une etape via navigation directe.
     */
    public FormsPage uncheckStepFinaleDirect(int formId, String stepName) {
        int stepId = extractStepId(formId, stepName);
        navigate("/jsp/admin/plugins/forms/ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);
        waitForLoad();
        // Onglet "Parametres de l'etape"
        page().getByRole(AriaRole.TAB,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Paramètres de l'étape")).click();
        waitForLoad();
        page().getByRole(AriaRole.CHECKBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Finale")).uncheck();
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
        waitForLoad();
        return this;
    }

    // === Etapes (anciennes methodes via UI — conservees pour compatibilite) ===

    public FormsPage clickStepsTab() {
        waitForLoad();
        page().getByRole(AriaRole.TAB,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Etapes")).click();
        waitForLoad();
        return this;
    }

    public FormsPage openStepByName(String stepName) {
        waitForLoad();
        // Essayer plusieurs sélecteurs pour trouver l'étape
        var stepLink = page().locator(
            "a:has-text('" + stepName + "'), " +
            ".step-title:has-text('" + stepName + "'), " +
            "[data-step-title='" + stepName + "'], " +
            "tr:has-text('" + stepName + "') a, " +
            ".list-group-item:has-text('" + stepName + "')"
        ).first();

        if (stepLink.isVisible()) {
            stepLink.click();
            waitForLoad();
        } else {
            // Fallback: utiliser ARIA role sans exact match
            page().getByRole(AriaRole.LINK,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName(stepName))
                    .first().click();
            waitForLoad();
        }
        return this;
    }

    /**
     * Clique sur "Modifier l'étape" pour éditer la première étape.
     * Basé sur le script Playwright de l'utilisateur.
     */
    public FormsPage clickModifyStep() {
        page().getByRole(AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Modifier l'étape")).first().click();
        waitForLoad();
        return this;
    }

    /**
     * Clique sur "Modifier l'étape" pour une étape spécifique par son nom.
     * Trouve la ligne contenant le nom de l'étape et clique sur son lien "Modifier l'étape".
     */
    public FormsPage clickModifyStepByName(String stepName) {
        waitForLoad();

        // Chercher la ligne qui contient le nom de l'étape
        var stepRow = page().locator("[id^='step_']:has-text('" + stepName + "')").first();

        // Pattern pour matcher "Modifier l'étape" avec espaces potentiels
        var modifyPattern = java.util.regex.Pattern.compile("Modifier l'étape", java.util.regex.Pattern.CASE_INSENSITIVE);

        if (stepRow.isVisible()) {
            stepRow.getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName(modifyPattern)).click();
        } else {
            // Fallback: chercher par le texte de l'étape et cliquer sur le lien associé
            var stepLink = page().locator(".list-group-item:has-text('" + stepName + "'), tr:has-text('" + stepName + "')").first();
            if (stepLink.isVisible()) {
                stepLink.getByRole(AriaRole.LINK,
                        new Locator.GetByRoleOptions().setName(modifyPattern)).click();
            } else {
                // Dernier fallback: utiliser le premier lien "Modifier l'étape"
                page().getByRole(AriaRole.LINK,
                        new com.microsoft.playwright.Page.GetByRoleOptions().setName(modifyPattern)).first().click();
            }
        }
        waitForLoad();
        return this;
    }

    // === Questions ===

    /**
     * Clique sur l'onglet "Liste des Questions".
     * Basé sur le script Playwright de l'utilisateur.
     */
    public FormsPage clickQuestionsTab() {
        page().getByRole(AriaRole.TAB,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Liste des Questions")).click();
        waitForLoad();
        return this;
    }

    public FormsPage clickStepParametersTab() {
        page().getByRole(AriaRole.TAB,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Paramètres de l'étape")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type texte court à l'étape.
     * Basé sur le script Playwright de l'utilisateur:
     * - Cliquer sur "Ajouter une question"
     * - Cliquer sur "Zone de texte court"
     * - Remplir le titre
     * - Cliquer sur "Enregistrer"
     */
    public FormsPage addTextQuestion(String title) {
        // Cliquer sur "Ajouter une question"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        // Cliquer sur "Zone de texte court"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Zone de texte court")).click();
        waitForLoad();

        // Remplir le titre
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        // Enregistrer
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type nombre à l'étape.
     * Basé sur le script Playwright de l'utilisateur.
     */
    public FormsPage addNumberQuestion(String title) {
        // Cliquer sur "Ajouter une question"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        // Cliquer sur "Nombre"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Nombre")).click();
        waitForLoad();

        // Remplir le titre
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        // Enregistrer
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type date à l'étape.
     * Basé sur le script Playwright de l'utilisateur.
     */
    public FormsPage addDateQuestion(String title) {
        // Cliquer sur "Ajouter une question"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        // Cliquer sur "Date"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Date")).click();
        waitForLoad();

        // Remplir le titre
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        // Enregistrer
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    public FormsPage addCommentQuestion(String customCode, String commentText) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Commentaire")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Code personnalisé")).fill(customCode);
        FrameLocator richTextIframe = page().frameLocator("iframe[title=\"Rich Text Area\"]");
        richTextIframe.locator("html").click();
        richTextIframe.getByLabel("Zone de texte riche. Appuyez").fill(commentText);
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type liste déroulante à l'étape.
     */
    public FormsPage addDropdownQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Liste déroulante")).click();
        waitForLoad();

        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type fichier à l'étape.
     * Le type Fichier a deux champs obligatoires: file_max_size et max_files.
     */
    public FormsPage addFileQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Fichier")).click();
        waitForLoad();

        // Remplir le titre
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        // Remplir les champs obligatoires: file_max_size et max_files
        page().locator("#file_max_size").fill("10485760"); // 10 Mo
        page().locator("#max_files").fill("1");

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type attribut Mylutece à l'étape.
     */
    public FormsPage addMylutecAttributeQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        // Cliquer sur "Attribut de l'utilisateur MyLutece" pour sélectionner le type
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Attribut de l'utilisateur MyLutece")).click();
        waitForLoad();

        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type géolocalisation à l'étape.
     */
    public FormsPage addGeolocationQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Géolocalisation")).click();
        waitForLoad();

        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type liste triable à l'étape.
     */
    public FormsPage addSortableListQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Liste triable")).click();
        waitForLoad();

        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type zone de texte long à l'étape.
     * Le type Zone de texte long a un champ obligatoire: height (hauteur).
     */
    public FormsPage addTextareaQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Zone de texte long")).click();
        waitForLoad();

        // Remplir le titre
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        // Remplir le champ obligatoire: height (hauteur de la zone de texte)
        page().locator("#height").fill("5"); // 5 lignes par défaut

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type bouton radio à l'étape.
     */
    public FormsPage addRadioButtonQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Bouton radio")).click();
        waitForLoad();

        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type case à cocher à l'étape.
     */
    public FormsPage addCheckboxQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        // Le bouton peut s'appeler "Case à cocher", "Cases à cocher" ou "Checkbox" selon la version
        var checkboxButton = page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Case à cocher"));
        if (!checkboxButton.isVisible()) {
            checkboxButton = page().getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Cases à cocher"));
        }
        if (!checkboxButton.isVisible()) {
            // Fallback: chercher par pattern regex insensible à la casse
            checkboxButton = page().getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions()
                            .setName(java.util.regex.Pattern.compile("case.*cocher", java.util.regex.Pattern.CASE_INSENSITIVE)));
        }
        if (!checkboxButton.isVisible()) {
            // Dernier fallback: chercher un bouton contenant le texte
            checkboxButton = page().locator("button:has-text('cocher'), button:has-text('Checkbox')").first();
        }
        checkboxButton.click();
        waitForLoad();

        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type numérotation à l'étape.
     */
    public FormsPage addNumberingQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        // Cliquer sur "Numérotation" pour sélectionner le type
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Numérotation")).click();
        waitForLoad();

        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    /**
     * Ajoute une question de type image à l'étape.
     * Le type Image a deux champs obligatoires: file_max_size et max_files.
     */
    public FormsPage addImageQuestion(String title) {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une question")).click();
        waitForLoad();

        // Le bouton peut s'appeler "Image" ou "Téléchargement d'image" selon la version
        var imageButton = page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Téléchargement d'image"));
        if (!imageButton.isVisible()) {
            imageButton = page().getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Image"));
        }
        imageButton.click();
        waitForLoad();

        // Remplir le titre
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).click();
        page().getByRole(AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Titre *")).fill(title);

        // Remplir les champs obligatoires: file_max_size et max_files
        var maxSizeField = page().locator("#file_max_size");
        if (maxSizeField.isVisible()) {
            maxSizeField.fill("10485760"); // 10 Mo
        }
        var maxFilesField = page().locator("#max_files");
        if (maxFilesField.isVisible()) {
            maxFilesField.fill("1");
        }

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Enregistrer")).click();
        waitForLoad();
        return this;
    }

    // === Transitions ===

    /**
     * Clique sur "Modifier l'étape" pour une étape spécifique par son index (1-based).
     * Basé sur le script Playwright: page.locator("#step_8").getByRole(...)
     */
    public FormsPage clickModifyStepByIndex(int stepIndex) {
        waitForLoad();

        // Trouver tous les liens "Modifier l'étape" et cliquer sur celui demandé
        var modifyLinks = page().getByRole(AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Modifier l'étape"));

        // Attendre que les liens soient visibles
        modifyLinks.first().waitFor();

        // Cliquer sur le lien à l'index demandé (1-based, donc nth(0) pour le premier)
        int count = modifyLinks.count();
        if (count >= stepIndex) {
            modifyLinks.nth(stepIndex - 1).click();
        } else {
            modifyLinks.first().click();
        }
        waitForLoad();
        return this;
    }

    /**
     * Modifie une étape existante pour décocher "Finale" par index.
     */
    public FormsPage uncheckStepFinale(int stepIndex) {
        waitForLoad();

        // Cliquer sur l'onglet Etapes
        clickStepsTab();

        // Cliquer sur "Modifier l'étape" pour l'étape demandée
        clickModifyStepByIndex(stepIndex);

        // Décocher "Finale"
        page().getByRole(AriaRole.CHECKBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Finale")).uncheck();

        // Cliquer sur "OK"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
        waitForLoad();

        return this;
    }

    /**
     * Modifie une étape existante pour décocher "Finale" par nom.
     */
    public FormsPage uncheckStepFinaleByName(String stepName) {
        waitForLoad();

        // Cliquer sur l'onglet Etapes
        clickStepsTab();

        // Cliquer sur "Modifier l'étape" pour l'étape spécifique
        clickModifyStepByName(stepName);

        // Décocher "Finale"
        page().getByRole(AriaRole.CHECKBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Finale")).uncheck();

        // Cliquer sur "OK"
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
        waitForLoad();

        return this;
    }

    /**
     * Ajoute une liaison entre l'étape courante et l'étape suivante.
     * Basé sur le script Playwright de l'utilisateur.
     * IMPORTANT: Doit être appelé après avoir ouvert la modification d'une étape.
     */
    public FormsPage addStepTransition() {
        waitForLoad();

        // D'abord, s'assurer qu'on est sur l'onglet "Paramètres de l'étape"
        // car les transitions sont dans cet onglet, pas dans "Liste des Questions"
        var stepParamsTab = page().getByRole(AriaRole.TAB,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Paramètres de l'étape"));
        if (stepParamsTab.isVisible()) {
            stepParamsTab.click();
            waitForLoad();
        }

        // Attendre que le lien "Ajouter une liaison" soit visible
        var addTransitionLink = page().getByRole(AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Ajouter une liaison"));

        // Si le lien n'est pas visible, essayer un sélecteur alternatif
        if (!addTransitionLink.isVisible()) {
            addTransitionLink = page().locator("a:has-text('Ajouter une liaison'), a[title*='liaison']").first();
        }

        addTransitionLink.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        addTransitionLink.click();
        waitForLoad();

        // Essayer plusieurs sélecteurs pour l'iframe ou le modal de transition
        FrameLocator offcanvasIframe = null;
        String[] iframeSelectors = {
            "iframe[title=\"Offcanvas\"]",
            "iframe[title*=\"Offcanvas\"]",
            "iframe[title*=\"liaison\"]",
            "iframe[title*=\"transition\"]",
            "iframe.offcanvas-iframe",
            ".offcanvas iframe",
            ".modal iframe",
            "iframe[src*='transition']",
            "iframe[src*='Transition']",
            "iframe[src*='ManageControl']"
        };

        for (String selector : iframeSelectors) {
            var iframe = page().locator(selector);
            if (iframe.count() > 0 && iframe.first().isVisible()) {
                offcanvasIframe = page().frameLocator(selector);
                break;
            }
        }

        if (offcanvasIframe != null) {
            // Attendre et cliquer sur OK dans l'iframe
            var okButton = offcanvasIframe.getByRole(AriaRole.BUTTON,
                    new FrameLocator.GetByRoleOptions().setName("OK"));
            okButton.waitFor(new Locator.WaitForOptions().setTimeout(10000));
            okButton.click();
        } else {
            // Pas d'iframe trouvé - peut-être un modal sans iframe ou action directe
            // Essayer de cliquer sur OK directement sur la page
            var okButton = page().getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK"));
            if (okButton.isVisible()) {
                okButton.click();
            } else {
                // Dernier fallback: chercher un bouton de confirmation
                var confirmButton = page().locator("button:has-text('OK'), button:has-text('Confirmer'), button:has-text('Valider')").first();
                if (confirmButton.isVisible()) {
                    confirmButton.click();
                }
            }
        }
        waitForLoad();
        return this;
    }

    public FormsPage configureStepTransition() {
        return addStepTransition();
    }

    public FormsPage uncheckFinalAndSave() {
        page().getByRole(AriaRole.CHECKBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Finale")).uncheck();
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
        waitForLoad();
        return this;
    }

    // === Publication ===

    public FormsPage publishOnPortal(String formName, String startDate) {
        // Navigate to the forms list
        navigateToList();
        waitForLoad();

        // Find the form card by name and open its Actions dropdown
        // In Lutece 8, forms are .card elements with a .form-actions area containing a .dropdown-toggle button
        page().locator(".card")
                .filter(new Locator.FilterOptions().setHasText(formName))
                .locator(".form-actions .btn.dropdown-toggle").click();

        // Click "Editer la publication du formulaire" in the dropdown
        page().getByRole(AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Editer la publication du")).click();
        waitForLoad();

        // Set start date via flatpickr on #availabilityStartDate
        page().evaluate("(date) => {\n"
                + "  const input = document.querySelector('#availabilityStartDate');\n"
                + "  if (input && input._flatpickr) {\n"
                + "    input._flatpickr.setDate(date, true);\n"
                + "  }\n"
                + "}", startDate);

        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK")).click();
        waitForLoad();
        return this;
    }

    // === Navigation dans l'arborescence ===

    public FormsPage clickShowSteps() {
        page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(" Afficher les étapes")).click();
        return this;
    }

    public FormsPage clickFormByName(String formName) {
        page().getByRole(AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(formName)).last().click();
        return this;
    }

    public FormsPage clickStepByName(String stepName) {
        page().getByRole(AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(stepName).setExact(true))
                .last().click();
        return this;
    }

    // === Utilitaires ===

    public int extractFormIdFromUrl() {
        String url = page().url();
        if (url.contains("id_form=")) {
            return Integer.parseInt(url.split("id_form=")[1].split("&")[0].split("#")[0]);
        } else if (url.contains("id=")) {
            return Integer.parseInt(url.split("id=")[1].split("&")[0].split("#")[0]);
        }
        return -1;
    }

    /**
     * Extrait l'ID d'un formulaire depuis la liste en cherchant par titre.
     * Utilise quand l'URL apres creation ne contient pas id_form (redirection vers la liste).
     */
    // === Front Office ===

    /**
     * Navigue vers la liste des formulaires FO et clique sur le formulaire.
     */
    public FormsPage navigateToFrontOffice(String formTitle) {
        navigate("/jsp/site/Portal.jsp?page=forms");
        waitForLoad();
        page().waitForTimeout(2000);

        var formLink = page().locator("a:has-text('" + formTitle + "')");
        if (formLink.count() > 0) {
            formLink.first().click();
            waitForLoad();
            page().waitForTimeout(1000);
        }

        // Fermer l'offcanvas s'il est present
        var backdrop = page().locator(".offcanvas-backdrop");
        if (backdrop.count() > 0) {
            page().keyboard().press("Escape");
            backdrop.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
        }

        return this;
    }

    /**
     * Remplit un champ texte FO par son label.
     */
    public FormsPage fillTextFieldFO(String label, String value) {
        page().waitForLoadState();
        var byRole = page().getByRole(AriaRole.TEXTBOX,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName(label));
        if (byRole.count() > 0) {
            byRole.first().click();
            byRole.first().fill(value);
            return this;
        }
        var byLabel = page().getByLabel(label);
        if (byLabel.count() > 0) {
            byLabel.first().click();
            byLabel.first().fill(value);
            return this;
        }
        // Fallback: premier champ texte visible
        var allTextInputs = page().locator("input[type='text']");
        for (int i = 0; i < allTextInputs.count(); i++) {
            var input = allTextInputs.nth(i);
            if (input.isVisible()) {
                input.click();
                input.fill(value);
                return this;
            }
        }
        throw new RuntimeException("Aucun champ texte trouve pour le label: " + label);
    }

    /**
     * Remplit un champ nombre FO par son label.
     */
    public FormsPage fillNumberFieldFO(String label, String value) {
        page().waitForLoadState();
        var byRole = page().getByRole(AriaRole.SPINBUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName(label));
        if (byRole.count() > 0) {
            byRole.first().click();
            byRole.first().fill(value);
            return this;
        }
        var byLabel = page().getByLabel(label);
        if (byLabel.count() > 0) {
            byLabel.first().click();
            byLabel.first().fill(value);
            return this;
        }
        var allNumberInputs = page().locator("input[type='number']");
        for (int i = 0; i < allNumberInputs.count(); i++) {
            var input = allNumberInputs.nth(i);
            if (input.isVisible()) {
                input.click();
                input.fill(value);
                return this;
            }
        }
        throw new RuntimeException("Aucun champ nombre trouve pour le label: " + label);
    }

    /**
     * Remplit un champ date FO (flatpickr) via JavaScript.
     */
    public FormsPage fillDateFieldFO(String value) {
        var input = page().locator("input.flatpickr-input");
        if (input.count() > 0) {
            input.first().evaluate(
                "(el, date) => { if (el._flatpickr) { el._flatpickr.setDate(date, true); } else { el.value = date; el.dispatchEvent(new Event('change')); } }",
                value);
        }
        return this;
    }

    /**
     * Clique sur "Etape suivante" en FO.
     */
    public FormsPage clickNextStepFO() {
        page().getByRole(AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Etape suivante")).click();
        return this;
    }

    /**
     * Clique sur "Voir le recapitulatif" en FO.
     */
    public FormsPage clickViewSummaryFO() {
        page().getByRole(AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Voir le récapitulatif")).click();
        return this;
    }

    /**
     * Clique sur "Valider le recapitulatif" en FO.
     */
    public FormsPage clickValidateSummaryFO() {
        page().getByRole(AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Valider le récapitulatif")).click();
        page().waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        return this;
    }

    public int extractFormIdFromList(String formTitle) {
        // S'assurer qu'on est sur la page de liste
        if (!page().url().contains("ManageForms")) {
            navigateToList();
        }
        waitForLoad();

        // Chercher le lien du formulaire par son titre (href contient id_form=X)
        var formLinks = page().locator("a[href*='id_form=']").all();
        for (var link : formLinks) {
            String text = link.textContent().trim();
            if (text.contains(formTitle)) {
                String href = link.getAttribute("href");
                if (href != null && href.contains("id_form=")) {
                    String idStr = href.split("id_form=")[1].split("&")[0].split("#")[0];
                    return Integer.parseInt(idStr);
                }
            }
        }
        return -1;
    }
}
