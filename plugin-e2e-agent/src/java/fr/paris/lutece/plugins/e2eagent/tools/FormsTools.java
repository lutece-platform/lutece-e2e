package fr.paris.lutece.plugins.e2eagent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.e2e.actions.AuthActions;
import fr.paris.lutece.e2e.actions.FormsActions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Tools LangChain4j pour la gestion des formulaires.
 * IMPORTANT: Utiliser createCompleteForm pour creer un formulaire complet en une seule operation.
 * NE PAS retenter si une operation echoue — retourner l'erreur a l'utilisateur.
 */
@ApplicationScoped
public class FormsTools {

    @Inject
    FormsActions formsActions;

    @Inject
    AuthActions authActions;

    @Tool("Cree un formulaire complet avec etapes, questions et transition en UNE SEULE operation. " +
          "C'est l'outil OBLIGATOIRE pour creer un formulaire. " +
          "Cree le formulaire, ajoute 2 etapes, ajoute les questions, decoche Finale sur l'etape 1, et configure la transition. " +
          "NE PAS retenter si ca echoue — retourner l'erreur a l'utilisateur.")
    public String createCompleteForm(
            @P("Nom du formulaire, par exemple 'MonFormulaire'") String formName,
            @P("Nom du workflow a associer, par exemple 'WF_RAF'") String workflowName,
            @P("Nom de la premiere etape (non finale), par exemple 'Saisie'") String step1Name,
            @P("Nom de la deuxieme etape (finale), par exemple 'Validation'") String step2Name,
            @P("Titre de la question texte sur l'etape 1, par exemple 'Nom'") String textQuestionTitle,
            @P("Titre de la question nombre sur l'etape 1, par exemple 'Age'") String numberQuestionTitle,
            @P("Titre de la question texte sur l'etape 2, par exemple 'Commentaire'") String step2QuestionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        StringBuilder results = new StringBuilder();

        // 1. Creer le formulaire
        var formResult = formsActions.createForm(formName, workflowName);
        if (!formResult.isSuccess()) {
            return "ECHEC creation formulaire: " + formResult.getMessage()
                    + " | URL: " + safeGetUrl();
        }
        int formId = formResult.getData().id();
        results.append("1. Formulaire '").append(formName).append("' cree (ID: ")
                .append(formId).append(")\n");

        // 2. Ajouter l'etape 1 (non finale)
        var step1Result = formsActions.addStep(step1Name, false);
        if (!step1Result.isSuccess()) {
            return results + "ECHEC ajout etape 1: " + step1Result.getMessage()
                    + " | URL: " + safeGetUrl() + " | formId=" + formId;
        }
        results.append("2. Etape '").append(step1Name).append("' ajoutee\n");

        // 3. Ajouter l'etape 2 (finale)
        var step2Result = formsActions.addStep(step2Name, true);
        if (!step2Result.isSuccess()) {
            return results + "ECHEC ajout etape 2: " + step2Result.getMessage()
                    + " | URL: " + safeGetUrl() + " | formId=" + formId;
        }
        results.append("3. Etape finale '").append(step2Name).append("' ajoutee\n");

        // 4. Ajouter question texte sur etape 1
        var textResult = formsActions.addTextQuestion(step1Name, textQuestionTitle);
        if (!textResult.isSuccess()) {
            return results + "ECHEC ajout question texte: " + textResult.getMessage()
                    + " | URL: " + safeGetUrl();
        }
        results.append("4. Question texte '").append(textQuestionTitle).append("' ajoutee sur '")
                .append(step1Name).append("'\n");

        // 5. Ajouter question nombre sur etape 1
        var numberResult = formsActions.addNumberQuestion(step1Name, numberQuestionTitle);
        if (!numberResult.isSuccess()) {
            return results + "ECHEC ajout question nombre: " + numberResult.getMessage()
                    + " | URL: " + safeGetUrl();
        }
        results.append("5. Question nombre '").append(numberQuestionTitle).append("' ajoutee sur '")
                .append(step1Name).append("'\n");

        // 6. Ajouter question texte sur etape 2
        var step2QResult = formsActions.addTextQuestion(step2Name, step2QuestionTitle);
        if (!step2QResult.isSuccess()) {
            return results + "ECHEC ajout question etape 2: " + step2QResult.getMessage()
                    + " | URL: " + safeGetUrl();
        }
        results.append("6. Question texte '").append(step2QuestionTitle).append("' ajoutee sur '")
                .append(step2Name).append("'\n");

        // 7. Decocher Finale sur l'etape 1
        var uncheckResult = formsActions.uncheckStepFinaleByName(step1Name);
        if (!uncheckResult.isSuccess()) {
            return results + "ECHEC decochage Finale: " + uncheckResult.getMessage()
                    + " | URL: " + safeGetUrl();
        }
        results.append("7. Etape '").append(step1Name).append("' modifiee (non finale)\n");

        // 8. Configurer la transition
        var transitionResult = formsActions.configureStepTransition(step1Name);
        if (!transitionResult.isSuccess()) {
            return results + "ECHEC configuration transition: " + transitionResult.getMessage()
                    + " | URL: " + safeGetUrl();
        }
        results.append("8. Transition ").append(step1Name).append(" -> ").append(step2Name)
                .append(" configuree\n");

        results.append("\nFormulaire complet cree avec succes!");
        return results.toString();
    }

    private String safeGetUrl() {
        try {
            return formsActions.getCurrentUrl();
        } catch (Exception e) {
            return "URL inconnue";
        }
    }

    @Tool("Cree un nouveau formulaire vide (sans etapes ni questions). " +
          "Utiliser UNIQUEMENT si le formulaire ne correspond pas au schema de createCompleteForm. " +
          "Si ca echoue, NE PAS retenter — retourner l'erreur a l'utilisateur.")
    public String createForm(
            @P("Titre du formulaire") String title,
            @P("Nom du workflow a associer (laisser vide si aucun)") String workflowName,
            @P("Date de debut (format: YYYY-MM-DD ou 'today')") String startDate,
            @P("Date de fin (format: YYYY-MM-DD)") String endDate) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.createForm(title, workflowName, startDate, endDate);
        return result.toToolMessage();
    }

    @Tool("Ajoute une etape au formulaire en cours d'edition. " +
          "Le formulaire doit avoir ete cree avant avec createForm ou createCompleteForm. " +
          "NE PAS retenter si ca echoue.")
    public String addStep(
            @P("Titre de l'etape, par exemple 'Saisie' ou 'Validation'") String title,
            @P("true si c'est la derniere etape du formulaire, false sinon") boolean isLastStep) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addStep(title, isLastStep);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type texte court a une etape du formulaire en cours d'edition. " +
          "NE PAS retenter si ca echoue.")
    public String addTextQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addTextQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type nombre a une etape.")
    public String addNumberQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addNumberQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type date a une etape.")
    public String addDateQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addDateQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Modifie une etape pour decocher 'Finale', permettant d'ajouter une transition.")
    public String uncheckStepFinale(
            @P("Nom de l'etape dont il faut decocher 'Finale'") String stepName) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.uncheckStepFinaleByName(stepName);
        return result.toToolMessage();
    }

    @Tool("Configure la transition entre deux etapes du formulaire.")
    public String configureTransition(
            @P("Nom de l'etape source de la transition") String fromStep) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.configureStepTransition(fromStep);
        return result.toToolMessage();
    }

    @Tool("Publie un formulaire sur le portail.")
    public String publishForm(
            @P("Titre du formulaire a publier") String formTitle,
            @P("Date de debut de publication (format: YYYY-MM-DD ou 'today')") String startDate) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.publishForm(formTitle, startDate);
        return result.toToolMessage();
    }

    @Tool("Affiche la liste des formulaires existants.")
    public String listForms() {
        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.navigateToList();
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type liste deroulante a une etape.")
    public String addDropdownQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {
        if (!authActions.isLoggedIn()) return "ERREUR: Connectez-vous d'abord";
        return formsActions.addDropdownQuestion(stepName, questionTitle).toToolMessage();
    }

    @Tool("Ajoute une question de type fichier a une etape.")
    public String addFileQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {
        if (!authActions.isLoggedIn()) return "ERREUR: Connectez-vous d'abord";
        return formsActions.addFileQuestion(stepName, questionTitle).toToolMessage();
    }

    @Tool("Ajoute une question de type zone de texte long a une etape.")
    public String addTextareaQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {
        if (!authActions.isLoggedIn()) return "ERREUR: Connectez-vous d'abord";
        return formsActions.addTextareaQuestion(stepName, questionTitle).toToolMessage();
    }

    @Tool("Ajoute une question de type bouton radio a une etape.")
    public String addRadioButtonQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {
        if (!authActions.isLoggedIn()) return "ERREUR: Connectez-vous d'abord";
        return formsActions.addRadioButtonQuestion(stepName, questionTitle).toToolMessage();
    }

    @Tool("Ajoute une question de type case a cocher a une etape.")
    public String addCheckboxQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {
        if (!authActions.isLoggedIn()) return "ERREUR: Connectez-vous d'abord";
        return formsActions.addCheckboxQuestion(stepName, questionTitle).toToolMessage();
    }

    @Tool("Ajoute une question de type numerotation a une etape.")
    public String addNumberingQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {
        if (!authActions.isLoggedIn()) return "ERREUR: Connectez-vous d'abord";
        return formsActions.addNumberingQuestion(stepName, questionTitle).toToolMessage();
    }

    @Tool("Ajoute une question de type image a une etape.")
    public String addImageQuestion(
            @P("Nom de l'etape") String stepName,
            @P("Titre de la question") String questionTitle) {
        if (!authActions.isLoggedIn()) return "ERREUR: Connectez-vous d'abord";
        return formsActions.addImageQuestion(stepName, questionTitle).toToolMessage();
    }
}
