package fr.paris.lutece.e2e.actions;

import fr.paris.lutece.e2e.core.ActionResult;
import fr.paris.lutece.e2e.core.BrowserManager;
import fr.paris.lutece.e2e.pages.FormsPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Actions de gestion des formulaires.
 * Utilisable par les tests JUnit et les tools LangChain4j.
 */
@ApplicationScoped
public class FormsActions {

    private static final Logger LOG = LogManager.getLogger(FormsActions.class);

    @Inject
    BrowserManager browser;

    @Inject
    FormsPage formsPage;

    /**
     * Informations sur un formulaire cree.
     */
    public record FormInfo(int id, String title, String workflowName) {}

    // Stocke l'ID et nom du formulaire en cours d'édition
    private int currentFormId = -1;
    private String currentFormName = null;

    /**
     * Informations sur une question.
     */
    public record QuestionInfo(String title, String type) {}

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

            String currentUrl = browser.getCurrentUrl();
            LOG.info("URL apres creation: {}", currentUrl);
            int formId = formsPage.extractFormIdFromUrl();
            if (formId > 0) {
                currentFormId = formId;
                currentFormName = title;
                LOG.info("Formulaire cree avec ID: {}", formId);
                return ActionResult.success(
                        new FormInfo(formId, title, workflowName),
                        "Formulaire '" + title + "' cree avec succes (ID: " + formId + ")",
                        browser.screenshot("form-created-" + formId));
            }

            // Même si l'ID n'est pas dans l'URL, on garde le nom pour retrouver le formulaire
            currentFormName = title;
            LOG.warn("Formulaire cree mais ID non trouve dans l'URL: {}", currentUrl);
            return ActionResult.success(
                    new FormInfo(-1, title, workflowName),
                    "Formulaire '" + title + "' cree (ID non recupere). URL: " + currentUrl,
                    browser.screenshot("form-created-no-id"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la creation du formulaire", e);
            // IMPORTANT: Reset le contexte en cas d'erreur pour éviter d'agir sur un autre formulaire
            currentFormId = -1;
            currentFormName = null;
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("form-creation-error"));
        }
    }

    /**
     * Cree un formulaire simple (dates par defaut).
     */
    public ActionResult<FormInfo> createForm(String title, String workflowName) {
        return createForm(title, workflowName, "today", "2033-12-31");
    }

    /**
     * Ajoute une etape au formulaire.
     * TOUJOURS utilise openFormByName pour naviguer vers le formulaire le plus récent.
     */
    public ActionResult<String> addStep(String stepTitle, boolean isFinal) {
        LOG.info("Ajout de l'etape: {} (finale: {})", stepTitle, isFinal);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer via openFormByName (premier formulaire de la liste = le plus récent)
            LOG.info("Ouverture du formulaire '{}' depuis la liste", currentFormName);
            formsPage.openFormByName(currentFormName);

            LOG.info("URL après navigation: {}", browser.getCurrentUrl());

            formsPage.clickStepsTab()
                    .addStep(stepTitle, isFinal);

            LOG.info("Etape '{}' ajoutee", stepTitle);
            return ActionResult.success(stepTitle,
                    "Etape '" + stepTitle + "' ajoutee" + (isFinal ? " (finale)" : ""),
                    browser.screenshot("step-added-" + stepTitle.replace(" ", "-")));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de l'etape", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("step-add-error"));
        }
    }

    /**
     * Ajoute une etape initiale au formulaire.
     * TOUJOURS utilise openFormByName pour naviguer vers le formulaire le plus récent.
     */
    public ActionResult<String> addInitialStep(String stepTitle) {
        LOG.info("Ajout de l'etape initiale: {}", stepTitle);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer via openFormByName
            LOG.info("Ouverture du formulaire '{}' depuis la liste", currentFormName);
            formsPage.openFormByName(currentFormName);

            // Cliquer sur l'onglet Etapes et ajouter l'étape initiale
            formsPage.clickStepsTab()
                    .addInitialStep(stepTitle);

            LOG.info("Etape initiale '{}' ajoutee", stepTitle);
            return ActionResult.success(stepTitle,
                    "Etape initiale '" + stepTitle + "' ajoutee",
                    browser.screenshot("step-initial-added"));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de l'etape initiale: {}", e.getMessage(), e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("step-add-error"));
        }
    }

    // Mémorise si on est déjà dans l'onglet Questions d'une étape
    private String currentStepName = null;

    /**
     * Navigue vers l'onglet Questions d'une étape.
     * Appelée une seule fois avant d'ajouter plusieurs questions.
     */
    public ActionResult<Void> navigateToStepQuestions(String stepName) {
        LOG.info("Navigation vers l'onglet Questions de l'étape '{}'", stepName);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            formsPage.openFormByName(currentFormName);
            formsPage.clickStepsTab();
            formsPage.clickModifyStepByName(stepName);
            formsPage.clickQuestionsTab();

            LOG.info("Navigation vers l'onglet Questions de '{}' réussie", stepName);
            return ActionResult.success(null, "Navigation vers Questions de '" + stepName + "' réussie");

        } catch (Exception e) {
            LOG.error("Erreur lors de la navigation vers Questions", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("navigation-error"));
        }
    }

    /**
     * Ajoute une question texte court a l'etape.
     * Navigue TOUJOURS vers le formulaire/étape car la page peut avoir changé après la dernière action.
     */
    public ActionResult<QuestionInfo> addTextQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question texte '{}' a l'etape '{}'", questionTitle, stepName);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer car la page peut avoir changé après la dernière action
            formsPage.openFormByName(currentFormName);
            formsPage.clickStepsTab();
            formsPage.clickModifyStepByName(stepName);
            formsPage.clickQuestionsTab();

            // Ajouter la question
            formsPage.addTextQuestion(questionTitle);
            LOG.info("Question texte '{}' ajoutée à l'étape '{}'", questionTitle, stepName);

            return ActionResult.success(
                    new QuestionInfo(questionTitle, "TEXT"),
                    "Question texte '" + questionTitle + "' ajoutee a l'etape '" + stepName + "'",
                    browser.screenshot("question-text-added"));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de la question texte", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("question-add-error"));
        }
    }

    /**
     * Ajoute une question nombre a l'etape.
     * Navigue TOUJOURS vers le formulaire/étape car la page peut avoir changé après la dernière action.
     */
    public ActionResult<QuestionInfo> addNumberQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question nombre '{}' a l'etape '{}'", questionTitle, stepName);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer car la page peut avoir changé après la dernière action
            formsPage.openFormByName(currentFormName);
            formsPage.clickStepsTab();
            formsPage.clickModifyStepByName(stepName);
            formsPage.clickQuestionsTab();

            // Ajouter la question nombre
            formsPage.addNumberQuestion(questionTitle);
            LOG.info("Question nombre '{}' ajoutée à l'étape '{}'", questionTitle, stepName);

            return ActionResult.success(
                    new QuestionInfo(questionTitle, "NUMBER"),
                    "Question nombre '" + questionTitle + "' ajoutee a l'etape '" + stepName + "'",
                    browser.screenshot("question-number-added"));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de la question nombre", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("question-add-error"));
        }
    }

    /**
     * Ajoute une question date a l'etape.
     * Navigue TOUJOURS vers le formulaire/étape car la page peut avoir changé après la dernière action.
     */
    public ActionResult<QuestionInfo> addDateQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question date '{}' a l'etape '{}'", questionTitle, stepName);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer car la page peut avoir changé après la dernière action
            formsPage.openFormByName(currentFormName);
            formsPage.clickStepsTab();
            formsPage.clickModifyStepByName(stepName);
            formsPage.clickQuestionsTab();

            // Ajouter la question date
            formsPage.addDateQuestion(questionTitle);
            LOG.info("Question date '{}' ajoutée à l'étape '{}'", questionTitle, stepName);

            return ActionResult.success(
                    new QuestionInfo(questionTitle, "DATE"),
                    "Question date '" + questionTitle + "' ajoutee a l'etape '" + stepName + "'",
                    browser.screenshot("question-date-added"));

        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout de la question date", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("question-add-error"));
        }
    }

    /**
     * Modifie une etape existante pour decocher "Finale" par index.
     * Cela permet d'ajouter une transition depuis cette etape.
     * @deprecated Utiliser uncheckStepFinaleByName() à la place
     */
    public ActionResult<Void> uncheckStepFinale(int stepIndex) {
        LOG.info("Décochage de 'Finale' pour l'étape {}", stepIndex);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer via openFormByName
            LOG.info("Ouverture du formulaire '{}' depuis la liste", currentFormName);
            formsPage.openFormByName(currentFormName);

            // Décocher "Finale" pour l'étape
            formsPage.uncheckStepFinale(stepIndex);

            LOG.info("Étape {} modifiée (Finale décochée)", stepIndex);
            return ActionResult.success(null,
                    "Etape " + stepIndex + " modifiee (Finale decochee)",
                    browser.screenshot("step-finale-unchecked"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la modification de l'étape", e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("step-modify-error"));
        }
    }

    /**
     * Modifie une etape existante pour decocher "Finale" par nom.
     * Cela permet d'ajouter une transition depuis cette etape.
     */
    public ActionResult<Void> uncheckStepFinaleByName(String stepName) {
        LOG.info("Décochage de 'Finale' pour l'étape '{}'", stepName);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer via openFormByName
            LOG.info("Ouverture du formulaire '{}' depuis la liste", currentFormName);
            formsPage.openFormByName(currentFormName);

            // Décocher "Finale" pour l'étape par nom
            formsPage.uncheckStepFinaleByName(stepName);

            LOG.info("Étape '{}' modifiée (Finale décochée)", stepName);
            return ActionResult.success(null,
                    "Etape '" + stepName + "' modifiee (Finale decochee)",
                    browser.screenshot("step-finale-unchecked"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la modification de l'étape '{}'", stepName, e);
            return ActionResult.failure("Erreur: " + e.getMessage(),
                    browser.screenshot("step-modify-error"));
        }
    }

    /**
     * Configure la transition entre deux etapes.
     * Basé sur le script Playwright de l'utilisateur:
     * 1. Naviguer vers le formulaire
     * 2. Cliquer sur onglet Etapes
     * 3. Cliquer sur "Modifier l'étape" pour l'étape source
     * 4. Cliquer sur "Ajouter une liaison"
     * 5. Confirmer dans l'Offcanvas
     */
    public ActionResult<Void> configureStepTransition(String fromStep) {
        LOG.info("Configuration de la transition depuis l'etape '{}'", fromStep);

        try {
            if (currentFormName == null) {
                return ActionResult.failure(
                        "Aucun formulaire en cours d'edition. Creez d'abord un formulaire.",
                        browser.screenshot("no-form-context"));
            }

            // TOUJOURS naviguer via openFormByName
            LOG.info("Ouverture du formulaire '{}' depuis la liste", currentFormName);
            formsPage.openFormByName(currentFormName);

            // Cliquer sur l'onglet Etapes
            formsPage.clickStepsTab();
            LOG.info("Onglet Etapes cliqué");

            // Cliquer sur "Modifier l'étape" pour l'étape source par son nom
            formsPage.clickModifyStepByName(fromStep);
            LOG.info("Modification de l'étape '{}' ouverte", fromStep);

            // Ajouter la liaison
            formsPage.addStepTransition();
            LOG.info("Liaison ajoutée depuis '{}'", fromStep);

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
     * Ajoute une question liste déroulante à l'étape.
     */
    public ActionResult<QuestionInfo> addDropdownQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question liste déroulante '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addDropdownQuestion(questionTitle);
            LOG.info("Question liste déroulante '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "DROPDOWN"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question fichier à l'étape.
     */
    public ActionResult<QuestionInfo> addFileQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question fichier '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addFileQuestion(questionTitle);
            LOG.info("Question fichier '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "FILE"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question attribut Mylutece à l'étape.
     */
    public ActionResult<QuestionInfo> addMylutecAttributeQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question attribut Mylutece '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addMylutecAttributeQuestion(questionTitle);
            LOG.info("Question attribut Mylutece '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "MYLUTECE_ATTRIBUTE"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question géolocalisation à l'étape.
     */
    public ActionResult<QuestionInfo> addGeolocationQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question géolocalisation '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addGeolocationQuestion(questionTitle);
            LOG.info("Question géolocalisation '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "GEOLOCATION"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question liste triable à l'étape.
     */
    public ActionResult<QuestionInfo> addSortableListQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question liste triable '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addSortableListQuestion(questionTitle);
            LOG.info("Question liste triable '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "SORTABLE_LIST"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question zone de texte long à l'étape.
     */
    public ActionResult<QuestionInfo> addTextareaQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question zone de texte long '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addTextareaQuestion(questionTitle);
            LOG.info("Question zone de texte long '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "TEXTAREA"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question bouton radio à l'étape.
     */
    public ActionResult<QuestionInfo> addRadioButtonQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question bouton radio '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addRadioButtonQuestion(questionTitle);
            LOG.info("Question bouton radio '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "RADIO"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question case à cocher à l'étape.
     */
    public ActionResult<QuestionInfo> addCheckboxQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question case à cocher '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addCheckboxQuestion(questionTitle);
            LOG.info("Question case à cocher '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "CHECKBOX"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question numérotation à l'étape.
     */
    public ActionResult<QuestionInfo> addNumberingQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question numérotation '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addNumberingQuestion(questionTitle);
            LOG.info("Question numérotation '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "NUMBERING"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Ajoute une question image à l'étape.
     */
    public ActionResult<QuestionInfo> addImageQuestion(String stepName, String questionTitle) {
        LOG.info("Ajout question image '{}' à l'étape '{}'", questionTitle, stepName);
        try {
            if (currentFormName == null) {
                return ActionResult.failure("Aucun formulaire en cours d'édition.", browser.screenshot("no-form-context"));
            }
            // Navigation obligatoire
            {
                formsPage.openFormByName(currentFormName);
                formsPage.clickStepsTab();
                formsPage.clickModifyStepByName(stepName);
                formsPage.clickQuestionsTab();
            }
            formsPage.addImageQuestion(questionTitle);
            LOG.info("Question image '{}' ajoutée", questionTitle);
            return ActionResult.success(new QuestionInfo(questionTitle, "IMAGE"), "Question ajoutée", browser.screenshot("question-added"));
        } catch (Exception e) {
            LOG.error("Erreur ajout question", e);
            currentStepName = null;
            return ActionResult.failure("Erreur: " + e.getMessage(), browser.screenshot("error"));
        }
    }

    /**
     * Navigue vers la liste des formulaires et retourne la liste.
     */
    public ActionResult<String> navigateToList() {
        try {
            formsPage.navigateToList();
            if (formsPage.isListDisplayed()) {
                var forms = formsPage.getFormsList();
                if (forms.isEmpty()) {
                    return ActionResult.success("Aucun formulaire trouvé",
                            "Aucun formulaire dans la liste");
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
}
