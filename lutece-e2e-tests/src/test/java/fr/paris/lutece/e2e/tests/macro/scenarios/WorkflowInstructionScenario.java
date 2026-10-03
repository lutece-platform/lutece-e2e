package fr.paris.lutece.e2e.tests.macro.scenarios;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.ConfigureFormWorkflowQuestionsMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.CreateNotifygruMappingMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.*;

import static fr.paris.lutece.e2e.tests.macro.scenarios.FormulaireBranchantScenario.Q_NAISSANCE;
import static fr.paris.lutece.e2e.tests.macro.scenarios.FormulaireBranchantScenario.Q_NATURE;
import static fr.paris.lutece.e2e.tests.macro.scenarios.FormulaireBranchantScenario.Q_NOM;

/**
 * Workflow d'instruction complet : etats, actions, et les taches que chacune porte.
 *
 * <p>Fragment de scenario reutilisable. Une suite qui veut eprouver autre chose — un formulaire
 * different, un autre parcours d'usager — s'appuie sur ce workflow sans le redecrire, et se refere
 * a ses libelles pour ses propres controles.</p>
 *
 * <p>Les taches sont reparties sur les actions plutot que concentrees sur l'action d'entree :
 * c'est ce que fait un workflow reel, et seul cet eclatement verifie que chaque transition
 * execute bien les siennes. Celles qui se branchent sur un formulaire ne peuvent pas etre posees
 * ici et le sont par {@link #completerAvecLeFormulaire}.</p>
 */
public final class WorkflowInstructionScenario {

    public static final String ETAT_NOUVELLE = "Nouvelle demande";

    public static final String ETAT_INSTRUCTION = "En cours d'instruction";

    public static final String ETAT_COMPLEMENT = "Complement demande";

    public static final String ETAT_CORRECTION = "Correction demandee";

    public static final String ETAT_COMPLEMENT_RECU = "Complement recu";

    public static final String ETAT_CORRECTION_RECUE = "Correction recue";

    public static final String ETAT_CLOTUREE = "Cloturee";


    public static final String ACTION_PRISE_EN_CHARGE = "Prendre en charge";

    public static final String ACTION_COMPLEMENT = "Demander un complement";

    public static final String ACTION_CORRECTION = "Demander une correction";

    public static final String ACTION_CLOTURE = "Cloturer";

    public static final String ACTION_REPRISE_COMPLEMENT = "Reprendre apres complement";

    public static final String ACTION_REPRISE_CORRECTION = "Reprendre apres correction";


    public static final String COMMENTAIRE_INSTRUCTION = "Commentaire d'instruction";

    public static final String MESSAGE_NOTIFICATION = "Votre demande est en cours d'instruction";

    public static final String MESSAGE_COMPLEMENT = "Merci de completer votre dossier.";

    public static final String MESSAGE_CORRECTION = "Merci de corriger votre saisie.";


    public static final String NOTIF_COMPLEMENT = "Completez votre dossier";

    public static final String NOTIF_CORRECTION = "Corrigez votre saisie";


    public static final String SIGNET_COMPLEMENT = "${complete_form_url!}";

    public static final String SIGNET_CORRECTION = "${resubmit_form_url!}";

    public static final String TRACE_NOTIFICATION = "Voir les notifications";

    public static final String CANAL_NOTIFICATION = "Agent";


    private WorkflowInstructionScenario() {
    }

    /**
     * Workflow d'instruction : cinq etats, quatre actions, et les taches qui ne dependent que du
     * workflow lui-meme.
     *
     * <p>Les taches sont volontairement reparties sur les quatre actions plutot que concentrees sur
     * l'action d'entree : c'est ce que fait un workflow reel, et seul cet eclatement verifie que
     * chaque transition execute bien les siennes.</p>
     *
     * @param page    page Playwright pilotant le navigateur
     * @param baseUrl adresse du site cible
     * @param suffix  suffixe unique du run
     * @param units  entites organisationnelles sur lesquelles s'appuie la tache d'affectation
     * @return le contexte workflow alimente
     */
    public static WorkflowContext construire(Page page, String baseUrl, String suffix,
        UnittreeContext units) {
        WorkflowContext wf = new WorkflowContext(page, baseUrl, suffix);
        CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName("Instruction demande"));

        AddStateMacroTest.run(wf, StateDataSet.initial(ETAT_NOUVELLE));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_INSTRUCTION));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_COMPLEMENT));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_CORRECTION));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_COMPLEMENT_RECU));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_CORRECTION_RECUE));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_CLOTUREE));

        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_PRISE_EN_CHARGE, 0, 1));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_COMPLEMENT, 1, 2));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_CORRECTION, 1, 3));
        // Les deux retours : une fois l'usager passe par le front office, la demande revient a
        // l'instruction. Sans ces actions, le dossier resterait bloque dans l'etat d'attente.
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_REPRISE_COMPLEMENT, 4, 1));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_REPRISE_CORRECTION, 5, 1));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_CLOTURE, 1, 6));

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
    private static void tachesDePriseEnCharge(WorkflowContext wf, UnittreeContext units) {
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("modifyUpdateStatusTask", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, "modifyUpdateStatusTask", TaskConfigDataSet.vide()
            .radio("published", "true"));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskTypeComment", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, "taskTypeComment", TaskConfigDataSet.vide()
            .texte("title", COMMENTAIRE_INSTRUCTION)
            .radio("mandatory", "false")
            .radio("richText", "true"));

        // Le mode de selection retenu est celui qui se suffit a lui-meme. « Assigner une entite
        // selon le parametrage defini » exige, apres l'ajout du mode, de designer une configuration
        // parametrable puis un formulaire support, et ce parametrage se fait sur le formulaire
        // lui-meme : il est couvert plus loin, une fois le formulaire cree.
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskUnitAssignmentManual", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, "taskUnitAssignmentManual", TaskConfigDataSet.vide()
            .radio("assignment_type", "create")
            .selection("unit_selection_id_to_add", "UnitSelectionSpecificUnit")
            .appliquer("addUnitSelection")
            .selection("task_unit_assignment_config_selection_specific_unit_id",
                String.valueOf(units.unit(0).id)));

        // La notification se parametre en deux temps : le fournisseur de donnees doit etre
        // enregistre avant que les canaux ne deviennent proposables. Le canal « agent » est celui
        // dont le contenu se retrouve dans l'historique de la reponse, donc observable.
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskNotifyGru", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, "taskNotifyGru", TaskConfigDataSet.vide()
            .selectionLibelle("list_provider", "Forms")
            .case_("marker_providers", "workflow-notifygru.commentMarkerProvider")
            .enregistrer()
            .selection("added_notification_config", "agent")
            .appliquer("AddNotificationConfig")
            .texte("status_text_agent", ETAT_INSTRUCTION)
            .texte("message_agent", MESSAGE_NOTIFICATION));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskTypeConfirmAction", ACTION_PRISE_EN_CHARGE));
        ConfigureTaskMacroTest.run(wf, "taskTypeConfirmAction", TaskConfigDataSet.vide()
            .texte("message", "Confirmez-vous la prise en charge de cette demande ?"));
    }

    /**
     * Taches de l'action de demande de complement.
     *
     * @param wf contexte workflow courant
     */
    private static void tachesDeComplement(WorkflowContext wf) {
        // L'etat de sortie est celui que prend la demande une fois l'usager passe par le front
        // office : c'est lui qui distingue une demande en attente d'une demande revenue completee.
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("completeFormResponseTypeTask", ACTION_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, "completeFormResponseTypeTask", TaskConfigDataSet.vide()
            .selection("idStateAfterEdition", ETAT_COMPLEMENT_RECU)
            .texte("defaultMessage", MESSAGE_COMPLEMENT));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskNotifyGru", ACTION_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, notification(
            "workflow-forms.completeFormResponseMarkerProvider", ETAT_COMPLEMENT,
            NOTIF_COMPLEMENT, SIGNET_COMPLEMENT));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskUnitAssignmentNotification", ACTION_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, "taskUnitAssignmentNotification", TaskConfigDataSet.vide()
            .texte("subject", "Complement demande sur un dossier")
            .texte("message", "Un complement a ete demande a l'usager."));
    }

    /**
     * Taches de l'action de demande de correction.
     *
     * @param wf contexte workflow courant
     */
    private static void tachesDeCorrection(WorkflowContext wf) {
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("resubmitFormResponseTypeTask", ACTION_CORRECTION));
        ConfigureTaskMacroTest.run(wf, "resubmitFormResponseTypeTask", TaskConfigDataSet.vide()
            .selection("idStateAfterEdition", ETAT_CORRECTION_RECUE)
            .texte("defaultMessage", MESSAGE_CORRECTION));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskNotifyGru", ACTION_CORRECTION));
        ConfigureTaskMacroTest.run(wf, notification(
            "workflow-forms.resubmitFormResponseMarkerProvider", ETAT_CORRECTION,
            NOTIF_CORRECTION, SIGNET_CORRECTION));

        // Piece jointe non obligatoire : l'exiger imposerait un televersement a chaque passage de
        // l'action, ce qui n'est pas l'objet du controle.
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("taskTypeUpload", ACTION_CORRECTION));
        ConfigureTaskMacroTest.run(wf, "taskTypeUpload", TaskConfigDataSet.vide()
            .texte("title", "Piece a l'appui de la correction")
            .texte("maxFile", "1")
            .texte("maxSizeFile", "1000000")
            .radio("mandatory", "false"));
    }

    /**
     * Parametrage d'une tache de notification adressee a l'usager.
     *
     * <p>Le fournisseur de donnees doit etre enregistre avant que les canaux ne deviennent
     * proposables, et la famille de signets activee dans la configuration avancee avant que le
     * message puisse s'y referer. Les deux canaux sont poses : la vue agent, dont le contenu se
     * retrouve dans l'historique de la reponse et qu'un test peut donc observer, et le courriel,
     * qui porte l'adresse de retour de l'usager.</p>
     *
     * <p><b>Prerequis du site.</b> Les signets d'adresse de retour ne sont calculables que si le
     * site sait quelle est son URL publique. Selon {@code workflow-forms.base_url.use_property},
     * elle est prise dans la requete ou dans {@code lutece.base.url} puis {@code lutece.prod.url}.
     * Sur une instance ou aucune n'est renseignee, le fournisseur de signets echoue a l'execution
     * de l'action — {@code NullPointerException} dans {@code getTaskResourceInfo} — et annule
     * toute la transition sans message a l'ecran.</p>
     *
     * @param famille fournisseur de signets a activer
     * @param statut  statut affiche dans la vue agent, et objet du courriel
     * @param message corps commun aux deux canaux
     * @param signet  signet portant l'adresse de retour, ajoute au seul courriel
     * @return le parametrage correspondant
     */
    private static TaskConfigDataSet notification(String famille, String statut, String message, String signet) {
        return TaskConfigDataSet.vide()
            .selectionLibelle("list_provider", "Forms")
            .enregistrer()
            .avance()
            .case_("marker_providers", famille)
            .appliquer("saveAdvancedConfig")
            .selection("added_notification_config", "agent")
            .appliquer("AddNotificationConfig")
            .texte("status_text_agent", statut)
            .texte("message_agent", message)
            // Le courriel porte l'adresse de retour. Les signets ne sont resolus que dans ce
            // canal : la vue agent affiche un statut et un message, pas un lien a suivre.
            .selection("added_notification_config", "email")
            .appliquer("AddNotificationConfig")
            .texte("sender_name_email", "no-reply")
            .texte("subject_email", statut)
            .texte("message_email", message + " " + signet);
    }

    /**
     * Taches de l'action de cloture.
     *
     * @param wf contexte workflow courant
     */
    private static void tachesDeCloture(WorkflowContext wf) {
        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("modifyUpdateDateTypeTask", ACTION_CLOTURE));
        ConfigureTaskMacroTest.run(wf, "modifyUpdateDateTypeTask", TaskConfigDataSet.vide());
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
    public static void completerAvecLeFormulaire(WorkflowContext wf, FormsContext forms) {
        // Les demandes de correction et de complement ne rouvrent a l'usager que les questions
        // declarees ici. Sans cette declaration, l'ecran d'execution ne propose rien a selectionner
        // et la demande part sans objet.
        ConfigureFormWorkflowQuestionsMacroTest.run(forms,
            FormWorkflowQuestionsDataSet.memesQuestions(Q_NOM, Q_NAISSANCE, Q_NATURE));

        // La notification de l'usager ne lit pas la reponse directement : elle passe par un mapping
        // qui, pour ce formulaire, designe les questions portant ses coordonnees. Sans lui, l'action
        // echoue a l'execution et annule toute la transition.
        CreateNotifygruMappingMacroTest.run(forms);

        AddTaskToActionMacroTest.run(wf, TaskDataSet.sur("editFormResponseTypeTask", ACTION_CLOTURE));
        ConfigureTaskMacroTest.run(wf, "editFormResponseTypeTask", TaskConfigDataSet.vide()
            .selectionLibelle("form_select", forms.formTitle)
            .appliquer("select_form_config"));
    }
}
