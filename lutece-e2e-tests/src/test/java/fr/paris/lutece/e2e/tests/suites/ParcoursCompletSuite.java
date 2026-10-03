package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import fr.paris.lutece.e2e.tests.macro.unittree.AddUsersToUnitMacroTest;
import fr.paris.lutece.e2e.tests.macro.system.RunDaemonMacroTest;
import fr.paris.lutece.e2e.tests.macro.unittree.CreateUnitMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Parcours metier complet, de l'organisation jusqu'a l'instruction de deux reponses.
 *
 * <p>Couvre en une seule execution les quatre briques fonctionnelles du socle, chacune dans sa
 * version non triviale, et surtout leurs points de jonction — c'est la ou les regressions
 * apparaissent, pas dans les briques prises isolement :</p>
 *
 * <ol>
 *   <li><b>Unittree</b> : deux entites organisationnelles et l'affectation d'un agent.</li>
 *   <li><b>Workflow</b> : cinq etats et quatre actions formant un graphe branchant, portant
 *       ensemble onze types de taches differents repartis sur les quatre actions —
 *       publication, commentaire, affectation a une entite, confirmation, notification de l'usager,
 *       demande de complement, demande de correction, piece jointe, notification de l'entite.
 *       <i>Chacune est reellement parametree et sa configuration relue</i> : une tache inseree sans
 *       configuration n'echoue pas a l'enregistrement, elle fait echouer l'action bien plus tard,
 *       par une erreur serveur qui annule la transition sans aucun message a l'ecran.</li>
 *   <li><b>Formulaire</b> : quatre etapes dont un embranchement — l'etape d'identite mene a l'une
 *       ou l'autre des deux etapes de detail selon la nature de la demande, et les deux branches se
 *       rejoignent sur une etape finale de confirmation. Les questions couvrent quinze
 *       types differents, listes reellement pourvues de leurs choix. Puis association au workflow et
 *       publication.</li>
 *   <li><b>Front office puis instruction</b> : <i>deux</i> soumissions, une par branche, chacune
 *       verifiant l'etape atteinte et celle qui a ete ecartee, puis l'enchainement de deux actions
 *       de workflow sur la reponse recue, avec controle de l'etat, de l'historique, du
 *       contenu de la notification adressee a l'usager et de ce que les taches y ont depose.</li>
 * </ol>
 *
 * <p>L'ordre n'est pas arbitraire : le workflow doit etre actif avant d'etre associe, le
 * formulaire publie avant d'etre ouvert en front office, et une reponse soumise avant qu'une
 * action puisse s'y appliquer. Les taches qui se branchent sur un formulaire font exception et
 * sont ajoutees apres sa creation, d'ou le retour sur le workflow en milieu de parcours.</p>
 *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.ParcoursCompletSuite \
 *     -Dlutece.base.url=https://mon-site/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Parcours complet")
@Tag("macro")
@Tag("suite")
@DisplayName("Parcours complet : unites, workflow multi-etats, formulaire a embranchement, deux soumissions FO et instruction")
public class ParcoursCompletSuite extends MacroTest {

    private static final String UNITE_DIRECTION = "Direction des demarches";
    private static final String UNITE_SERVICE = "Service instruction";

    private static final String ETAT_NOUVELLE = "Nouvelle demande";
    private static final String ETAT_INSTRUCTION = "En cours d'instruction";
    private static final String ETAT_COMPLEMENT = "Complement demande";
    private static final String ETAT_CORRECTION = "Correction demandee";
    private static final String ETAT_CLOTUREE = "Cloturee";

    private static final String ACTION_PRISE_EN_CHARGE = "Prendre en charge";
    private static final String ACTION_COMPLEMENT = "Demander un complement";
    private static final String ACTION_CORRECTION = "Demander une correction";
    private static final String ACTION_CLOTURE = "Cloturer";

    private static final String ETAPE_IDENTITE = "Identite";
    private static final String ETAPE_SUBVENTION = "Dossier de subvention";
    private static final String ETAPE_INFORMATION = "Demande d'information";
    private static final String ETAPE_CONFIRMATION = "Confirmation";

    private static final String Q_NATURE = "Nature de la demande";
    private static final String Q_NOM = "Nom du demandeur";
    private static final String Q_NAISSANCE = "Date de naissance";
    private static final String Q_OBJET = "Objet de la demande";
    private static final String Q_MONTANT = "Montant demande";
    private static final String Q_SUJET = "Sujet de la question";

    private static final String CHOIX_SUBVENTION = "Subvention";
    private static final String CHOIX_INFORMATION = "Information";

    private static final String COMMENTAIRE_INSTRUCTION = "Commentaire d'instruction";
    private static final String MESSAGE_NOTIFICATION = "Votre demande est en cours d'instruction";
    private static final String MESSAGE_COMPLEMENT = "Merci de completer votre dossier.";
    private static final String TRACE_NOTIFICATION = "Voir les notifications";
    private static final String CANAL_NOTIFICATION = "Agent";

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Organisation, workflow outille, formulaire a embranchement, deux soumissions et instruction")
    void parcoursComplet() {
        String suffix = newSuffix();
        login();

        UnittreeContext units = organisation(suffix);
        WorkflowContext wf = workflow(suffix, units);
        FormsContext forms = formulaire(suffix);
        completerWorkflowAvecLeFormulaire(wf, forms);
        mettreEnService(forms, wf);

        soumettre(forms, CHOIX_SUBVENTION, ETAPE_SUBVENTION, ETAPE_INFORMATION);
        soumettre(forms, CHOIX_INFORMATION, ETAPE_INFORMATION, ETAPE_SUBVENTION);
        indexer(forms);

        instruction(forms);

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, units.units.size(),
                "Les deux unites doivent avoir ete creees"),
            () -> Assertions.assertEquals(5, wf.states.size(),
                "Les cinq etats du workflow doivent avoir ete crees"),
            () -> Assertions.assertEquals(4, wf.actions.size(),
                "Les quatre actions du workflow doivent avoir ete creees"),
            () -> Assertions.assertEquals(4, forms.steps.size(),
                "Les quatre etapes du formulaire doivent avoir ete creees"));
    }

    /**
     * Entites organisationnelles et affectation d'un agent.
     *
     * <p>Les deux unites sont creees a la racine plutot qu'imbriquees : la page de creation sous
     * parent depend d'un identifiant que la liste des unites n'expose pas de maniere fiable selon
     * les sites, ce qui rendait la brique tributaire de l'environnement sans rien apporter au
     * scenario — l'affectation d'une ressource a une entite, elle, est bien couverte.</p>
     *
     * @param suffix suffixe unique du run, pour des libelles non collisionnants
     * @return le contexte unittree alimente
     */
    private UnittreeContext organisation(String suffix) {
        UnittreeContext units = new UnittreeContext(page, BASE_URL, suffix);
        CreateUnitMacroTest.run(units, UnitDataSet.of(UNITE_DIRECTION));
        CreateUnitMacroTest.run(units, UnitDataSet.of(UNITE_SERVICE));
        AddUsersToUnitMacroTest.run(units, UserAssignmentDataSet.defaults());
        return units;
    }

    /**
     * Workflow d'instruction : cinq etats, quatre actions, et les taches qui ne dependent que du
     * workflow lui-meme.
     *
     * <p>Les taches sont volontairement reparties sur les quatre actions plutot que concentrees sur
     * l'action d'entree : c'est ce que fait un workflow reel, et seul cet eclatement verifie que
     * chaque transition execute bien les siennes.</p>
     *
     * @param suffix suffixe unique du run
     * @param units  entites organisationnelles sur lesquelles s'appuie la tache d'affectation
     * @return le contexte workflow alimente
     */
    private WorkflowContext workflow(String suffix, UnittreeContext units) {
        WorkflowContext wf = new WorkflowContext(page, BASE_URL, suffix);
        CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName("Instruction demande"));

        AddStateMacroTest.run(wf, StateDataSet.initial(ETAT_NOUVELLE));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_INSTRUCTION));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_COMPLEMENT));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_CORRECTION));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_CLOTUREE));

        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_PRISE_EN_CHARGE, 0, 1));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_COMPLEMENT, 1, 2));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_CORRECTION, 1, 3));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_CLOTURE, 1, 4));

        tachesDePriseEnCharge(wf, units);
        tachesDeComplement(wf);
        tachesDeCorrection(wf);
        tachesDeCloture(wf);

        ActivateWorkflowMacroTest.run(wf);
        VerifyWorkflowActiveMacroTest.run(wf);
        return wf;
    }

    /**
     * Taches de l'action d'entree : publication, commentaire, affectation, notification, confirmation.
     *
     * @param wf    contexte workflow courant
     * @param units entites organisationnelles disponibles
     */
    private void tachesDePriseEnCharge(WorkflowContext wf, UnittreeContext units) {
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("modifyUpdateStatusTask", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .radio("published", "true"));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskTypeComment", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .texte("title", COMMENTAIRE_INSTRUCTION)
            .radio("mandatory", "false")
            .radio("richText", "true"));

        // Le mode de selection retenu est celui qui se suffit a lui-meme. « Assigner une entite
        // selon le parametrage defini » exige, apres l'ajout du mode, de designer une configuration
        // parametrable puis un formulaire support, et ce parametrage se fait sur le formulaire
        // lui-meme : il est couvert plus loin, une fois le formulaire cree.
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskUnitAssignmentManual", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .radio("assignment_type", "create")
            .selection("unit_selection_id_to_add", "UnitSelectionSpecificUnit")
            .appliquer("addUnitSelection")
            .selection("task_unit_assignment_config_selection_specific_unit_id",
                String.valueOf(units.unit(0).id)));

        // La notification se parametre en deux temps : le fournisseur de donnees doit etre
        // enregistre avant que les canaux ne deviennent proposables. Le canal « agent » est celui
        // dont le contenu se retrouve dans l'historique de la reponse, donc observable.
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskNotifyGru", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .selectionLibelle("list_provider", "Forms")
            .case_("marker_providers", "workflow-notifygru.commentMarkerProvider")
            .enregistrer()
            .selection("added_notification_config", "agent")
            .appliquer("AddNotificationConfig")
            .texte("status_text_agent", ETAT_INSTRUCTION)
            .texte("message_agent", MESSAGE_NOTIFICATION));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskTypeConfirmAction", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .texte("message", "Confirmez-vous la prise en charge de cette demande ?"));
    }

    /**
     * Taches de l'action de demande de complement.
     *
     * @param wf contexte workflow courant
     */
    private void tachesDeComplement(WorkflowContext wf) {
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("completeFormResponseTypeTask", ACTION_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .selection("idStateAfterEdition", wf.states.get(2).name)
            .texte("defaultMessage", MESSAGE_COMPLEMENT));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskUnitAssignmentNotification", ACTION_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .texte("subject", "Complement demande sur un dossier")
            .texte("message", "Un complement a ete demande a l'usager."));
    }

    /**
     * Taches de l'action de demande de correction.
     *
     * @param wf contexte workflow courant
     */
    private void tachesDeCorrection(WorkflowContext wf) {
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("resubmitFormResponseTypeTask", ACTION_CORRECTION));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .selection("idStateAfterEdition", wf.states.get(3).name)
            .texte("defaultMessage", "Merci de corriger votre saisie."));

        // Piece jointe non obligatoire : l'exiger imposerait un televersement a chaque passage de
        // l'action, ce qui n'est pas l'objet du controle.
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskTypeUpload", ACTION_CORRECTION));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .texte("title", "Piece a l'appui de la correction")
            .texte("maxFile", "1")
            .texte("maxSizeFile", "1000000")
            .radio("mandatory", "false"));
    }

    /**
     * Taches de l'action de cloture.
     *
     * @param wf contexte workflow courant
     */
    private void tachesDeCloture(WorkflowContext wf) {
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("modifyUpdateDateTypeTask", ACTION_CLOTURE));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide());
    }

    /**
     * Taches qui se branchent sur un formulaire, ajoutees une fois celui-ci cree.
     *
     * <p>Ces taches designent un formulaire support dans leur configuration : elles ne peuvent donc
     * pas etre posees au moment ou le workflow est bati. Revenir sur le workflow a ce stade est la
     * demarche normale, et c'est aussi ce qui verifie qu'un workflow deja actif reste modifiable.</p>
     *
     * @param wf    contexte workflow courant
     * @param forms formulaire support
     */
    private void completerWorkflowAvecLeFormulaire(WorkflowContext wf, FormsContext forms) {
        // La notification de l'usager ne lit pas la reponse directement : elle passe par un mapping
        // qui, pour ce formulaire, designe les questions portant ses coordonnees. Sans lui, l'action
        // echoue a l'execution et annule toute la transition.
        CreateNotifygruMappingMacroTest.run(forms);

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("editFormResponseTypeTask", ACTION_CLOTURE));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .selectionLibelle("form_select", forms.formTitle)
            .appliquer("select_form_config"));
    }

    /**
     * Associe le formulaire au workflow actif, puis le publie.
     *
     * @param forms contexte formulaire courant
     * @param wf    workflow a associer
     */
    private void mettreEnService(FormsContext forms, WorkflowContext wf) {
        forms.workflowId = wf.workflowId;
        forms.workflowName = wf.workflowName;
        AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
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
     * @param suffix suffixe unique du run
     * @return le contexte formulaire alimente
     */
    private FormsContext formulaire(String suffix) {
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
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
    private void questionsIdentite(FormsContext forms) {
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
    private void questionsSubvention(FormsContext forms) {
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
    private void questionsInformation(FormsContext forms) {
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
    private void questionsConfirmation(FormsContext forms) {
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
    private int indexQuestion(FormsContext forms, String titre) {
        for (int i = 0; i < forms.questions.size(); i++) {
            if (titre.equals(forms.questions.get(i).title)) {
                return i;
            }
        }
        throw new IllegalStateException("Question '" + titre + "' absente du contexte");
    }

    /**
     * Soumet une reponse en empruntant l'une des deux branches.
     *
     * <p>Verifier l'etape atteinte <i>et</i> celle qui a ete ecartee est ce qui prouve que
     * l'embranchement a joue : sans ce controle, une condition inoperante laisserait simplement le
     * parcours suivre la premiere transition, et les deux soumissions passeraient par la meme
     * branche sans que rien ne le signale.</p>
     *
     * @param forms         contexte formulaire courant
     * @param choix         reponse donnee a la question qui pilote le parcours
     * @param etapeAttendue titre de l'etape qui doit suivre
     * @param etapeEcartee  titre de l'etape qui ne doit pas etre atteinte
     */
    private void soumettre(FormsContext forms, String choix, String etapeAttendue, String etapeEcartee) {
        OpenFormFOMacroTest.run(forms);
        VerifyStepFOMacroTest.run(forms, ETAPE_IDENTITE);

        SelectChoiceFOMacroTest.run(forms, ChoiceSelectionDataSet.of(Q_NATURE, choix));
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.text(Q_NOM, "Dupont"));
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.date(Q_NAISSANCE, "15/10/1980"));
        remplirEtape(forms, ETAPE_IDENTITE);
        NextStepFOMacroTest.run(forms);

        VerifyStepFOMacroTest.run(forms, etapeAttendue);
        VerifyStepFOMacroTest.absente(forms, etapeEcartee);
        remplirEtape(forms, etapeAttendue);
        NextStepFOMacroTest.run(forms);

        VerifyStepFOMacroTest.run(forms, ETAPE_CONFIRMATION);
        remplirEtape(forms, ETAPE_CONFIRMATION);
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
    }

    /**
     * Renseigne toutes les questions de l'etape affichee et exige qu'au moins une l'ait ete.
     *
     * <p>Le remplissage exhaustif est silencieux par construction : il parcourt ce qu'il trouve.
     * Si les champs cessaient d'etre reconnus — un type de question rendu differemment, un
     * selecteur devenu caduc —, l'etape serait traversee vide et la reponse soumise sans contenu,
     * sans qu'aucune brique ne proteste.</p>
     *
     * @param forms contexte formulaire courant
     * @param etape titre de l'etape, pour situer un eventuel echec
     */
    private void remplirEtape(FormsContext forms, String etape) {
        int renseignes = FillAllFieldsFOMacroTest.run(forms);
        Assertions.assertTrue(renseignes > 0,
            "L'etape '" + etape + "' n'a vu aucune question renseignee : la reponse serait soumise "
                + "sans contenu");
    }

    /**
     * Declenche l'indexation pour que les reponses soumises apparaissent dans la multivue.
     *
     * <p>La multivue ne lit pas la base mais un index, alimente par un demon : une reponse tout
     * juste soumise y est absente tant qu'il n'est pas passe.</p>
     *
     * @param forms contexte formulaire courant
     */
    private void indexer(FormsContext forms) {
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        VerifyResponseSubmittedMacroTest.run(forms);
    }

    /**
     * Instruction d'une reponse : enchainement de deux actions, avec controle de leurs effets.
     *
     * @param forms contexte formulaire courant
     */
    private void instruction(FormsContext forms) {
        OpenMultiviewMacroTest.run(forms);
        OpenResponseDetailMacroTest.run(forms);
        VerifyResponseStateMacroTest.run(forms, ResponseStateDataSet.sansHistorique(ETAT_NOUVELLE));

        RunWorkflowActionOnResponseMacroTest.run(forms, ResponseActionDataSet.of(ACTION_PRISE_EN_CHARGE));
        // Les traces attendues sont celles des taches de l'action : le commentaire, l'entite
        // affectee, et la notification adressee a l'usager — c'est dans l'historique de la reponse
        // que son existence se constate.
        VerifyResponseStateMacroTest.run(forms, ResponseStateDataSet.avecTraces(
            ETAT_INSTRUCTION, ACTION_PRISE_EN_CHARGE,
            COMMENTAIRE_INSTRUCTION, UNITE_DIRECTION, TRACE_NOTIFICATION));
        VerifyNotificationMacroTest.run(forms,
            NotificationDataSet.of(CANAL_NOTIFICATION, MESSAGE_NOTIFICATION));

        RunWorkflowActionOnResponseMacroTest.run(forms, ResponseActionDataSet.of(ACTION_COMPLEMENT));
        VerifyResponseStateMacroTest.run(forms, ResponseStateDataSet.avecTraces(
            ETAT_COMPLEMENT, ACTION_COMPLEMENT, MESSAGE_COMPLEMENT));
    }
}
