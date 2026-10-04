package fr.paris.lutece.e2e.tests.macro.scenarios;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.ConfigureFormWorkflowQuestionsMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.CreateNotifygruMappingMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.*;

/**
 * Traitement des declarations d'interets : transcription du workflow de la commission de deontologie.
 *
 * <p>Fragment de scenario reutilisable, calque sur un workflow reellement en service. Dix etats et
 * treize actions, dont le parcours nominal mene la declaration de son depot jusqu'a son archivage,
 * en passant par la verification du secretariat, l'instruction de la commission, la signature de
 * l'avis et l'envoi au declarant.</p>
 *
 * <p>Deux traits distinguent ce workflow de celui d'une demande ordinaire, et ce sont eux que le
 * scenario eprouve :</p>
 * <ul>
 *   <li><b>Un etat technique de reprise.</b> « Regenerer le PDF » n'est pas une etape du dossier
 *       mais un sas : quatre actions de retour en repartent, une par etat d'ou l'on peut y etre
 *       entre. Regenerer un document depuis n'importe quel etat suppose donc de savoir y revenir,
 *       et c'est la que les workflows se cassent.</li>
 *   <li><b>Deux taches d'edition de reponse en back-office.</b> La commission ne saisit pas son
 *       analyse dans un champ de workflow mais dans une etape du formulaire reservee a l'agent :
 *       l'action ouvre cette etape, et le dossier porte l'analyse comme il porte la declaration.</li>
 * </ul>
 *
 * <p>Les taches qui dependent du formulaire ne sont pas posees par {@link #construire}, qui
 * s'execute avant qu'il n'existe, mais par {@link #completerAvecLeFormulaire}.</p>
 */
public final class WorkflowDeontologieScenario {

    public static final String NOM_WORKFLOW = "Traitement des declarations d'interets";


    public static final String ETAT_DECLARATION_EFFECTUEE = "Declaration effectuee";

    public static final String ETAT_EN_VERIFICATION = "Declaration en cours de verification";

    public static final String ETAT_COMPLEMENTS_DEMANDES = "Complements d'informations demandes";

    public static final String ETAT_COMPLEMENTS_RECUS = "Complements d'informations recus";

    public static final String ETAT_INSTRUCTION_CDVP = "En cours d'instruction CDVP";

    public static final String ETAT_ATTENTE_AVIS = "En attente avis definitif du president";

    public static final String ETAT_ENVOI_DECLARANT = "Envoi au declarant";

    public static final String ETAT_ARCHIVEE = "Declaration archivee";

    public static final String ETAT_SUPPRIMEE = "Declaration supprimee";

    public static final String ETAT_REGENERER_PDF = "Regenerer le PDF";


    public static final String ACTION_DEBUTER_VERIFICATION = "Generer le PDF et debuter la verification";

    public static final String ACTION_DEMANDER_COMPLEMENT = "Demander un complement d'informations";

    public static final String ACTION_REPRENDRE_INSTRUCTION = "Reprendre l'instruction et regenerer le PDF";

    public static final String ACTION_INSTRUCTION_CDVP = "Mettre la declaration en instruction CDVP";

    public static final String ACTION_ENREGISTRER_ANALYSE = "Enregistrer l'analyse et l'avis avant signature";

    public static final String ACTION_ENREGISTRER_AVIS_SIGNE = "Enregistrer l'avis definitif signe";

    public static final String ACTION_ARCHIVER = "A archiver";

    public static final String ACTION_SUPPRIMER = "Supprimer la declaration";

    public static final String ACTION_REGENERER_PDF = "Regenerer le PDF";

    public static final String ACTION_RETOUR_COMPLEMENTS_DEMANDES = "Retour complements demandes";

    public static final String ACTION_RETOUR_COMPLEMENTS_RECUS = "Retour complements recus";

    public static final String ACTION_RETOUR_ATTENTE_AVIS = "Retour attente avis definitif";

    public static final String ACTION_RETOUR_ENVOI_DECLARANT = "Retour envoi au declarant";


    public static final String MESSAGE_COMPLEMENT =
        "Merci de modifier la reponse apportee aux questions ci-dessous";

    public static final String TITRE_COMMENTAIRE_CORRECTION =
        "Preciser la nature des corrections souhaitees";

    public static final String NOTIF_COMPLEMENT = "Demande de complements d'informations";

    public static final String EXPEDITEUR_NOTIFICATION = "CDVP";

    public static final String MESSAGE_NOTIFICATION =
        "Votre declaration est en cours d'instruction par le secretariat de la commission.";

    public static final String SIGNET_CORRECTION = "${resubmit_form_url!}";

    public static final String CANAL_NOTIFICATION = "Agent";

    public static final String TRACE_NOTIFICATION = "Voir les notifications";


    /** Rang des etats dans {@code wf.states}, dans l'ordre de creation. */
    private static final int RANG_DECLARATION_EFFECTUEE = 0;

    private static final int RANG_EN_VERIFICATION = 1;

    private static final int RANG_COMPLEMENTS_DEMANDES = 2;

    private static final int RANG_COMPLEMENTS_RECUS = 3;

    private static final int RANG_INSTRUCTION_CDVP = 4;

    private static final int RANG_ATTENTE_AVIS = 5;

    private static final int RANG_ENVOI_DECLARANT = 6;

    private static final int RANG_ARCHIVEE = 7;

    private static final int RANG_SUPPRIMEE = 8;

    private static final int RANG_REGENERER_PDF = 9;


    private WorkflowDeontologieScenario() {
    }

    /**
     * Construit le workflow : dix etats, treize actions, et les taches qui ne dependent que de lui.
     *
     * <p>Un ecart assume par rapport au workflow d'origine : « Supprimer la declaration » et
     * « Regenerer le PDF » y partent de sept et six etats respectivement. Le back-office cree une
     * action depuis un seul etat de depart, et le scenario ne retient donc que le plus
     * representatif — l'etat de verification, d'ou l'on emprunte reellement ces deux sorties. Ce
     * que le scenario perd, c'est la redondance des origines ; ce qu'il garde, l'etat d'arrivee et
     * les taches, qui sont ce que les actions executent.</p>
     *
     * @param page    page Playwright pilotant le navigateur
     * @param baseUrl adresse du site cible
     * @param suffix  suffixe unique du run
     * @return le contexte workflow alimente
     */
    @Step("Construire le workflow de traitement des declarations")
    public static WorkflowContext construire(Page page, String baseUrl, String suffix) {
        WorkflowContext wf = new WorkflowContext(page, baseUrl, suffix);
        CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName(NOM_WORKFLOW));

        creerLesEtats(wf);
        creerLesActions(wf);
        tachesDeVerification(wf);
        tachesDeDemandeDeComplement(wf);

        ActivateWorkflowMacroTest.run(wf);
        VerifyWorkflowActiveMacroTest.run(wf);
        return wf;
    }

    /**
     * Les dix etats, dans l'ordre du cycle de vie d'une declaration.
     *
     * @param wf contexte workflow courant
     */
    private static void creerLesEtats(WorkflowContext wf) {
        AddStateMacroTest.run(wf, StateDataSet.initial(ETAT_DECLARATION_EFFECTUEE));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_EN_VERIFICATION));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_COMPLEMENTS_DEMANDES));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_COMPLEMENTS_RECUS));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_INSTRUCTION_CDVP));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_ATTENTE_AVIS));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_ENVOI_DECLARANT));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_ARCHIVEE));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_SUPPRIMEE));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_REGENERER_PDF));
    }

    /**
     * Les treize actions : le parcours nominal, les deux sorties transverses, et les quatre retours.
     *
     * <p>Les quatre dernieres repartent toutes de l'etat technique « Regenerer le PDF » : c'est par
     * elles qu'un dossier entre dans ce sas en ressort vers l'etat ou il se trouvait. Les creer est
     * la seule maniere de verifier que ce sas n'est pas un cul-de-sac.</p>
     *
     * @param wf contexte workflow courant
     */
    private static void creerLesActions(WorkflowContext wf) {
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_DEBUTER_VERIFICATION,
            RANG_DECLARATION_EFFECTUEE, RANG_EN_VERIFICATION));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_DEMANDER_COMPLEMENT,
            RANG_EN_VERIFICATION, RANG_COMPLEMENTS_DEMANDES));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_REPRENDRE_INSTRUCTION,
            RANG_COMPLEMENTS_RECUS, RANG_EN_VERIFICATION));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_INSTRUCTION_CDVP,
            RANG_EN_VERIFICATION, RANG_INSTRUCTION_CDVP));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_ENREGISTRER_ANALYSE,
            RANG_INSTRUCTION_CDVP, RANG_ATTENTE_AVIS));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_ENREGISTRER_AVIS_SIGNE,
            RANG_ATTENTE_AVIS, RANG_ENVOI_DECLARANT));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_ARCHIVER,
            RANG_ENVOI_DECLARANT, RANG_ARCHIVEE));

        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_SUPPRIMER,
            RANG_EN_VERIFICATION, RANG_SUPPRIMEE));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_REGENERER_PDF,
            RANG_EN_VERIFICATION, RANG_REGENERER_PDF));

        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_RETOUR_COMPLEMENTS_DEMANDES,
            RANG_REGENERER_PDF, RANG_COMPLEMENTS_DEMANDES));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_RETOUR_COMPLEMENTS_RECUS,
            RANG_REGENERER_PDF, RANG_COMPLEMENTS_RECUS));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_RETOUR_ATTENTE_AVIS,
            RANG_REGENERER_PDF, RANG_ATTENTE_AVIS));
        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_RETOUR_ENVOI_DECLARANT,
            RANG_REGENERER_PDF, RANG_ENVOI_DECLARANT));
    }

    /**
     * Taches de l'action d'entree en verification.
     *
     * <p>Le workflow d'origine y produit l'edition PDF de la declaration. Cette tache appartient au
     * module {@code forms-documentproducer} et exige un modele d'edition pre-existant : elle est
     * remplacee ici par la mise a jour du statut de publication, qui marque de la meme maniere
     * l'entree du dossier dans le circuit et ne suppose rien du site.</p>
     *
     * @param wf contexte workflow courant
     */
    private static void tachesDeVerification(WorkflowContext wf) {
        AddTaskToActionMacroTest.run(wf,
            TaskDataSet.sur("modifyUpdateStatusTask", ACTION_DEBUTER_VERIFICATION));
        ConfigureTaskMacroTest.run(wf, "modifyUpdateStatusTask", TaskConfigDataSet.vide()
            .radio("published", "true"));

        AddTaskToActionMacroTest.run(wf,
            TaskDataSet.sur("modifyUpdateDateTypeTask", ACTION_REPRENDRE_INSTRUCTION));
        ConfigureTaskMacroTest.run(wf, "modifyUpdateDateTypeTask", TaskConfigDataSet.vide());
    }

    /**
     * Les trois taches de la demande de complement, dans leur ordre d'execution.
     *
     * <p>Cet ordre fait partie du comportement : la demande de correction fixe l'etat que prendra
     * la declaration quand l'usager aura repondu, le commentaire recueille ce que l'instructeur
     * veut lui dire, et la notification porte le tout jusqu'a lui. Les permuter enverrait une
     * notification vide.</p>
     *
     * <p>Le workflow d'origine nomme cette action « demande de complement » mais l'arme d'une tache
     * de <i>correction de saisie</i> : l'usager est renvoye sur les questions deja remplies pour
     * les reprendre, et non invite a en remplir de nouvelles. Le scenario reproduit ce choix, qui
     * commande le signet utilisable dans la notification.</p>
     *
     * @param wf contexte workflow courant
     */
    private static void tachesDeDemandeDeComplement(WorkflowContext wf) {
        AddTaskToActionMacroTest.run(wf,
            TaskDataSet.sur("resubmitFormResponseTypeTask", ACTION_DEMANDER_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, "resubmitFormResponseTypeTask", TaskConfigDataSet.vide()
            .selection("idStateAfterEdition", ETAT_COMPLEMENTS_RECUS)
            .texte("defaultMessage", MESSAGE_COMPLEMENT));

        AddTaskToActionMacroTest.run(wf,
            TaskDataSet.sur("taskTypeComment", ACTION_DEMANDER_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, "taskTypeComment", TaskConfigDataSet.vide()
            .texte("title", TITRE_COMMENTAIRE_CORRECTION)
            .radio("mandatory", "false")
            .radio("richText", "true"));

        AddTaskToActionMacroTest.run(wf,
            TaskDataSet.sur("taskNotifyGru", ACTION_DEMANDER_COMPLEMENT));
        ConfigureTaskMacroTest.run(wf, notificationDeComplement());
    }

    /**
     * Parametrage de la notification adressee au declarant.
     *
     * <p>Deux canaux, comme sur le workflow d'origine. La vue agent porte le statut et le message,
     * et c'est elle qu'un test peut observer : son contenu se retrouve dans l'historique de la
     * reponse. Le courriel porte en plus le signet de retour, seul canal ou les signets sont
     * resolus.</p>
     *
     * <p>Le fournisseur de donnees doit etre enregistre avant que les canaux ne deviennent
     * proposables, et la famille de signets activee dans la configuration avancee avant que le
     * message puisse s'y referer.</p>
     *
     * @return le parametrage correspondant
     */
    private static TaskConfigDataSet notificationDeComplement() {
        return TaskConfigDataSet.vide()
            .selectionLibelle("list_provider", "Forms")
            .enregistrer()
            .avance()
            .case_("marker_providers", "workflow-forms.resubmitFormResponseMarkerProvider")
            .case_("marker_providers", "workflow-notifygru.commentMarkerProvider")
            .appliquer("saveAdvancedConfig")
            .selection("added_notification_config", "agent")
            .appliquer("AddNotificationConfig")
            .texte("status_text_agent", NOTIF_COMPLEMENT)
            .texte("message_agent", MESSAGE_NOTIFICATION)
            .selection("added_notification_config", "email")
            .appliquer("AddNotificationConfig")
            .texte("sender_name_email", EXPEDITEUR_NOTIFICATION)
            .texte("subject_email", NOTIF_COMPLEMENT)
            .texte("message_email", MESSAGE_NOTIFICATION + " " + SIGNET_CORRECTION);
    }

    /**
     * Pose les taches qui ne peuvent exister qu'une fois le formulaire cree.
     *
     * <p>Trois dependances, dans cet ordre. Les questions rouvertes a l'usager par une demande de
     * correction doivent etre declarees sur le formulaire, sans quoi l'ecran d'execution de
     * l'action ne propose rien a selectionner et la demande part sans objet. Le mapping NotifyGru
     * doit designer les questions portant les coordonnees du declarant, sans quoi la notification
     * echoue a l'execution et annule toute la transition. Les deux taches d'edition de reponse,
     * enfin, designent le formulaire dont elles ouvriront l'etape d'instruction.</p>
     *
     * @param wf    contexte workflow courant
     * @param forms contexte formulaire, deja construit
     */
    @Step("Completer le workflow avec les taches liees au formulaire")
    public static void completerAvecLeFormulaire(WorkflowContext wf, FormsContext forms) {
        ConfigureFormWorkflowQuestionsMacroTest.run(forms,
            FormWorkflowQuestionsDataSet.memesQuestions(
                DeclarationInteretsScenario.Q_NOM_USAGE,
                DeclarationInteretsScenario.Q_ADRESSE,
                DeclarationInteretsScenario.Q_PROFESSION));

        CreateNotifygruMappingMacroTest.run(forms);

        // La commission saisit son analyse dans l'etape d'instruction du formulaire, pas dans un
        // champ de workflow : l'action ouvre cette etape en back-office et la reponse la porte.
        AddTaskToActionMacroTest.run(wf,
            TaskDataSet.sur("editFormResponseTypeTask", ACTION_ENREGISTRER_ANALYSE));
        ConfigureTaskMacroTest.run(wf, "editFormResponseTypeTask", TaskConfigDataSet.vide()
            .selectionLibelle("form_select", DeclarationInteretsScenario.TITRE_FORMULAIRE));

        // Meme tache sur l'action de signature : c'est la relecture du meme dossier, une fois
        // l'avis du president rendu.
        AddTaskToActionMacroTest.run(wf,
            TaskDataSet.sur("editFormResponseTypeTask", ACTION_ENREGISTRER_AVIS_SIGNE));
        ConfigureTaskMacroTest.run(wf, "editFormResponseTypeTask", TaskConfigDataSet.vide()
            .selectionLibelle("form_select", DeclarationInteretsScenario.TITRE_FORMULAIRE));
    }
}
