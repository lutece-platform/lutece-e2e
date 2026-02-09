package fr.paris.lutece.e2e.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.e2e.actions.AuthActions;
import fr.paris.lutece.e2e.actions.FormsActions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Tools LangChain4j pour la gestion des formulaires.
 */
@ApplicationScoped
public class FormsTools {

    @Inject
    FormsActions formsActions;

    @Inject
    AuthActions authActions;

    @Tool("Cree un nouveau formulaire dans Lutece. " +
          "Necessite d'etre connecte. " +
          "Retourne l'ID du formulaire cree.")
    public String createForm(
            @P("Titre du formulaire") String title,
            @P("Nom du workflow a associer (optionnel, laisser vide si aucun)") String workflowName,
            @P("Date de debut de disponibilite (format: YYYY-MM-DD ou 'today')") String startDate,
            @P("Date de fin de disponibilite (format: YYYY-MM-DD)") String endDate) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.createForm(title, workflowName, startDate, endDate);
        return result.toToolMessage();
    }

    @Tool("Ajoute une etape au formulaire. " +
          "Le premier parametre est le titre de l'etape (String). " +
          "Le second parametre indique si c'est la derniere etape du formulaire (Boolean: true ou false).")
    public String addStep(
            @P("title") String title,
            @P("isLastStep") boolean isLastStep) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addStep(title, isLastStep);
        return result.toToolMessage();
    }

    @Tool("Ajoute une etape non finale au formulaire (raccourci). " +
          "Utilise cette methode pour ajouter rapidement une etape intermediaire.")
    public String addIntermediateStep(
            @P("title") String title) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addStep(title, false);
        return result.toToolMessage();
    }

    @Tool("Ajoute une etape initiale au formulaire. " +
          "Utilise cette methode pour la premiere etape du formulaire (coche 'Initiale').")
    public String addInitialStep(
            @P("title") String title) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addInitialStep(title);
        return result.toToolMessage();
    }

    @Tool("Ajoute une etape finale au formulaire (raccourci). " +
          "Utilise cette methode pour ajouter la derniere etape du formulaire.")
    public String addFinalStep(
            @P("title") String title) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addStep(title, true);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type texte court a une etape.")
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
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addNumberQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type date a une etape.")
    public String addDateQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addDateQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Modifie une etape existante pour decocher 'Finale' par son nom. " +
          "Cela permet d'ajouter une transition depuis cette etape.")
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
            @P("Nom de l'etape source") String fromStep) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.configureStepTransition(fromStep);
        return result.toToolMessage();
    }

    @Tool("Publie un formulaire sur le portail pour qu'il soit accessible aux utilisateurs.")
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
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addDropdownQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type fichier a une etape. Permet de telecharger des fichiers.")
    public String addFileQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addFileQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type attribut Mylutece a une etape.")
    public String addMylutecAttributeQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addMylutecAttributeQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type geolocalisation a une etape.")
    public String addGeolocationQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addGeolocationQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type liste triable a une etape.")
    public String addSortableListQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addSortableListQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type zone de texte long a une etape.")
    public String addTextareaQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addTextareaQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type bouton radio a une etape.")
    public String addRadioButtonQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addRadioButtonQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type case a cocher a une etape.")
    public String addCheckboxQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addCheckboxQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type numerotation a une etape.")
    public String addNumberingQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addNumberingQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Ajoute une question de type image a une etape. Permet de telecharger des images.")
    public String addImageQuestion(
            @P("Nom de l'etape ou ajouter la question") String stepName,
            @P("Titre de la question") String questionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        var result = formsActions.addImageQuestion(stepName, questionTitle);
        return result.toToolMessage();
    }

    @Tool("Cree un formulaire complet avec 2 etapes, 3 questions et une transition. " +
          "Etape1 contient les questions texte, nombre et date. " +
          "Etape2 est l'etape finale. " +
          "Une transition est configuree de Etape1 vers Etape2.")
    public String createCompleteForm(
            @P("Nom du formulaire") String formName,
            @P("Nom du workflow a associer") String workflowName,
            @P("Nom de la premiere etape") String step1Name,
            @P("Nom de la deuxieme etape (finale)") String step2Name,
            @P("Titre de la question texte") String textQuestionTitle,
            @P("Titre de la question nombre") String numberQuestionTitle,
            @P("Titre de la question date") String dateQuestionTitle) {

        if (!authActions.isLoggedIn()) {
            return "ERREUR: Vous devez d'abord vous connecter avec login()";
        }

        StringBuilder results = new StringBuilder();

        // 1. Créer le formulaire
        var formResult = formsActions.createForm(formName, workflowName);
        if (!formResult.isSuccess()) {
            return "ERREUR création formulaire: " + formResult.getMessage();
        }
        results.append("✓ Formulaire '").append(formName).append("' créé\n");

        // 2. Ajouter l'étape 1 (non finale)
        var step1Result = formsActions.addStep(step1Name, false);
        if (!step1Result.isSuccess()) {
            return results + "ERREUR ajout étape 1: " + step1Result.getMessage();
        }
        results.append("✓ Étape '").append(step1Name).append("' ajoutée\n");

        // 3. Ajouter l'étape 2 (finale)
        var step2Result = formsActions.addStep(step2Name, true);
        if (!step2Result.isSuccess()) {
            return results + "ERREUR ajout étape 2: " + step2Result.getMessage();
        }
        results.append("✓ Étape finale '").append(step2Name).append("' ajoutée\n");

        // 4. Ajouter la question texte
        var textResult = formsActions.addTextQuestion(step1Name, textQuestionTitle);
        if (!textResult.isSuccess()) {
            return results + "ERREUR ajout question texte: " + textResult.getMessage();
        }
        results.append("✓ Question texte '").append(textQuestionTitle).append("' ajoutée\n");

        // 5. Ajouter la question nombre
        var numberResult = formsActions.addNumberQuestion(step1Name, numberQuestionTitle);
        if (!numberResult.isSuccess()) {
            return results + "ERREUR ajout question nombre: " + numberResult.getMessage();
        }
        results.append("✓ Question nombre '").append(numberQuestionTitle).append("' ajoutée\n");

        // 6. Ajouter la question date
        var dateResult = formsActions.addDateQuestion(step1Name, dateQuestionTitle);
        if (!dateResult.isSuccess()) {
            return results + "ERREUR ajout question date: " + dateResult.getMessage();
        }
        results.append("✓ Question date '").append(dateQuestionTitle).append("' ajoutée\n");

        // 7. Décocher Finale sur l'étape 1 (par son nom)
        var uncheckResult = formsActions.uncheckStepFinaleByName(step1Name);
        if (!uncheckResult.isSuccess()) {
            return results + "ERREUR décochage Finale: " + uncheckResult.getMessage();
        }
        results.append("✓ Étape '").append(step1Name).append("' modifiée (non finale)\n");

        // 8. Configurer la transition
        var transitionResult = formsActions.configureStepTransition(step1Name);
        if (!transitionResult.isSuccess()) {
            return results + "ERREUR configuration transition: " + transitionResult.getMessage();
        }
        results.append("✓ Transition ").append(step1Name).append(" → ").append(step2Name).append(" configurée\n");

        results.append("\n🎉 Formulaire complet créé avec succès!");
        return results.toString();
    }
}
