package fr.paris.lutece.e2e.actions;

import fr.paris.lutece.e2e.core.ActionResult;
import fr.paris.lutece.e2e.core.BrowserSession;
import fr.paris.lutece.e2e.pages.FormsPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Actions de gestion des formulaires.
 * Utilise la navigation directe par URL (comme les tests pipeline).
 */
@ApplicationScoped
public class FormsActions {

    private static final Logger LOG = LogManager.getLogger(FormsActions.class);

    @Inject
    BrowserSession browser;

    @Inject
    FormsPage formsPage;

    public record FormInfo(int id, String title, String workflowName) {}
    public record QuestionInfo(String title, String type) {}

    // ID du formulaire en cours d'edition
    private int currentFormId = -1;

    /**
     * Cree un nouveau formulaire.
     */
    public ActionResult<FormInfo> createForm(String title, String workflowName, String startDate, String endDate) {
        LOG.info("Creation du formulaire: {} avec workflow: {}", title, workflowName);

        try {
            formsPage.navigateToList()
                    .clickAddForm()
                    .fillTitle(title)
                    .setStartDate(startDate)
                    .setEndDate(endDate);

            if (workflowName != null && !workflowName.isEmpty()) {
                formsPage.selectWorkflow(workflowName);
            }

            formsPage.clickCreateForm();

            int formId = formsPage.extractFormIdFromUrl();
            if (formId <= 0) {
                // Apres creation, Lutece redirige vers la liste (ManageForms) sans id_form dans l'URL.
                // Fallback: chercher le formulaire par son titre dans la liste.
                LOG.info("id_form absent de l'URL ({}), recherche par titre dans la liste...", getCurrentUrl());
                formId = formsPage.extractFormIdFromList(title);
            }
            if (formId > 0) {
                currentFormId = formId;
                LOG.info("Formulaire cree avec ID: {}", formId);
                return ActionResult.success(
                        new FormInfo(formId, title, workflowName),
                        "Formulaire '" + title + "' cree avec succes (ID: " + formId + ")",
                        safeScreenshot("form-created-" + formId));
            }

            currentFormId = -1;
            LOG.warn("Formulaire cree mais ID non trouve (URL: {}, titre: {})", getCurrentUrl(), title);
            return ActionResult.failure("Formulaire cree mais ID non recupere - verifiez manuellement",
                    safeScreenshot("form-created-no-id"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la creation du formulaire", e);
            currentFormId = -1;
            return ActionResult.failure("Erreur creation formulaire: " + e.getMessage(),
                    safeScreenshot("form-creation-error"));
        }
    }

    public ActionResult<FormInfo> createForm(String title, String workflowName) {
        return createForm(title, workflowName, "today", "2033-12-31");
    }

    /**
     * Ajoute une etape au formulaire (navigation directe par URL).
     */
    public ActionResult<String> addStep(String stepTitle, boolean isFinal) {
        LOG.info("Ajout de l'etape: {} (finale: {}) au formulaire ID={}", stepTitle, isFinal, currentFormId);

        try {
            if (currentFormId <= 0) {
                return ActionResult.failure("Aucun formulaire en cours d'edition (currentFormId=" + currentFormId + ").");
            }

            formsPage.addStepDirect(currentFormId, stepTitle, isFinal);
            LOG.info("Etape '{}' ajoutee au formulaire ID={}", stepTitle, currentFormId);
            return ActionResult.success(stepTitle,
                    "Etape '" + stepTitle + "' ajoutee" + (isFinal ? " (finale)" : ""),
                    safeScreenshot("step-added"));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de l'etape '{}' au formulaire ID={}", stepTitle, currentFormId, e);
            return ActionResult.failure(
                    "Erreur ajout etape '" + stepTitle + "' (formId=" + currentFormId + "): " + e.getMessage(),
                    safeScreenshot("step-add-error"));
        }
    }

    /**
     * Ajoute une etape initiale (non finale).
     */
    public ActionResult<String> addInitialStep(String stepTitle) {
        return addStep(stepTitle, false);
    }

    /**
     * Ajoute une question a une etape (navigation directe par URL).
     * @param stepName Nom de l'etape
     * @param questionTitle Titre de la question
     * @param questionType Type de question (TEXT, NUMBER, DATE, etc.)
     */
    private ActionResult<QuestionInfo> addQuestion(String stepName, String questionTitle, String questionType,
                                                    QuestionAdder adder) {
        LOG.info("Ajout question {} '{}' a l'etape '{}'", questionType, questionTitle, stepName);

        try {
            if (currentFormId <= 0) {
                return ActionResult.failure("Aucun formulaire en cours d'edition. Creez d'abord un formulaire.");
            }

            // Navigation directe vers la page des questions de l'etape
            formsPage.navigateToStepQuestions(currentFormId, stepName);

            // Ajouter la question
            adder.add(questionTitle);

            LOG.info("Question {} '{}' ajoutee a l'etape '{}'", questionType, questionTitle, stepName);
            return ActionResult.success(
                    new QuestionInfo(questionTitle, questionType),
                    "Question " + questionType + " '" + questionTitle + "' ajoutee a l'etape '" + stepName + "'",
                    browser.screenshot("question-added"));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de la question {} '{}' a l'etape '{}'",
                    questionType, questionTitle, stepName, e);
            return ActionResult.failure("Erreur ajout question " + questionType + " '" + questionTitle
                    + "' sur '" + stepName + "': " + e.getMessage(),
                    safeScreenshot("question-add-error"));
        }
    }

    @FunctionalInterface
    private interface QuestionAdder {
        void add(String title);
    }

    public ActionResult<QuestionInfo> addTextQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "TEXT", formsPage::addTextQuestion);
    }

    public ActionResult<QuestionInfo> addNumberQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "NUMBER", formsPage::addNumberQuestion);
    }

    public ActionResult<QuestionInfo> addDateQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "DATE", formsPage::addDateQuestion);
    }

    public ActionResult<QuestionInfo> addDropdownQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "DROPDOWN", formsPage::addDropdownQuestion);
    }

    public ActionResult<QuestionInfo> addFileQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "FILE", formsPage::addFileQuestion);
    }

    public ActionResult<QuestionInfo> addMylutecAttributeQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "MYLUTECE_ATTRIBUTE", formsPage::addMylutecAttributeQuestion);
    }

    public ActionResult<QuestionInfo> addGeolocationQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "GEOLOCATION", formsPage::addGeolocationQuestion);
    }

    public ActionResult<QuestionInfo> addSortableListQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "SORTABLE_LIST", formsPage::addSortableListQuestion);
    }

    public ActionResult<QuestionInfo> addTextareaQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "TEXTAREA", formsPage::addTextareaQuestion);
    }

    public ActionResult<QuestionInfo> addRadioButtonQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "RADIO", formsPage::addRadioButtonQuestion);
    }

    public ActionResult<QuestionInfo> addCheckboxQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "CHECKBOX", formsPage::addCheckboxQuestion);
    }

    public ActionResult<QuestionInfo> addNumberingQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "NUMBERING", formsPage::addNumberingQuestion);
    }

    public ActionResult<QuestionInfo> addImageQuestion(String stepName, String questionTitle) {
        return addQuestion(stepName, questionTitle, "IMAGE", formsPage::addImageQuestion);
    }

    /**
     * Decoche Finale sur une etape (navigation directe par URL).
     */
    public ActionResult<Void> uncheckStepFinaleByName(String stepName) {
        LOG.info("Decochage de 'Finale' pour l'etape '{}'", stepName);

        try {
            if (currentFormId <= 0) {
                return ActionResult.failure("Aucun formulaire en cours d'edition.");
            }

            formsPage.uncheckStepFinaleDirect(currentFormId, stepName);

            LOG.info("Etape '{}' modifiee (Finale decochee)", stepName);
            return ActionResult.success(null,
                    "Etape '" + stepName + "' modifiee (Finale decochee)",
                    browser.screenshot("step-finale-unchecked"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la modification de l'etape", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("step-modify-error"));
        }
    }

    /**
     * Configure la transition depuis une etape (navigation directe par URL).
     */
    public ActionResult<Void> configureStepTransition(String fromStep) {
        LOG.info("Configuration de la transition depuis l'etape '{}'", fromStep);

        try {
            if (currentFormId <= 0) {
                return ActionResult.failure("Aucun formulaire en cours d'edition.");
            }

            formsPage.configureStepTransitionDirect(currentFormId, fromStep);

            LOG.info("Transition configuree depuis '{}'", fromStep);
            return ActionResult.success(null,
                    "Transition configuree depuis '" + fromStep + "'",
                    browser.screenshot("transition-configured"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la configuration de la transition", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("transition-error"));
        }
    }

    /**
     * Publie le formulaire sur le portail.
     */
    public ActionResult<Void> publishForm(String formTitle, String startDate) {
        LOG.info("Publication du formulaire '{}' a partir du {}", formTitle, startDate);

        try {
            formsPage.publishOnPortal(formTitle, startDate);

            return ActionResult.success(null,
                    "Formulaire '" + formTitle + "' publie sur le portail",
                    browser.screenshot("form-published"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la publication", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("publish-error"));
        }
    }

    /**
     * Navigue vers la liste des formulaires.
     */
    public ActionResult<String> navigateToList() {
        try {
            formsPage.navigateToList();
            if (formsPage.isListDisplayed()) {
                var forms = formsPage.getFormsList();
                if (forms.isEmpty()) {
                    return ActionResult.success("Aucun formulaire trouve", "Aucun formulaire dans la liste");
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Liste des formulaires (").append(forms.size()).append(") :\n");
                for (int i = 0; i < forms.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(forms.get(i).toString()).append("\n");
                }
                return ActionResult.success(sb.toString(), sb.toString());
            }
            return ActionResult.failure("Page liste non affichee",
                    browser.screenshot("forms-list-error"));
        } catch (Exception e) {
            LOG.error("Erreur lors de la navigation vers la liste", e);
            return ActionResult.failure("Erreur: " + e.getMessage());
        }
    }

    /**
     * Soumet un formulaire en front office.
     */
    public ActionResult<Void> submitFormFrontOffice(String formTitle, String textLabel, String textValue,
                                                     String numberLabel, String numberValue, String dateValue) {
        LOG.info("Soumission FO du formulaire '{}'", formTitle);

        try {
            formsPage.navigateToFrontOffice(formTitle);

            // Verifier que les champs du formulaire sont presents
            boolean hasFormFields = browser.getPage().locator("input[type='text']").count() > 0 ||
                                    browser.getPage().locator("input[type='number']").count() > 0 ||
                                    browser.getPage().locator("textarea").count() > 0;

            if (!hasFormFields) {
                return ActionResult.failure("Les champs du formulaire ne sont pas disponibles en front office. URL: "
                        + getCurrentUrl(), safeScreenshot("fo-form-not-found"));
            }

            formsPage.fillTextFieldFO(textLabel, textValue);
            formsPage.fillNumberFieldFO(numberLabel, numberValue);
            formsPage.fillDateFieldFO(dateValue);
            formsPage.clickNextStepFO();
            formsPage.clickViewSummaryFO();
            formsPage.clickValidateSummaryFO();

            LOG.info("Formulaire '{}' soumis en front office", formTitle);
            return ActionResult.success(null,
                    "Formulaire '" + formTitle + "' soumis avec succes en front office",
                    safeScreenshot("fo-submission-success"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la soumission FO du formulaire '{}'", formTitle, e);
            return ActionResult.failure("Erreur soumission FO: " + e.getMessage(),
                    safeScreenshot("fo-submission-error"));
        }
    }

    /**
     * Retourne l'URL courante du navigateur (pour debug).
     */
    public String getCurrentUrl() {
        try {
            return browser.getCurrentUrl();
        } catch (Exception e) {
            return "URL inconnue: " + e.getMessage();
        }
    }

    private java.nio.file.Path safeScreenshot(String name) {
        try {
            return browser.screenshot(name);
        } catch (Exception e) {
            LOG.warn("Impossible de prendre screenshot '{}': {}", name, e.getMessage());
            return null;
        }
    }
}
