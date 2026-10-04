package fr.paris.lutece.e2e.tests.declaratif;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.declaratif.DescriptionDeSuite.*;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import fr.paris.lutece.e2e.tests.macro.system.RunDaemonMacroTest;
import fr.paris.lutece.e2e.tests.macro.unittree.AddUsersToUnitMacroTest;
import fr.paris.lutece.e2e.tests.macro.unittree.CreateUnitMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.*;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * Joue une suite decrite dans un fichier, en s'appuyant sur les briques existantes.
 *
 * <p>Cette classe n'ecrit pas une ligne de Playwright : elle traduit une description en appels de
 * briques, celles-la memes que les suites ecrites a la main emploient. C'est ce qui fait tenir
 * l'ensemble — une correction apportee a une brique profite aux deux familles de suites, et une
 * suite declarative ne peut pas deriver du comportement verifie par les suites de reference.</p>
 *
 * <p>L'ordre des operations n'est pas celui du fichier mais celui qu'impose le back-office : le
 * workflow avant le formulaire puisqu'il faut l'associer, les groupes d'une etape avant ses
 * questions, toutes les questions avant les controles qui les relient, et les liaisons en dernier
 * parce que creer une etape en pose une d'office qu'il faut d'abord effacer.</p>
 */
public final class ExecuteurDeSuite {

    private final Page page;

    private final String urlDeBase;

    private final String suffixe;

    private FormsContext forms;

    private WorkflowContext workflow;

    /** Le formulaire decrit, garde pour savoir quelle etape porte quelle question. */
    private Formulaire formulaireDecrit;

    private UnittreeContext organisation;

    /**
     * @param page      page Playwright pilotant le navigateur
     * @param urlDeBase adresse du site cible
     * @param suffixe   suffixe unique du run, pour des libelles non collisionnants
     */
    public ExecuteurDeSuite(Page page, String urlDeBase, String suffixe) {
        this.page = page;
        this.urlDeBase = urlDeBase;
        this.suffixe = suffixe;
    }

    /**
     * Execute la suite decrite.
     *
     * @param description la suite a jouer
     */
    public void executer(DescriptionDeSuite description) {
        if (description.organisation() != null) {
            batirLOrganisation(description.organisation());
        }
        if (description.workflow() != null) {
            batirLeWorkflow(description.workflow());
        }
        if (description.formulaire() != null) {
            batirLeFormulaire(description.formulaire());
            mettreEnService(description);
        }
        jouerLeParcours(description.parcours());
    }

    /**
     * Cree les entites organisationnelles, et y rattache un agent si la description le demande.
     *
     * <p>Les entites sont creees a la racine plutot qu'imbriquees : la creation sous parent depend
     * d'un identifiant que la liste des unites n'expose pas de facon fiable selon les sites, ce qui
     * rendrait la suite tributaire de l'environnement sans rien apporter — l'affectation d'une
     * ressource a une entite, elle, reste couverte.</p>
     *
     * @param decrite l'organisation decrite
     */
    @Step("Batir l'organisation decrite")
    private void batirLOrganisation(Organisation decrite) {
        organisation = new UnittreeContext(page, urlDeBase, suffixe);
        for (String entite : decrite.entites()) {
            CreateUnitMacroTest.run(organisation, UnitDataSet.of(entite));
        }
        if (decrite.affecterUnAgent()) {
            AddUsersToUnitMacroTest.run(organisation, UserAssignmentDataSet.defaults());
        }
    }

    /** @return le contexte des entites organisationnelles, ou {@code null} si la suite n'en decrit pas */
    public UnittreeContext organisation() {
        return organisation;
    }

    /** @return le contexte formulaire alimente, pour les controles d'une suite appelante */
    public FormsContext formulaire() {
        return forms;
    }

    /** @return le contexte workflow alimente, pour les controles d'une suite appelante */
    public WorkflowContext workflow() {
        return workflow;
    }

    // ---------------------------------------------------------------- workflow

    @Step("Batir le workflow decrit")
    private void batirLeWorkflow(Workflow decrit) {
        workflow = new WorkflowContext(page, urlDeBase, suffixe);
        CreateWorkflowMacroTest.run(workflow, WorkflowDataSet.defaults().withName(decrit.nom()));

        List<String> etats = decrit.etats();
        for (int rang = 0; rang < etats.size(); rang++) {
            // Le premier etat decrit est l'etat initial : c'est celui que prend une reponse des sa
            // soumission, et le seul d'ou peut partir la premiere action d'instruction.
            AddStateMacroTest.run(workflow, rang == 0
                ? StateDataSet.initial(etats.get(rang))
                : StateDataSet.of(etats.get(rang)));
        }

        for (Action action : decrit.actions()) {
            AddActionMacroTest.run(workflow, ActionDataSet.of(action.nom(),
                etats.indexOf(action.de()), etats.indexOf(action.vers())));
        }
        for (Action action : decrit.actions()) {
            for (Tache tache : action.taches()) {
                AddTaskToActionMacroTest.run(workflow, TaskDataSet.sur(tache.cle(), action.nom()));
                ConfigureTaskMacroTest.run(workflow, tache.cle(), reglages(tache));
            }
        }

        ActivateWorkflowMacroTest.run(workflow);
        VerifyWorkflowActiveMacroTest.run(workflow);
    }

    /**
     * Traduit les reglages d'une tache en saisies sur son formulaire de configuration.
     *
     * <p>La nature du champ n'est pas decrite, et ne se devine pas depuis la valeur : « Complements
     * recus » peut aussi bien designer une option de liste qu'un texte libre. C'est l'ecran de la
     * tache qui tranche, au moment de poser le reglage — seul endroit ou l'information existe.
     * Demander a l'auteur du fichier de distinguer « texte », « selection » et « radio »
     * l'obligerait a ouvrir le formulaire de chaque type de tache pour le renseigner.</p>
     *
     * @param tache la tache decrite
     * @return le parametrage a appliquer
     */
    private static TaskConfigDataSet reglages(Tache tache) {
        TaskConfigDataSet config = TaskConfigDataSet.vide();
        for (var reglage : tache.reglages().entrySet()) {
            config = config.auto(reglage.getKey(), reglage.getValue());
        }
        return config;
    }

    // -------------------------------------------------------------- formulaire

    @Step("Batir le formulaire decrit")
    private void batirLeFormulaire(Formulaire decrit) {
        formulaireDecrit = decrit;
        forms = new FormsContext(page, urlDeBase, suffixe);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle(decrit.titre()));

        List<Etape> etapes = decrit.etapes();
        for (Etape etape : etapes) {
            CreateStepMacroTest.run(forms, StepDataSet.of(etape.titre()));
        }
        // Le back-office marque d'office la premiere etape creee comme initiale et la derniere comme
        // finale. Reposer les deux marques explicitement evite qu'une etape hors parcours, creee en
        // dernier, n'herite d'un caractere final qui revient a une autre.
        marquer(etapes, Etape::initiale, SetStepInitialMacroTest::run);
        marquer(etapes, Etape::finale, SetStepFinalMacroTest::run);
        for (int rang = 0; rang < etapes.size(); rang++) {
            if (!etapes.get(rang).finale()) {
                UnsetStepFinalMacroTest.run(forms, StepTargetDataSet.of(rang));
            }
        }

        for (int rang = 0; rang < etapes.size(); rang++) {
            Etape etape = etapes.get(rang);
            for (Groupe groupe : etape.groupes()) {
                CreateGroupMacroTest.run(forms, groupe.iterations() > 1
                    ? GroupDataSet.repeatable(groupe.titre(), groupe.iterations()).onStep(rang)
                    : GroupDataSet.of(groupe.titre()).onStep(rang));
            }
            for (Question question : etape.questions()) {
                AddQuestionMacroTest.run(forms,
                    QuestionDataSet.of(question.type(), question.titre()).onStep(rang));
                if (!question.choix().isEmpty()) {
                    AddQuestionChoicesMacroTest.run(forms, QuestionChoicesDataSet.of(
                        question.titre(), question.choix().toArray(new String[0])));
                }
            }
            rangerDansLesGroupes(etape);
        }

        poserLesControles(etapes);
        poserLesValidations(etapes);
        enchainer(decrit, etapes);

        if (decrit.options() != null) {
            appliquerLesOptions(decrit.options());
        }
    }

    /**
     * Applique les options du formulaire.
     *
     * <p>Elles sont posees apres les etapes et les questions : la page de modification du
     * formulaire porte aussi l'association au workflow, et y revenir plus tard l'ecraserait.</p>
     *
     * @param options les options decrites
     */
    @Step("Appliquer les options du formulaire")
    private void appliquerLesOptions(Options options) {
        ConfigureFormOptionsMacroTest.run(forms, new FormOptionsDataSet(
            options.disponibleDu(), options.disponibleAu(), options.messageIndisponible(),
            options.reponsesMax(), options.uneReponseParUsager(), options.recapitulatif(),
            options.brouillon(), options.filAriane(), options.authentification(), null, null));
    }

    /**
     * Pose les regles de validation decrites sur les questions.
     *
     * @param etapes les etapes du formulaire
     */
    private void poserLesValidations(List<Etape> etapes) {
        for (Etape etape : etapes) {
            for (Question question : etape.questions()) {
                if (question.validation() == null) {
                    continue;
                }
                AddValidationControlMacroTest.run(forms, ControlDataSet.validation(
                    rangDeLaQuestion(question.titre()),
                    question.validation().regle(),
                    question.validation().message()));
            }
        }
    }

    private void marquer(List<Etape> etapes, java.util.function.Predicate<Etape> critere,
        java.util.function.BiConsumer<FormsContext, StepTargetDataSet> brique) {

        for (int rang = 0; rang < etapes.size(); rang++) {
            if (critere.test(etapes.get(rang))) {
                brique.accept(forms, StepTargetDataSet.of(rang));
            }
        }
    }

    /**
     * Fait entrer dans leur groupe les questions qui en designent un.
     *
     * <p>Une question creee sur une etape se pose a sa racine ; c'est le deplacement qui la fait
     * entrer dans un groupe. L'etape compte pour un groupe repetable, ou seule l'appartenance rend
     * une question iterable.</p>
     *
     * @param etape l'etape traitee
     */
    private void rangerDansLesGroupes(Etape etape) {
        for (Question question : etape.questions()) {
            if (question.groupe() == null) {
                continue;
            }
            MoveQuestionIntoGroupMacroTest.run(forms, GroupTargetDataSet.of(
                rangDuGroupe(question.groupe()), rangDeLaQuestion(question.titre())));
        }
    }

    /**
     * Pose les controles d'affichage conditionnel, une fois toutes les questions creees.
     *
     * <p>Un controle relie une question pilote a une question cible : les deux doivent exister, et
     * elles peuvent tenir sur des etapes differentes.</p>
     *
     * @param etapes les etapes du formulaire
     */
    private void poserLesControles(List<Etape> etapes) {
        for (Etape etape : etapes) {
            for (Question question : etape.questions()) {
                Condition condition = question.afficheeSi();
                if (condition == null) {
                    continue;
                }
                AddConditionalControlMacroTest.run(forms, ControlDataSet.conditionalSurListe(
                    rangDeLaQuestion(condition.question()),
                    rangDeLaQuestion(question.titre()),
                    condition.vaut()));
            }
        }
    }

    /**
     * Pose l'enchainement voulu, apres avoir efface celui que le back-office a cree de lui-meme.
     *
     * <p>Creer une etape la relie a la precedente. Poser les liaisons voulues par-dessus donnerait
     * un graphe hybride ou la liaison implicite, prioritaire, l'emporterait.</p>
     *
     * @param decrit le formulaire decrit
     * @param etapes ses etapes
     */
    private void enchainer(Formulaire decrit, List<Etape> etapes) {
        for (int rang = 0; rang < etapes.size(); rang++) {
            ClearStepTransitionsMacroTest.run(forms, StepTargetDataSet.of(rang));
        }
        if (decrit.enchainement() != null) {
            for (Liaison liaison : decrit.enchainement()) {
                CreateTransitionMacroTest.run(forms, TransitionDataSet.of(
                    rangDeLEtape(etapes, liaison.de()), rangDeLEtape(etapes, liaison.vers())));
            }
            // Les conditions sont posees apres toutes les liaisons : l'ecran de controle d'une
            // transition se construit a partir de celles qui existent deja.
            for (Liaison liaison : decrit.enchainement()) {
                if (liaison.si() != null) {
                    AddTransitionControlMacroTest.run(forms, TransitionControlDataSet.valeurChoisie(
                        rangDeLEtape(etapes, liaison.de()), rangDeLEtape(etapes, liaison.vers()),
                        rangDeLaQuestion(liaison.si().question()), liaison.si().vaut()));
                }
            }
            return;
        }
        // Enchainement lineaire : chaque etape du parcours mene a la suivante. Les etapes hors
        // parcours en sont exclues, de part et d'autre.
        List<Integer> rangs = new java.util.ArrayList<>();
        for (int rang = 0; rang < etapes.size(); rang++) {
            if (!etapes.get(rang).horsParcours()) {
                rangs.add(rang);
            }
        }
        for (int i = 0; i + 1 < rangs.size(); i++) {
            CreateTransitionMacroTest.run(forms, TransitionDataSet.of(rangs.get(i), rangs.get(i + 1)));
        }
    }

    @Step("Associer le workflow et publier le formulaire")
    private void mettreEnService(DescriptionDeSuite description) {
        Formulaire decrit = description.formulaire();
        if (!decrit.questionsRouvertes().isEmpty()) {
            // Sans cette declaration, l'ecran d'execution d'une demande de correction ou de
            // complement ne propose aucune question a selectionner, et la demande part sans objet.
            ConfigureFormWorkflowQuestionsMacroTest.run(forms,
                FormWorkflowQuestionsDataSet.memesQuestions(
                    decrit.questionsRouvertes().toArray(new String[0])));
        }
        if (decrit.mappingNotification()) {
            // La notification de l'usager ne lit pas la reponse directement : elle passe par un
            // mapping qui designe les questions portant ses coordonnees.
            CreateNotifygruMappingMacroTest.run(forms);
        }
        if (description.workflow() != null) {
            forms.workflowId = workflow.workflowId;
            forms.workflowName = workflow.workflowName;
            AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(workflow.workflowName));
        }
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
    }

    // ---------------------------------------------------------------- parcours

    private void jouerLeParcours(List<EtapeDeParcours> parcours) {
        for (EtapeDeParcours etape : parcours) {
            if (etape instanceof Soumission soumission) {
                soumettre(soumission);
            } else if (etape instanceof Instruction instruction) {
                instruire(instruction);
            } else if (etape instanceof Daemon daemon) {
                RunDaemonMacroTest.run(forms, DaemonDataSet.of(daemon.cle()));
            } else if (etape instanceof Export export) {
                ExportResponsesMacroTest.run(forms,
                    "pdf".equals(export.format()) ? ExportDataSet.pdf() : ExportDataSet.csv());
            }
        }
    }

    /**
     * Joue une soumission en front office, du premier ecran jusqu'a la validation du recapitulatif.
     *
     * <p>Le fichier enumere les reponses du parcours entier sans dire sur quel ecran chacune se
     * pose : l'auteur decrit une demarche, pas une suite de pages. Chaque reponse est donc proposee
     * a chaque etape et retenue la ou sa question figure. Ce qu'une telle tolerance pourrait
     * masquer — une reponse qui ne trouve jamais sa question, pour une faute de frappe dans le
     * libelle — est rattrape a la fin : une reponse non placee arrete la soumission.</p>
     *
     * <p>Les controles de visibilite, eux, ne sont pas tolerants : ils portent sur une etape
     * precise, celle que la description assigne a leur question. Les verifier partout rendrait
     * « masquee » vrai sur toutes les etapes qui ne portent pas la question, et le controle ne
     * prouverait plus rien.</p>
     *
     * @param soumission la soumission decrite
     */
    @Step("Soumettre une reponse en front office")
    private void soumettre(Soumission soumission) {
        OpenFormFOMacroTest.run(forms);
        java.util.Set<String> placees = new java.util.LinkedHashSet<>();
        int ecran = 0;

        while (true) {
            for (var reponse : soumission.reponses().entrySet()) {
                if (SelectChoiceFOMacroTest.siPresente(forms,
                        ChoiceSelectionDataSet.of(reponse.getKey(), reponse.getValue()))) {
                    placees.add(reponse.getKey());
                }
            }
            for (Saisie saisie : soumission.valeurs()) {
                if (FillFieldFOMacroTest.siPresent(forms, new FieldValueDataSet(
                        saisie.question(), saisie.valeur(), saisie.nature()))) {
                    placees.add(saisie.question());
                }
            }
            for (ControleDeVisibilite controle : soumission.controles()) {
                if (ecran == etapeDeLaQuestion(controle.question())) {
                    VerifyQuestionVisibilityFOMacroTest.run(forms, new QuestionVisibilityDataSet(
                        controle.question(), controle.visible()));
                }
            }
            for (SaisieRefusee refus : soumission.refus()) {
                if (forms.page.getByText(refus.question()).count() > 0) {
                    VerifyValidationErrorFOMacroTest.run(forms, ValidationCheckDataSet.of(
                        refus.question(), refus.valeur(), refus.accepte(), refus.message()));
                }
            }
            controlerLesEtapes(soumission, ecran);
            FillAllFieldsFOMacroTest.run(forms);
            itererSiDemande(soumission, ecran);
            if (!NextStepFOMacroTest.estDisponible(forms)) {
                break;
            }
            NextStepFOMacroTest.run(forms);
            ecran++;
        }

        java.util.Set<String> orphelines = new java.util.LinkedHashSet<>(soumission.reponses().keySet());
        soumission.valeurs().forEach(saisie -> orphelines.add(saisie.question()));
        orphelines.removeAll(placees);
        Assertions.assertTrue(orphelines.isEmpty(),
            "Ces reponses n'ont trouve leur question sur aucune etape du parcours : " + orphelines
            + ". Le libelle differe de celui declare dans la section « formulaire », ou l'etape qui "
            + "porte la question n'est pas atteinte par l'enchainement decrit.");

        if (soumission.brouillon()) {
            SaveDraftFOMacroTest.run(forms);
        }
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
        // La multivue lit l'index Lucene et non la base : sans passage de l'indexeur, une reponse
        // bien enregistree y reste invisible.
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        VerifyResponseSubmittedMacroTest.run(forms);
    }

    /**
     * Constate l'etape atteinte, et celle qui ne l'a pas ete.
     *
     * <p>Les deux vont ensemble pour un formulaire a embranchement : verifier seulement l'etape
     * atteinte laisserait passer une condition inoperante, le parcours suivant alors simplement la
     * premiere liaison sans que rien ne le signale.</p>
     *
     * @param soumission la soumission decrite
     */
    private void controlerLesEtapes(Soumission soumission, int ecran) {
        for (String attendue : soumission.etapesAttendues()) {
            // L'etape n'est constatee que la ou elle doit s'afficher : l'exiger partout ferait
            // echouer toutes les autres.
            if (ecran == rangDEcran(attendue)) {
                VerifyStepFOMacroTest.run(forms, attendue);
            }
        }
        for (String ecartee : soumission.etapesEcartees()) {
            // Une etape ecartee ne doit s'afficher nulle part : le constat vaut a chaque ecran.
            VerifyStepFOMacroTest.absente(forms, ecartee);
        }
    }

    /**
     * Ajoute puis retire une iteration, sur les etapes que la description designe.
     *
     * <p>Ajouter sans retirer laisserait passer une regression sur la suppression d'iteration, qui
     * est la moitie du mecanisme et la plus fragile : elle manipule un bloc deja rempli.</p>
     *
     * @param soumission la soumission decrite
     * @param ecran      rang de l'etape affichee
     */
    private void itererSiDemande(Soumission soumission, int ecran) {
        for (String etape : soumission.iterations()) {
            if (ecran == rangDEcran(etape)) {
                AddIterationFOMacroTest.run(forms);
                RemoveIterationFOMacroTest.run(forms);
            }
        }
    }

    /**
     * Rang d'ecran d'une etape du parcours, les etapes hors parcours ne comptant pas.
     *
     * @param titre titre de l'etape
     * @return son rang, ou -1 si elle n'est pas dans le parcours
     */
    private int rangDEcran(String titre) {
        int ecran = 0;
        for (Etape etape : formulaireDecrit.etapes()) {
            if (etape.horsParcours()) {
                continue;
            }
            if (etape.titre().equals(titre)) {
                return ecran;
            }
            ecran++;
        }
        return -1;
    }

    /**
     * Rang, dans le parcours de l'usager, de l'etape qui porte une question.
     *
     * <p>Les etapes hors parcours ne comptent pas : elles ne sont jamais affichees en front
     * office, et leur presence decalerait tous les rangs suivants.</p>
     *
     * @param titre libelle de la question
     * @return son rang d'ecran, ou -1 si la question n'est sur aucune etape du parcours
     */
    private int etapeDeLaQuestion(String titre) {
        int ecran = 0;
        for (Etape etape : formulaireDecrit.etapes()) {
            if (etape.horsParcours()) {
                continue;
            }
            if (etape.questions().stream().anyMatch(q -> q.titre().equals(titre))) {
                return ecran;
            }
            ecran++;
        }
        return -1;
    }

    @Step("Executer une action de workflow sur la reponse")
    private void instruire(Instruction instruction) {
        OpenMultiviewMacroTest.run(forms);
        RunWorkflowActionOnResponseMacroTest.run(forms, instruction.surEtat() == null
            ? ResponseActionDataSet.of(instruction.action())
            : ResponseActionDataSet.surReponseDansEtat(instruction.action(), instruction.surEtat()));

        if (instruction.etatAttendu() != null) {
            VerifyResponseStateMacroTest.run(forms, ResponseStateDataSet.avecTraces(
                instruction.etatAttendu(), instruction.action(),
                instruction.traces().toArray(new String[0])));
        }
        if (instruction.notification() != null) {
            VerifyNotificationMacroTest.run(forms, NotificationDataSet.of(
                instruction.notification().canal(), instruction.notification().message()));
        }
        if (instruction.lienFrontOffice()) {
            // Une demande adressee a l'usager n'a de sens que s'il peut y repondre : l'historique
            // doit porter le lien qui le ramene sur sa reponse.
            VerifyResponseFoLinkMacroTest.run(forms);
        }
    }

    // ------------------------------------------------------------- resolution

    private int rangDeLaQuestion(String titre) {
        for (int rang = 0; rang < forms.questions.size(); rang++) {
            if (titre.equals(forms.questions.get(rang).title)) {
                return rang;
            }
        }
        throw new IllegalStateException("Question « " + titre + " » absente du contexte");
    }

    private int rangDuGroupe(String titre) {
        for (int rang = 0; rang < forms.groups.size(); rang++) {
            // Les briques suffixent le titre d'un groupe, que l'auteur du fichier ne connait pas.
            if (forms.groups.get(rang).title.startsWith(titre)) {
                return rang;
            }
        }
        throw new IllegalStateException("Groupe « " + titre + " » absent du contexte");
    }

    private static int rangDeLEtape(List<Etape> etapes, String titre) {
        for (int rang = 0; rang < etapes.size(); rang++) {
            if (titre.equals(etapes.get(rang).titre())) {
                return rang;
            }
        }
        throw new IllegalStateException("Etape « " + titre + " » absente de la description");
    }
}
