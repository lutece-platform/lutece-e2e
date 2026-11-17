package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Non-regression Forms 10/10 — affichage conditionnel et controle de validation sur la question de l'etape 2 qui
 * precise une reponse de l'etape 1.
 *
 * <p>Reprend {@link FormsNonRegCrossStepConditionSuite} (LUT-34462) et pose un controle de validation sur la question
 * conditionnelle « Préciser Autre » : expression reguliere « Email » (seule expression utilisable du banc avec
 * « Fichier JPG », releve du 2026-11-12), message « Saisie invalide ».</p>
 *
 * <ol>
 *   <li>Autre : la precision s'affiche a l'etape 2, une saisie invalide est refusee avec le message du controle, une
 *       saisie valide passe ; reponse relue en BO.</li>
 *   <li>Fonctionnaire : la precision est masquee, le controle ne s'applique pas a une question vide, recapitulatif sans
 *       erreur ; reponse relue en BO, sans precision.</li>
 *   <li>Changement d'avis : Autre, saisie invalide a l'etape 2, retour a l'etape 1 par le fil des etapes,
 *       Fonctionnaire ; a l'etape 2 la precision est masquee. Attendu : recapitulatif sans erreur et reponse sans la
 *       saisie abandonnee.</li>
 * </ol>
 *
 * <p>Constat du 2026-11-12 (forms {@code develop} af1d2196 = upstream 84afcdbd + LUT-34491/34494/34500) : le 3e
 * lancement est bloque sur l'etape 2. La question masquee garde la saisie invalide, le controle s'y applique et
 * l'etape est reaffichee avec « Saisie invalide » sur un champ invisible : l'usager ne voit aucun message. Cause lue
 * dans {@code FormsResponseUtils.fillResponseManagerWithResponses} : la seconde boucle applique les controles de
 * validation a toutes les reponses de l'etape sans tenir compte de {@code question.isVisible()}. Defaut couvert par
 * LUT-34074 : correctif fusionne sur {@code develop_4.0.2-beta-04.b} seulement, PR #677 ({@code develop}) ouverte au
 * 2026-11-12. Suite rouge sur {@code GoToSummaryFO} tant que cette PR n'est pas fusionnee.</p>
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegCrossStepControlSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Front-office")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : controle sur la question de l'etape 2 affichee selon une reponse de l'etape 1")
public class FormsNonRegCrossStepControlSuite extends MacroTest {

    private static final String PRECISION = "Préciser Autre";
    private static final String MESSAGE = "Saisie invalide";

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("« Préciser Autre » controlee : refus si invalide, ignoree si masquee, meme apres un changement d'avis")
    void controleDeLaPrecisionDuStatut() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            // Preparation en BO : le formulaire de LUT-34462, plus un controle sur la question conditionnelle
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Statut de l'agent"));
            CreateStepMacroTest.run(forms, StepDataSet.of("Statut"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Precisions"));
            VerifyTransitionTargetsMacroTest.run(forms, TransitionTargetsDataSet.of(0, 1));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Votre statut ?"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, PRECISION).onStep(1));
            VerifyQuestionsListedMacroTest.run(forms);
            AddListValueMacroTest.run(forms, ListValueDataSet.create(0, "Contractuel", "contractuel"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(0, "Fonctionnaire", "fonctionnaire"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(0, "Autre", "autre"));
            SetQuestionMandatoryMacroTest.run(forms, QuestionTargetDataSet.of(0));
            SetQuestionMandatoryMacroTest.run(forms, QuestionTargetDataSet.of(1));
            AddDisplayConditionMacroTest.run(forms, DisplayConditionDataSet.of(0, 1, "Autre", "Fonctionnaire"));
            AddValidationControlMacroTest.run(forms, ControlDataSet.validation(1, "Email", MESSAGE));
            PublishFormMacroTest.run(forms, PublishDataSet.defaults());

            // 1er lancement : Autre, saisie invalide refusee, saisie valide acceptee
            OpenFormFOMacroTest.run(forms);
            ChooseValueFOMacroTest.run(forms, ChoiceDataSet.of(0, "Autre"));
            GoToNextStepFOMacroTest.run(forms, StepTargetDataSet.of(1));
            VerifyQuestionShownFOMacroTest.run(forms, QuestionShownDataSet.shown(1));
            RefuseInvalidValueFOMacroTest.run(forms, InvalidValueDataSet.of(1, "@@@", MESSAGE));
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text(PRECISION, "agent@example.org"));
            GoToSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Votre statut ?", "Autre", PRECISION, "agent@example.org"));

            // 2e lancement : Fonctionnaire, precision masquee et vide, le controle ne bloque pas
            OpenFormFOMacroTest.run(forms);
            ChooseValueFOMacroTest.run(forms, ChoiceDataSet.of(0, "Fonctionnaire"));
            GoToNextStepFOMacroTest.run(forms, StepTargetDataSet.of(1));
            VerifyQuestionShownFOMacroTest.run(forms, QuestionShownDataSet.hidden(1));
            GoToSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Votre statut ?", "Fonctionnaire").without(PRECISION));

            // 3e lancement : changement d'avis apres une saisie invalide, la precision masquee ne doit plus bloquer
            OpenFormFOMacroTest.run(forms);
            ChooseValueFOMacroTest.run(forms, ChoiceDataSet.of(0, "Autre"));
            GoToNextStepFOMacroTest.run(forms, StepTargetDataSet.of(1));
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text(PRECISION, "@@@"));
            GoToStepFOMacroTest.run(forms, StepTargetDataSet.of(0));
            ChooseValueFOMacroTest.run(forms, ChoiceDataSet.of(0, "Fonctionnaire"));
            GoToNextStepFOMacroTest.run(forms, StepTargetDataSet.of(1));
            VerifyQuestionShownFOMacroTest.run(forms, QuestionShownDataSet.hidden(1));
            GoToSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Votre statut ?", "Fonctionnaire").without(PRECISION));
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
