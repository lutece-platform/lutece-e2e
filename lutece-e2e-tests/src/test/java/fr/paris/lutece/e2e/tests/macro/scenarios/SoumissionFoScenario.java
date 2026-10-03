package fr.paris.lutece.e2e.tests.macro.scenarios;

import io.qameta.allure.Step;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import fr.paris.lutece.e2e.tests.macro.system.RunDaemonMacroTest;
import org.junit.jupiter.api.Assertions;

import static fr.paris.lutece.e2e.tests.macro.scenarios.FormulaireBranchantScenario.*;

/**
 * Soumission d'une reponse en front office, en empruntant l'une des branches du formulaire.
 *
 * <p>Fragment de scenario reutilisable : une suite qui veut plusieurs dossiers, ou des dossiers
 * dans des etats differents, appelle {@link #soumettre} autant de fois que necessaire.</p>
 */
public final class SoumissionFoScenario {

    private SoumissionFoScenario() {
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
    @Step("Soumettre une reponse en front office")
    public static void soumettre(FormsContext forms, String choix, String etapeAttendue, String etapeEcartee) {
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
    private static void remplirEtape(FormsContext forms, String etape) {
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
    @Step("Indexer les reponses soumises")
    public static void indexer(FormsContext forms) {
        RunDaemonMacroTest.run(forms, DaemonDataSet.formsIndexer());
        VerifyResponseSubmittedMacroTest.run(forms);
    }
}
