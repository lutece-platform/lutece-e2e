package fr.paris.lutece.e2e.tests.macro.scenarios;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;

/**
 * Formulaire a embranchement : l'etape d'identite mene a l'une ou l'autre des deux etapes de
 * detail selon la nature de la demande, et les deux branches se rejoignent sur une confirmation.
 *
 * <p>Fragment de scenario reutilisable, avec ses libelles d'etapes, de questions et de choix :
 * une suite qui eprouve un autre workflow, ou un autre parcours d'usager, s'appuie sur ce
 * formulaire sans le redecrire.</p>
 */
public final class FormulaireBranchantScenario {

    public static final String ETAPE_IDENTITE = "Identite";

    public static final String ETAPE_SUBVENTION = "Dossier de subvention";

    public static final String ETAPE_INFORMATION = "Demande d'information";

    public static final String ETAPE_CONFIRMATION = "Confirmation";


    public static final String Q_NATURE = "Nature de la demande";

    public static final String Q_NOM = "Nom du demandeur";

    public static final String Q_NAISSANCE = "Date de naissance";

    public static final String Q_OBJET = "Objet de la demande";

    public static final String Q_MONTANT = "Montant demande";

    public static final String Q_SUJET = "Sujet de la question";


    public static final String CHOIX_SUBVENTION = "Subvention";

    public static final String CHOIX_INFORMATION = "Information";


    private FormulaireBranchantScenario() {
    }

    /**
     * Formulaire a embranchement : quatre etapes, deux branches qui se rejoignent.
     *
     * <p>L'etape d'identite porte la question qui pilote le parcours. Deux transitions en partent :
     * la premiere, conditionnee sur le choix « subvention », mene au dossier ; la seconde, laissee
     * sans condition, sert de sortie par defaut et mene a la demande d'information. Lutece les
     * examine dans l'ordre de leur priorite et emprunte la premiere dont les conditions sont
     * satisfaites — l'ordre de creation fait donc partie du scenario.</p>
     *
     * @param page    page Playwright pilotant le navigateur
     * @param baseUrl adresse du site cible
     * @param suffix  suffixe unique du run
     * @return le contexte formulaire alimente
     */
    public static FormsContext construire(Page page, String baseUrl, String suffix) {
        FormsContext forms = new FormsContext(page, baseUrl, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Parcours complet"));

        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_IDENTITE));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_SUBVENTION));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_INFORMATION));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep(ETAPE_CONFIRMATION));
        SetStepInitialMacroTest.run(forms, StepTargetDataSet.of(0));
        SetStepFinalMacroTest.run(forms, StepTargetDataSet.of(3));
        UnsetStepFinalMacroTest.run(forms, StepTargetDataSet.of(0));

        questionsIdentite(forms);
        questionsSubvention(forms);
        questionsInformation(forms);
        questionsConfirmation(forms);

        // Creer une etape la relie automatiquement a la precedente : le formulaire possede donc
        // deja un enchainement lineaire que le scenario n'a pas demande. Poser les liaisons voulues
        // par-dessus donnerait un graphe hybride, ou la liaison automatique, prioritaire, l'emporte.
        for (int etape = 0; etape < forms.steps.size(); etape++) {
            ClearStepTransitionsMacroTest.run(forms, StepTargetDataSet.of(etape));
        }

        CreateTransitionMacroTest.run(forms, TransitionDataSet.of(0, 1));
        CreateTransitionMacroTest.run(forms, TransitionDataSet.of(0, 2));
        CreateTransitionMacroTest.run(forms, TransitionDataSet.of(1, 3));
        CreateTransitionMacroTest.run(forms, TransitionDataSet.of(2, 3));

        AddTransitionControlMacroTest.run(forms,
            TransitionControlDataSet.valeurChoisie(0, 1, indexQuestion(forms, Q_NATURE), CHOIX_SUBVENTION));
        return forms;
    }

    /**
     * Questions de l'etape d'identite, dont celle qui pilote l'embranchement.
     *
     * @param forms contexte formulaire courant
     */
    private static void questionsIdentite(FormsContext forms) {
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, Q_NATURE).onStep(0));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of(Q_NATURE, CHOIX_SUBVENTION, CHOIX_INFORMATION));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, Q_NOM).onStep(0));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.DATE, Q_NAISSANCE).onStep(0));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TELEPHONE, "Telephone").onStep(0));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.NUMBERING, "Numero de dossier").onStep(0));
    }

    /**
     * Questions de la branche « dossier de subvention ».
     *
     * @param forms contexte formulaire courant
     */
    private static void questionsSubvention(FormsContext forms) {
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXTAREA, Q_OBJET).onStep(1));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.NUMBER, Q_MONTANT).onStep(1));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Type de demandeur").onStep(1));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of("Type de demandeur", "Association", "Entreprise"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.CHECKBOX, "Dispositifs concernes").onStep(1));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of("Dispositifs concernes", "Fonctionnement", "Investissement"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT, "Direction de rattachement").onStep(1));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of("Direction de rattachement", "Direction A", "Direction B"));
    }

    /**
     * Questions de la branche « demande d'information ».
     *
     * @param forms contexte formulaire courant
     */
    private static void questionsInformation(FormsContext forms) {
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, Q_SUJET).onStep(2));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXTAREA, "Precisions complementaires").onStep(2));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT, "Canal de reponse souhaite").onStep(2));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of("Canal de reponse souhaite", "Courriel", "Courrier"));
    }

    /**
     * Questions de l'etape de confirmation, commune aux deux branches.
     *
     * @param forms contexte formulaire courant
     */
    private static void questionsConfirmation(FormsContext forms) {
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Personne a contacter").onStep(3));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.FILE, "Piece justificative").onStep(3));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.CHECKBOX, "Engagement sur l'honneur").onStep(3));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of("Engagement sur l'honneur", "Je certifie l'exactitude"));
        // Types moins courants, regroupes sur l'etape commune aux deux branches : ils elargissent la
        // couverture sans dependre du chemin emprunte.
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT_ORDER, "Priorites par ordre").onStep(3));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SLOT, "Creneau de rendez-vous").onStep(3));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SESSION, "Session").onStep(3));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Attribut utilisateur").onStep(3));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TERMS_OF_SERVICE, "Conditions d'utilisation").onStep(3));
        // Les types commentaire, image et geolocalisation sont volontairement absents : leurs
        // briques respectives echouent deja isolement, pour des raisons etrangeres a ce scenario.
        // Les inclure ferait tomber tout le parcours sur un defaut qu'il n'a pas vocation a
        // eprouver, et masquerait les regressions qu'il surveille reellement.
    }

    /**
     * Position d'une question dans le contexte, par son titre.
     *
     * @param forms contexte formulaire courant
     * @param titre titre de la question
     * @return sa position dans {@code forms.questions}
     */
    private static int indexQuestion(FormsContext forms, String titre) {
        for (int i = 0; i < forms.questions.size(); i++) {
            if (titre.equals(forms.questions.get(i).title)) {
                return i;
            }
        }
        throw new IllegalStateException("Question '" + titre + "' absente du contexte");
    }

    /**
     * Associe le formulaire au workflow actif, puis le publie.
     *
     * @param forms contexte formulaire courant
     * @param wf    workflow a associer
     */
    public static void mettreEnService(FormsContext forms, WorkflowContext wf) {
        forms.workflowId = wf.workflowId;
        forms.workflowName = wf.workflowName;
        AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
    }
}
