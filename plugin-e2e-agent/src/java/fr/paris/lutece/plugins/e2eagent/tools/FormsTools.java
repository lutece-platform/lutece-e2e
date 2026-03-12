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

    @Tool("Cree un formulaire complet avec N etapes et N questions en UNE SEULE operation. " +
          "C'est l'outil OBLIGATOIRE pour creer un formulaire. " +
          "Cree le formulaire, ajoute les etapes, ajoute les questions sur chaque etape, " +
          "decoche Finale sur les etapes non-finales, et configure les transitions entre etapes consecutives. " +
          "NE PAS retenter si ca echoue — retourner l'erreur a l'utilisateur.")
    public String createCompleteForm(
            @P("Nom du formulaire, par exemple 'MonFormulaire'") String formName,
            @P("Nom du workflow a associer, par exemple 'WF_RAF'. Laisser vide si aucun.") String workflowName,
            @P("Liste des etapes separees par des virgules. La derniere etape est la finale. " +
               "Exemple: Saisie, Verification, Validation") String steps,
            @P("Liste des questions au format 'NomEtape>type:Titre' separees par des virgules. " +
               "Types supportes: text, number, date, textarea, dropdown, file, radio, checkbox, numbering, image. " +
               "Exemple: Saisie>text:Nom, Saisie>number:Age, Saisie>date:DateNaissance, Verification>textarea:Commentaire, Validation>text:Signature") String questions) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        // Parser les etapes
        String[] stepNames = steps.split(",");
        if (stepNames.length < 1) {
            return "ERREUR: Il faut au moins 1 etape";
        }
        for (int i = 0; i < stepNames.length; i++) {
            stepNames[i] = stepNames[i].trim();
        }

        // Parser les questions
        String[] questionDefs = questions.split(",");

        StringBuilder results = new StringBuilder();
        results.append("Creation du formulaire '").append(formName).append("'...\n\n");
        int step = 1;

        // 1. Creer le formulaire
        var formResult = formsActions.createForm(formName,
                (workflowName != null && !workflowName.isBlank()) ? workflowName : null);
        if (!formResult.isSuccess()) {
            return "ECHEC creation formulaire: " + formResult.getMessage() + " | URL: " + safeGetUrl();
        }
        int formId = formResult.getData().id();
        results.append(step++).append(". Formulaire '").append(formName)
               .append("' cree (ID: ").append(formId).append(")\n");

        // 2. Ajouter les etapes (derniere = finale)
        for (int i = 0; i < stepNames.length; i++) {
            boolean isFinal = (i == stepNames.length - 1);
            var stepResult = formsActions.addStep(stepNames[i], isFinal);
            if (!stepResult.isSuccess()) {
                return results + "ECHEC ajout etape '" + stepNames[i] + "': " + stepResult.getMessage()
                        + " | URL: " + safeGetUrl();
            }
            results.append(step++).append(". Etape '").append(stepNames[i]).append("' ajoutee")
                   .append(isFinal ? " (finale)" : "").append("\n");
        }

        // 3. Ajouter les questions sur chaque etape
        for (String qDef : questionDefs) {
            String trimmed = qDef.trim();
            if (trimmed.isEmpty()) continue;

            // Format: "StepName>type:Title"
            int separatorIdx = trimmed.indexOf('>');
            if (separatorIdx < 0) {
                return results + "ERREUR: Format de question invalide '" + trimmed
                        + "'. Attendu: 'NomEtape>type:Titre'";
            }
            String qStepName = trimmed.substring(0, separatorIdx).trim();
            String typeAndTitle = trimmed.substring(separatorIdx + 1).trim();

            int colonIdx = typeAndTitle.indexOf(':');
            if (colonIdx < 0) {
                return results + "ERREUR: Format de question invalide '" + trimmed
                        + "'. Attendu: 'NomEtape>type:Titre'";
            }
            String qType = typeAndTitle.substring(0, colonIdx).trim();
            String qTitle = typeAndTitle.substring(colonIdx + 1).trim();

            var qResult = formsActions.addQuestionByType(qStepName, qTitle, qType);
            if (!qResult.isSuccess()) {
                return results + "ECHEC ajout question '" + qTitle + "' (" + qType + ") sur '"
                        + qStepName + "': " + qResult.getMessage() + " | URL: " + safeGetUrl();
            }
            results.append(step++).append(". Question ").append(qType).append(" '").append(qTitle)
                   .append("' ajoutee sur '").append(qStepName).append("'\n");
        }

        // 4. Decocher Finale et configurer les transitions pour les etapes non-finales
        if (stepNames.length > 1) {
            for (int i = 0; i < stepNames.length - 1; i++) {
                var uncheckResult = formsActions.uncheckStepFinaleByName(stepNames[i]);
                if (!uncheckResult.isSuccess()) {
                    return results + "ECHEC decochage Finale sur '" + stepNames[i] + "': "
                            + uncheckResult.getMessage() + " | URL: " + safeGetUrl();
                }
                results.append(step++).append(". Etape '").append(stepNames[i])
                       .append("' modifiee (non finale)\n");

                var transResult = formsActions.configureStepTransition(stepNames[i]);
                if (!transResult.isSuccess()) {
                    return results + "ECHEC transition '" + stepNames[i] + "' -> '"
                            + stepNames[i + 1] + "': " + transResult.getMessage() + " | URL: " + safeGetUrl();
                }
                results.append(step++).append(". Transition '").append(stepNames[i])
                       .append("' -> '").append(stepNames[i + 1]).append("' configuree\n");
            }
        }

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
