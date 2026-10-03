package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import fr.paris.lutece.e2e.tests.macro.scenarios.FormulaireBranchantScenario;
import fr.paris.lutece.e2e.tests.macro.system.RunDaemonMacroTest;
import fr.paris.lutece.e2e.tests.macro.scenarios.OrganisationScenario;
import fr.paris.lutece.e2e.tests.macro.scenarios.SoumissionFoScenario;
import fr.paris.lutece.e2e.tests.macro.scenarios.WorkflowInstructionScenario;

import static fr.paris.lutece.e2e.tests.macro.scenarios.FormulaireBranchantScenario.*;
import static fr.paris.lutece.e2e.tests.macro.scenarios.OrganisationScenario.UNITE_DIRECTION;
import static fr.paris.lutece.e2e.tests.macro.scenarios.WorkflowInstructionScenario.*;
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
 *   <li><b>Workflow</b> : sept etats et six actions formant un graphe branchant, portant
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

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Organisation, workflow outille, formulaire a embranchement, deux soumissions et instruction")
    void parcoursComplet() {
        String suffix = newSuffix();
        login();

        UnittreeContext units = OrganisationScenario.construire(page, BASE_URL, suffix);
        WorkflowContext wf = WorkflowInstructionScenario.construire(page, BASE_URL, suffix, units);
        FormsContext forms = FormulaireBranchantScenario.construire(page, BASE_URL, suffix);
        WorkflowInstructionScenario.completerAvecLeFormulaire(wf, forms);
        FormulaireBranchantScenario.mettreEnService(forms, wf);

        SoumissionFoScenario.soumettre(forms, CHOIX_SUBVENTION, ETAPE_SUBVENTION, ETAPE_INFORMATION);
        SoumissionFoScenario.soumettre(forms, CHOIX_INFORMATION, ETAPE_INFORMATION, ETAPE_SUBVENTION);
        SoumissionFoScenario.indexer(forms);

        instruction(forms);
        correction(forms);

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, units.units.size(),
                "Les deux unites doivent avoir ete creees"),
            () -> Assertions.assertEquals(7, wf.states.size(),
                "Les sept etats du workflow doivent avoir ete crees"),
            () -> Assertions.assertEquals(6, wf.actions.size(),
                "Les six actions du workflow doivent avoir ete creees"),
            () -> Assertions.assertEquals(4, forms.steps.size(),
                "Les quatre etapes du formulaire doivent avoir ete creees"));
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
        // La demande adressee a l'usager n'a de sens que s'il peut y repondre : l'historique doit
        // porter le lien qui le ramene sur sa reponse en front office.
        VerifyResponseFoLinkMacroTest.run(forms);
    }

    /**
     * Instruction d'une seconde reponse, par la demande de correction de saisie.
     *
     * <p>Les deux demandes se ressemblent mais ne partagent ni leur tache, ni leur etat de sortie,
     * ni les questions qu'elles rouvrent : les eprouver toutes les deux est le seul moyen de
     * verifier qu'elles ont bien ete configurees separement.</p>
     *
     * @param forms contexte formulaire courant
     */
    private void correction(FormsContext forms) {
        // La multivue lit un index, pas la base : sans nouvelle indexation, les reponses y portent
        // encore l'etat qu'elles avaient avant les actions precedentes, et designer un dossier par
        // son etat ouvrirait le mauvais.
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        OpenMultiviewMacroTest.run(forms);
        RunWorkflowActionOnResponseMacroTest.run(forms,
            ResponseActionDataSet.surReponseDansEtat(ACTION_PRISE_EN_CHARGE, ETAT_NOUVELLE));
        // Sans etat impose : la correction porte sur le dossier qu'on vient de prendre en charge.
        RunWorkflowActionOnResponseMacroTest.run(forms,
            ResponseActionDataSet.of(ACTION_CORRECTION));
        VerifyResponseStateMacroTest.run(forms, ResponseStateDataSet.avecTraces(
            ETAT_CORRECTION, ACTION_CORRECTION, MESSAGE_CORRECTION));
        VerifyResponseFoLinkMacroTest.run(forms);
    }
}
