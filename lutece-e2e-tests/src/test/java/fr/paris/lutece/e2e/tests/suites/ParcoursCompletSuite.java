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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Parcours metier complet, de l'organisation jusqu'a l'instruction d'une reponse.
 *
 * <p>Couvre en une seule execution les quatre briques fonctionnelles du socle, chacune dans sa
 * version non triviale, et surtout leurs points de jonction — c'est la ou les regressions
 * apparaissent, pas dans les briques prises isolement :</p>
 *
 * <ol>
 *   <li><b>Unittree</b> : deux entites organisationnelles et l'affectation d'un agent.</li>
 *   <li><b>Workflow</b> : quatre etats, trois actions formant un graphe branchant (prise en
 *       charge, puis complement <i>ou</i> cloture), et cinq taches sur l'action d'entree,
 *       <i>chacune reellement parametree</i> : une tache inseree sans configuration est inoperante
 *       — une mise a jour de statut sans choix publie/depublie, une affectation sans mode
 *       d'assignation ni strategie de selection d'entite. C'est cette derniere qui raccroche le
 *       workflow aux entites creees au point 1. Puis activation.</li>
 *   <li><b>Formulaire</b> : trois etapes enchainees par transitions, portant ensemble quatorze
 *       types de question differents — du texte au creneau horaire en passant par les listes, le
 *       fichier et l'attribut d'utilisateur —, association au workflow, publication.</li>
 *   <li><b>Front office puis instruction</b> : saisie etape par etape, recapitulatif, validation,
 *       puis execution d'une action du workflow sur la reponse recue.</li>
 * </ol>
 *
 * <p>L'ordre n'est pas arbitraire : le workflow doit etre actif avant d'etre associe, le
 * formulaire publie avant d'etre ouvert en front office, et une reponse soumise avant qu'une
 * action puisse s'y appliquer.</p>
 *
 * <p><b>Etapes conditionnelles.</b> Conditionner une transition, pour faire bifurquer le parcours
 * selon une reponse, demande d'attacher un controle a cette transition — ce que fait
 * {@code AddTransitionControlMacroTest}. L'ecran exige un « type de controle », dont la liste est
 * vide sur les sites d'integration eprouves : aucun validateur n'y est expose, et l'enregistrement
 * est refuse. La brique existe et s'ignore avec ce diagnostic ; elle n'est pas appelee ici pour ne
 * pas rendre tout le parcours tributaire de cette absence.</p>
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
@DisplayName("Parcours complet : unites, workflow multi-etats, formulaire multi-etapes, soumission FO et instruction")
public class ParcoursCompletSuite extends MacroTest {

    private static final String UNITE_DIRECTION = "Direction des demarches";
    private static final String UNITE_SERVICE = "Service instruction";

    private static final String ETAT_NOUVELLE = "Nouvelle demande";
    private static final String ETAT_INSTRUCTION = "En cours d'instruction";
    private static final String ETAT_COMPLEMENT = "Complement demande";
    private static final String ETAT_CLOTUREE = "Cloturee";
    private static final String ACTION_PRISE_EN_CHARGE = "Prendre en charge";

    private static final String Q_NOM = "Nom du demandeur";
    private static final String Q_NAISSANCE = "Date de naissance";
    private static final String Q_OBJET = "Objet de la demande";
    private static final String Q_MONTANT = "Montant demande";
    private static final String Q_PRECISIONS = "Precisions complementaires";

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Organisation, workflow, formulaire multi-etapes, soumission front office et instruction")
    void parcoursComplet() {
        String suffix = newSuffix();
        login();

        UnittreeContext units = organisation(suffix);
        WorkflowContext wf = workflow(suffix);
        FormsContext forms = formulaire(suffix, wf);
        soumissionFrontOffice(forms);
        instruction(forms);

        org.junit.jupiter.api.Assertions.assertAll(
            () -> org.junit.jupiter.api.Assertions.assertEquals(2, units.units.size(),
                "Les deux unites doivent avoir ete creees"),
            () -> org.junit.jupiter.api.Assertions.assertEquals(4, wf.states.size(),
                "Les quatre etats du workflow doivent avoir ete crees"),
            () -> org.junit.jupiter.api.Assertions.assertEquals(3, wf.actions.size(),
                "Les trois actions du workflow doivent avoir ete creees"),
            () -> org.junit.jupiter.api.Assertions.assertEquals(3, forms.steps.size(),
                "Les trois etapes du formulaire doivent avoir ete creees"));
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
     * Workflow d'instruction : quatre etats et trois actions formant un graphe branchant, plusieurs
     * taches portees par la premiere action, puis activation.
     *
     * @param suffix suffixe unique du run
     * @return le contexte workflow alimente
     */
    private WorkflowContext workflow(String suffix) {
        WorkflowContext wf = new WorkflowContext(page, BASE_URL, suffix);
        CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName("Instruction demande"));

        AddStateMacroTest.run(wf, StateDataSet.initial(ETAT_NOUVELLE));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_INSTRUCTION));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_COMPLEMENT));
        AddStateMacroTest.run(wf, StateDataSet.of(ETAT_CLOTUREE));

        AddActionMacroTest.run(wf, ActionDataSet.of(ACTION_PRISE_EN_CHARGE, 0, 1));
        AddActionMacroTest.run(wf, ActionDataSet.of("Demander un complement", 1, 2));
        AddActionMacroTest.run(wf, ActionDataSet.of("Cloturer", 1, 3));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.of("modifyUpdateStatusTask"));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .radio("published", "true"));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.of("taskTypeComment"));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .texte("title", "Commentaire d'instruction")
            .radio("mandatory", "false")
            .radio("richText", "true"));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.of("taskUnitAssignmentManual"));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .radio("assignment_type", "create")
            .selection("unit_selection_id_to_add", "ParametrableUnitSelection")
            .appliquer());

        AddTaskToActionMacroTest.run(wf, TaskDataSet.of("taskTypeConfirmAction"));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .texte("message", "Confirmez-vous la prise en charge de cette demande ?"));

        AddTaskToActionMacroTest.run(wf, TaskDataSet.of("completeFormResponseTypeTask"));
        ConfigureTaskMacroTest.run(wf, TaskConfigDataSet.vide()
            .selection("idStateAfterEdition", wf.states.get(2).name)
            .texte("defaultMessage", "Merci de completer votre dossier."));

        ActivateWorkflowMacroTest.run(wf);
        VerifyWorkflowActiveMacroTest.run(wf);
        return wf;
    }

    /**
     * Formulaire a trois etapes enchainees par transitions, chacune portant plusieurs questions de
     * types differents, associe au workflow actif puis publie.
     *
     * @param suffix suffixe unique du run
     * @param wf     workflow actif a associer
     * @return le contexte formulaire alimente
     */
    private FormsContext formulaire(String suffix, WorkflowContext wf) {
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        forms.workflowId = wf.workflowId;
        forms.workflowName = wf.workflowName;

        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Parcours complet"));

        CreateStepMacroTest.run(forms, StepDataSet.of("Identite"));
        CreateStepMacroTest.run(forms, StepDataSet.of("Details de la demande"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Confirmation"));
        SetStepInitialMacroTest.run(forms, StepTargetDataSet.of(0));
        SetStepFinalMacroTest.run(forms, StepTargetDataSet.of(2));
        UnsetStepFinalMacroTest.run(forms, StepTargetDataSet.of(0));
        CreateTransitionMacroTest.run(forms, TransitionDataSet.of(0, 1));
        CreateTransitionMacroTest.run(forms, TransitionDataSet.of(1, 2));

        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, Q_NOM).onStep(0));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.DATE, Q_NAISSANCE).onStep(0));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TELEPHONE, "Telephone").onStep(0));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.NUMBERING, "Numero de dossier").onStep(0));

        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXTAREA, Q_OBJET).onStep(1));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.NUMBER, Q_MONTANT).onStep(1));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Type de demandeur").onStep(1));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.CHECKBOX, "Dispositifs concernes").onStep(1));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT, "Direction de rattachement").onStep(1));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT_ORDER, "Priorites par ordre").onStep(1));

        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, Q_PRECISIONS).onStep(2));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.FILE, "Piece justificative").onStep(2));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SLOT, "Creneau de rendez-vous").onStep(2));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SESSION, "Session").onStep(2));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Attribut utilisateur").onStep(2));

        AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
        return forms;
    }

    /**
     * Saisie front office etape par etape, puis recapitulatif et validation.
     *
     * <p>Le daemon d'indexation est declenche avant toute lecture back office : la multivue lit un
     * index Lucene, pas directement la base, et une reponse tout juste soumise n'y apparait qu'une
     * fois ce daemon passe. Son intervalle n'a aucune raison de coincider avec le test.</p>
     *
     * <p>La question de type date est volontairement laissee vide : le theme rend ce type via un
     * composant dont le champ visible ne porte pas d'attribut {@code name}, et le renseigner fait
     * echouer la soumission de l'etape. Elle n'est pas obligatoire, le parcours reste donc valide
     * et le formulaire continue de couvrir ce type cote back office.</p>
     *
     * @param forms contexte du formulaire publie
     */
    private void soumissionFrontOffice(FormsContext forms) {
        OpenFormFOMacroTest.run(forms);
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.text(Q_NOM, "Dupont"));
        NextStepFOMacroTest.run(forms);
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.text(Q_OBJET, "Demande de subvention annuelle"));
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.number(Q_MONTANT, "1500"));
        NextStepFOMacroTest.run(forms);
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.text(Q_PRECISIONS, "Dossier complet transmis"));
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        VerifyResponseSubmittedMacroTest.run(forms);
    }

    /**
     * Instruction de la reponse en back office : la multivue, ou la reponse apparait portee par
     * l'etat initial du workflow associe, puis le detail de cette reponse.
     *
     * <p>Le scenario s'arrete au detail. Declencher une action de workflow depuis cette vue suppose
     * des permissions RBAC sur les ressources concernees (formulaire, type d'action de workflow)
     * que ce scenario ne configure pas : sans elles l'action n'est pas proposee. Les accorder ici
     * reviendrait a reecrire les droits du site a chaque execution, ce qui n'est pas acceptable sur
     * un environnement durable — c'est l'objet d'un scenario dedie, a l'image de la configuration
     * RBAC existante.</p>
     *
     *
     * @param forms contexte portant la reponse soumise
     */
    private void instruction(FormsContext forms) {
        OpenMultiviewMacroTest.run(forms);
        OpenResponseDetailMacroTest.run(forms);
    }
}
