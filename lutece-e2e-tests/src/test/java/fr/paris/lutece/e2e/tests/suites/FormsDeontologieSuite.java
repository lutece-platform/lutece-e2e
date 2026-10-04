package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.Evidence;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import fr.paris.lutece.e2e.tests.macro.scenarios.DeclarationInteretsScenario;
import fr.paris.lutece.e2e.tests.macro.scenarios.WorkflowDeontologieScenario;
import fr.paris.lutece.e2e.tests.macro.system.RunDaemonMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.VerifyWorkflowActiveMacroTest;

import static fr.paris.lutece.e2e.tests.macro.scenarios.DeclarationInteretsScenario.*;
import static fr.paris.lutece.e2e.tests.macro.scenarios.WorkflowDeontologieScenario.*;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Declaration d'interets simplifiee : le formulaire et son workflow, de la configuration a
 * l'instruction.
 *
 * <p>Cette suite transcrit une demarche reellement en service — onze etapes, sept groupes
 * repetables, trente et un controles d'affichage conditionnel, dix etats de workflow et treize
 * actions — et la fait fonctionner de bout en bout. Elle ne se contente pas de reconstruire la
 * configuration : elle la soumet, puis l'instruit.</p>
 *
 * <p>Ce qu'elle surveille, et qu'un formulaire plus simple ne montrerait pas :</p>
 * <ol>
 *   <li><b>L'affichage conditionnel a l'echelle.</b> Sept rubriques declaratives partagent le meme
 *       motif — une question fermee commande cinq questions de detail. La suite soumet une
 *       declaration qui repond « Non » partout, puis verifie qu'aucun detail ne s'affiche, et une
 *       seconde qui repond « Oui » sur une rubrique, pour verifier que le detail apparait bien.
 *       Une regression sur les controles conditionnels se voit alors sur sept rubriques, pas sur
 *       un cas isole.</li>
 *   <li><b>Les groupes repetables.</b> Chaque rubrique autorise jusqu'a dix declarations. La suite
 *       en ajoute puis en retire une, la ou la plupart des formulaires de test n'en declarent
 *       jamais plus d'une.</li>
 *   <li><b>L'etape reservee a l'agent.</b> L'etape d'instruction n'est atteinte par aucune
 *       liaison : seule une tache d'edition de reponse l'ouvre. La suite verifie qu'elle reste
 *       hors du parcours de l'usager, puis que l'instructeur y accede par l'action de workflow.</li>
 *   <li><b>Le sas de regeneration.</b> L'etat technique « Regenerer le PDF » n'a d'interet que si
 *       l'on en ressort. La suite y entre depuis la verification et en revient, ce qu'aucun
 *       parcours nominal ne ferait.</li>
 * </ol>
 *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsDeontologieSuite \
 *     -Dlutece.base.url=https://mon-site/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Declaration d'interets")
@Tag("macro")
@Tag("suite")
@DisplayName("Declaration d'interets : onze etapes, sept rubriques conditionnelles, workflow a dix etats")
public class FormsDeontologieSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Reconstruit la declaration d'interets simplifiee et son workflow de traitement, "
        + "puis les fait fonctionner : deux soumissions front-office eprouvant les deux faces de "
        + "l'affichage conditionnel, l'iteration d'un groupe repetable, puis l'instruction du "
        + "dossier par le secretariat jusqu'a l'envoi au declarant.")
    @DisplayName("Configuration, soumissions conditionnelles et instruction d'une declaration")
    void declarationInterets() {
        Evidence.contexteDExecution(BASE_URL);

        String suffix = newSuffix();
        login();

        WorkflowContext wf = WorkflowDeontologieScenario.construire(page, BASE_URL, suffix);
        FormsContext forms = DeclarationInteretsScenario.construire(page, BASE_URL, suffix);
        WorkflowDeontologieScenario.completerAvecLeFormulaire(wf, forms);
        DeclarationInteretsScenario.mettreEnService(forms, wf);

        verifierLaConfiguration(forms, wf);
        declarationSansRubrique(forms);
        declarationAvecRubriques(forms);
        instruireLaDeclaration(forms);
        eprouverLeSasDeRegeneration(forms);

        Assertions.assertAll(
            () -> Assertions.assertEquals(11, forms.steps.size(),
                "Les onze etapes du formulaire doivent avoir ete creees"),
            () -> Assertions.assertEquals(10, wf.states.size(),
                "Les dix etats du workflow doivent avoir ete crees"),
            () -> Assertions.assertEquals(13, wf.actions.size(),
                "Les treize actions du workflow doivent avoir ete creees"));
    }

    /**
     * Controles de configuration, avant toute soumission.
     *
     * <p>Verifier la configuration juste apres l'avoir posee peut sembler redondant — les briques
     * de creation assertent deja. Mais elles assertent chacune sur son ecran de creation, juste
     * apres l'enregistrement ; ce que l'on verifie ici, c'est que l'ensemble tient une fois
     * relu : la hierarchie des groupes, l'enchainement des etapes, et le fait que l'etape
     * d'instruction soit bien restee hors du parcours.</p>
     *
     * @param forms contexte formulaire courant
     * @param wf    contexte workflow courant
     */
    @Step("Verifier la configuration du formulaire et du workflow")
    private void verifierLaConfiguration(FormsContext forms, WorkflowContext wf) {
        VerifyGroupHierarchyMacroTest.run(forms, DeclarationInteretsScenario.imbricationConnue(forms));

        // L'enchainement du parcours usager : chaque etape mene a la suivante, et la derniere
        // rubrique declarative mene a la validation.
        VerifyTransitionMacroTest.run(forms, TransitionDataSet.of(0, 1));
        VerifyTransitionMacroTest.run(forms, TransitionDataSet.of(8, 9));

        VerifyWorkflowActiveMacroTest.run(wf);
    }

    /**
     * Premiere soumission : le declarant n'a rien a declarer.
     *
     * <p>C'est le parcours le plus frequent, et celui qui eprouve la face « masquee » des trente et
     * un controles : repondre « Non » a chaque question pivot doit laisser toutes les questions de
     * detail hors de l'ecran. Un controle mal pose se verrait ici par l'apparition d'un champ qui
     * ne devrait pas etre la — ou par un champ obligatoire invisible qui bloque la validation.</p>
     *
     * @param forms contexte formulaire courant
     */
    @Step("Soumettre une declaration sans rubrique a declarer")
    private void declarationSansRubrique(FormsContext forms) {
        OpenFormFOMacroTest.run(forms);
        VerifyStepFOMacroTest.run(forms, ETAPE_AVANT_DEMARRER);
        FillAllFieldsFOMacroTest.run(forms);
        NextStepFOMacroTest.run(forms);

        VerifyStepFOMacroTest.run(forms, ETAPE_RENSEIGNEMENTS);
        FillAllFieldsFOMacroTest.run(forms);
        NextStepFOMacroTest.run(forms);

        for (String pivot : DeclarationInteretsScenario.pivots()) {
            SelectChoiceFOMacroTest.run(forms, ChoiceSelectionDataSet.of(pivot, CHOIX_NON));
            VerifyQuestionVisibilityFOMacroTest.run(forms,
                QuestionVisibilityDataSet.masquee(cibleDe(pivot)));
            FillAllFieldsFOMacroTest.run(forms);
            NextStepFOMacroTest.run(forms);
        }

        VerifyStepFOMacroTest.run(forms, ETAPE_VALIDATION);
        FillAllFieldsFOMacroTest.run(forms);
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
        // La multivue lit un index, pas la base : sans passage de l'indexeur, elle reste vide alors
        // que la reponse est bien enregistree, et la preuve de soumission echouerait a tort.
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        VerifyResponseSubmittedMacroTest.run(forms);
    }

    /**
     * Seconde soumission : le declarant a des activites professionnelles a declarer, et plusieurs.
     *
     * <p>Elle eprouve la face « affichee » des controles, puis l'iteration : le groupe repetable
     * accepte jusqu'a dix declarations, on en ajoute une seconde et on la retire. Ajouter sans
     * retirer laisserait passer une regression sur la suppression d'iteration, qui est la moitie
     * du mecanisme et la plus fragile — elle manipule un bloc deja rempli.</p>
     *
     * @param forms contexte formulaire courant
     */
    @Step("Soumettre une declaration avec rubriques et iterations")
    private void declarationAvecRubriques(FormsContext forms) {
        OpenFormFOMacroTest.run(forms);
        FillAllFieldsFOMacroTest.run(forms);
        NextStepFOMacroTest.run(forms);
        FillAllFieldsFOMacroTest.run(forms);
        NextStepFOMacroTest.run(forms);

        // Rubrique « activites professionnelles » : on declare, donc le detail doit apparaitre.
        VerifyStepFOMacroTest.run(forms, ETAPE_ACTIVITES_PRO);
        SelectChoiceFOMacroTest.run(forms, ChoiceSelectionDataSet.of(Q_PIVOT_ACTIVITES_PRO, CHOIX_OUI));
        VerifyQuestionVisibilityFOMacroTest.run(forms,
            QuestionVisibilityDataSet.affichee(cibleDe(Q_PIVOT_ACTIVITES_PRO)));
        FillAllFieldsFOMacroTest.run(forms);
        AddIterationFOMacroTest.run(forms);
        RemoveIterationFOMacroTest.run(forms);
        NextStepFOMacroTest.run(forms);

        // Les rubriques suivantes ne sont pas declarees : le parcours doit les traverser sans
        // reclamer de champ obligatoire masque.
        for (String pivot : new String[] { Q_PIVOT_ORGANES, Q_PIVOT_CAPITAL, Q_PIVOT_BENEVOLAT,
            Q_PIVOT_MANDATS, Q_PIVOT_CONSULTANT, Q_PIVOT_CONJOINT }) {
            SelectChoiceFOMacroTest.run(forms, ChoiceSelectionDataSet.of(pivot, CHOIX_NON));
            FillAllFieldsFOMacroTest.run(forms);
            NextStepFOMacroTest.run(forms);
        }

        VerifyStepFOMacroTest.run(forms, ETAPE_VALIDATION);
        FillAllFieldsFOMacroTest.run(forms);
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        VerifyResponseSubmittedMacroTest.run(forms);
    }

    /**
     * Instruction par le secretariat : verification, demande de complement, puis circuit CDVP.
     *
     * <p>Chaque action est suivie du controle de ce qu'elle a produit — l'etat de la reponse, la
     * trace de ses taches dans l'historique, et le contenu de la notification adressee au
     * declarant. Une action peut s'executer sans erreur visible et n'avoir rien fait : c'est le
     * mode de defaillance que ces controles attrapent.</p>
     *
     * @param forms contexte formulaire courant
     */
    @Step("Instruire la declaration jusqu'a l'envoi au declarant")
    private void instruireLaDeclaration(FormsContext forms) {
        OpenMultiviewMacroTest.run(forms);
        OpenResponseDetailMacroTest.run(forms);
        VerifyResponseStateMacroTest.run(forms,
            ResponseStateDataSet.sansHistorique(ETAT_DECLARATION_EFFECTUEE));

        RunWorkflowActionOnResponseMacroTest.run(forms,
            ResponseActionDataSet.of(ACTION_DEBUTER_VERIFICATION));
        VerifyResponseStateMacroTest.run(forms,
            ResponseStateDataSet.avecTraces(ETAT_EN_VERIFICATION, ACTION_DEBUTER_VERIFICATION));

        // La demande de complement porte les trois taches : correction de saisie, commentaire et
        // notification. L'historique doit porter la trace des trois, et le declarant recevoir le
        // lien qui le ramene sur sa reponse.
        RunWorkflowActionOnResponseMacroTest.run(forms,
            ResponseActionDataSet.of(ACTION_DEMANDER_COMPLEMENT));
        VerifyResponseStateMacroTest.run(forms, ResponseStateDataSet.avecTraces(
            ETAT_COMPLEMENTS_DEMANDES, ACTION_DEMANDER_COMPLEMENT, TRACE_NOTIFICATION));
        VerifyNotificationMacroTest.run(forms,
            NotificationDataSet.of(CANAL_NOTIFICATION, MESSAGE_NOTIFICATION));
        VerifyResponseFoLinkMacroTest.run(forms);
    }

    /**
     * Entree dans le sas de regeneration, et retour.
     *
     * <p>Le second dossier sert ici : il est encore dans l'etat de verification, d'ou part l'action
     * de regeneration. On y entre puis on en revient par l'action de retour correspondante. Un sas
     * sans sortie laisserait le dossier bloque, et c'est precisement ce qu'un parcours nominal ne
     * montre jamais.</p>
     *
     * @param forms contexte formulaire courant
     */
    @Step("Entrer dans le sas de regeneration du PDF et en revenir")
    private void eprouverLeSasDeRegeneration(FormsContext forms) {
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        OpenMultiviewMacroTest.run(forms);
        RunWorkflowActionOnResponseMacroTest.run(forms,
            ResponseActionDataSet.surReponseDansEtat(ACTION_DEBUTER_VERIFICATION,
                ETAT_DECLARATION_EFFECTUEE));
        VerifyResponseStateMacroTest.run(forms,
            ResponseStateDataSet.avecTraces(ETAT_EN_VERIFICATION, ACTION_DEBUTER_VERIFICATION));

        RunWorkflowActionOnResponseMacroTest.run(forms,
            ResponseActionDataSet.of(ACTION_REGENERER_PDF));
        VerifyResponseStateMacroTest.run(forms,
            ResponseStateDataSet.avecTraces(ETAT_REGENERER_PDF, ACTION_REGENERER_PDF));

        RunWorkflowActionOnResponseMacroTest.run(forms,
            ResponseActionDataSet.of(ACTION_RETOUR_COMPLEMENTS_DEMANDES));
        VerifyResponseStateMacroTest.run(forms, ResponseStateDataSet.avecTraces(
            ETAT_COMPLEMENTS_DEMANDES, ACTION_RETOUR_COMPLEMENTS_DEMANDES));
    }
}
