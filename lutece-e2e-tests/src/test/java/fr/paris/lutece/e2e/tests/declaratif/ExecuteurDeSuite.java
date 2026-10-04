package fr.paris.lutece.e2e.tests.declaratif;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.declaratif.DescriptionDeSuite.*;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import fr.paris.lutece.e2e.tests.macro.system.RunDaemonMacroTest;
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
        if (description.workflow() != null) {
            batirLeWorkflow(description.workflow());
        }
        if (description.formulaire() != null) {
            batirLeFormulaire(description.formulaire());
            mettreEnService(description);
        }
        jouerLeParcours(description.parcours());
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
        enchainer(decrit, etapes);
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
            for (ControleDeVisibilite controle : soumission.controles()) {
                if (ecran == etapeDeLaQuestion(controle.question())) {
                    VerifyQuestionVisibilityFOMacroTest.run(forms, new QuestionVisibilityDataSet(
                        controle.question(), controle.visible()));
                }
            }
            FillAllFieldsFOMacroTest.run(forms);
            if (!NextStepFOMacroTest.estDisponible(forms)) {
                break;
            }
            NextStepFOMacroTest.run(forms);
            ecran++;
        }

        java.util.Set<String> orphelines = new java.util.LinkedHashSet<>(soumission.reponses().keySet());
        orphelines.removeAll(placees);
        Assertions.assertTrue(orphelines.isEmpty(),
            "Ces reponses n'ont trouve leur question sur aucune etape du parcours : " + orphelines
            + ". Le libelle differe de celui declare dans la section « formulaire », ou l'etape qui "
            + "porte la question n'est pas atteinte par l'enchainement decrit.");

        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
        // La multivue lit l'index Lucene et non la base : sans passage de l'indexeur, une reponse
        // bien enregistree y reste invisible.
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        VerifyResponseSubmittedMacroTest.run(forms);
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
