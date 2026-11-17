package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Non-regression Forms 9/9 — affichage conditionnel d'une question de l'etape 2 pour preciser une reponse de l'etape 1.
 *
 * <p>Preparation en BO : etape 1 avec la question obligatoire (bouton radio) « Votre statut ? » et ses trois reponses
 * Contractuel, Fonctionnaire, Autre ; etape 2 avec la question obligatoire (texte court) « Préciser Autre », affichee
 * seulement si « Votre statut ? » vaut Autre. Premier lancement : Autre a l'etape 1, « Préciser Autre » s'affiche et
 * se remplit a l'etape 2, recapitulatif, validation. Second lancement : Fonctionnaire a l'etape 1, aucune question a
 * l'etape 2, le recapitulatif doit s'afficher sans erreur, validation. Chaque reponse est relue dans son detail BO.</p>
 *
 * <p>Cas remonte LUT-34462 (Pre Open) : au second lancement, la question obligatoire masquee bloquait le passage au
 * recapitulatif (l'etape 2 reaffichee sans message, constate le 2026-11-06 sur forms bd4a558f). Corrige upstream par
 * LUT-34348 (a20bf5bb, integre le 2026-11-04) : la visibilite cote serveur tient compte des reponses des autres etapes.
 * Suite verte sur {@code develop} depuis (2026-11-09) : elle garde ce cas en non-regression, rouge sur
 * {@code GoToSummaryFO} s'il revient.</p>
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegCrossStepConditionSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Front-office")
@Issue("LUT-34462")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : question de l'etape 2 affichee selon une reponse de l'etape 1")
public class FormsNonRegCrossStepConditionSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("« Préciser Autre » a l'etape 2 : affichee et remplie si Autre, absente et non bloquante si Fonctionnaire")
    void precisionDuStatutAEtapeSuivante() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            // Preparation en BO
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Statut de l'agent"));
            CreateStepMacroTest.run(forms, StepDataSet.of("Statut"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Precisions"));
            VerifyTransitionTargetsMacroTest.run(forms, TransitionTargetsDataSet.of(0, 1));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Votre statut ?"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Préciser Autre").onStep(1));
            VerifyQuestionsListedMacroTest.run(forms);
            AddListValueMacroTest.run(forms, ListValueDataSet.create(0, "Contractuel", "contractuel"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(0, "Fonctionnaire", "fonctionnaire"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(0, "Autre", "autre"));
            SetQuestionMandatoryMacroTest.run(forms, QuestionTargetDataSet.of(0));
            SetQuestionMandatoryMacroTest.run(forms, QuestionTargetDataSet.of(1));
            AddDisplayConditionMacroTest.run(forms, DisplayConditionDataSet.of(0, 1, "Autre", "Fonctionnaire"));
            PublishFormMacroTest.run(forms, PublishDataSet.defaults());

            // 1er lancement : Autre, la precision s'affiche a l'etape 2
            OpenFormFOMacroTest.run(forms);
            ChooseValueFOMacroTest.run(forms, ChoiceDataSet.of(0, "Autre"));
            GoToNextStepFOMacroTest.run(forms, StepTargetDataSet.of(1));
            VerifyQuestionShownFOMacroTest.run(forms, QuestionShownDataSet.shown(1));
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text("Préciser Autre", "Vacataire"));
            GoToSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Votre statut ?", "Autre", "Préciser Autre", "Vacataire"));

            // 2e lancement : Fonctionnaire, aucune question a l'etape 2, recapitulatif sans erreur (LUT-34462)
            OpenFormFOMacroTest.run(forms);
            ChooseValueFOMacroTest.run(forms, ChoiceDataSet.of(0, "Fonctionnaire"));
            GoToNextStepFOMacroTest.run(forms, StepTargetDataSet.of(1));
            VerifyQuestionShownFOMacroTest.run(forms, QuestionShownDataSet.hidden(1));
            GoToSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Votre statut ?", "Fonctionnaire"));
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
