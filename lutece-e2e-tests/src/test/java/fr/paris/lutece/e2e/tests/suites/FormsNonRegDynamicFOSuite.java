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
 * Non-regression Forms — comportements dynamiques du front-office sur un meme formulaire : controle de validation
 * (expression reguliere), affichage conditionnel (apparait / disparait), iterations d'un groupe repetable (ajout /
 * retrait), brouillon d'un usager connecte (sauvegarde, absent une fois deconnecte, restaure a la reconnexion).
 *
 * <p>Requalification du 2026-10-29 des briques existantes en skip : les iterations passent en composant
 * {@code MoveQuestion} (au lieu de {@code MoveQuestionIntoGroup}) ; l'affichage conditionnel et le brouillon passent
 * par {@code AddDisplayCondition} / {@code VerifyConditionalRevealFO} et un usager FO MyLutece cree en BO
 * ({@code CreateFrontUserBO}, {@code LoginFrontUserFO}, {@code SaveBackupFO}, {@code VerifyBackupFO}).</p>
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegDynamicFOSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Front-office dynamique")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : validation, conditions, iterations et brouillon en FO")
public class FormsNonRegDynamicFOSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Inscription en centre de loisirs : controle, condition, enfants repetables, brouillon")
    void comportementsDynamiquesFrontOffice() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            CreateFrontUserBOMacroTest.run(forms, FrontUserDataSet.defaults());

            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Inscription centre de loisirs"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Inscription"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Courriel du responsable"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Allergies"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Precisions sur les allergies"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Prenom de l'enfant"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Observations"));
            VerifyQuestionsListedMacroTest.run(forms);
            AddListValueMacroTest.run(forms, ListValueDataSet.create(1, "Oui", "oui"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(1, "Non", "non"));

            AddValidationControlMacroTest.run(forms, ControlDataSet.validation(0, "Email", "Saisie invalide"));
            DisplayConditionDataSet allergies = DisplayConditionDataSet.of(1, 2, "Oui", "Non");
            AddDisplayConditionMacroTest.run(forms, allergies);
            CreateGroupMacroTest.run(forms, GroupDataSet.repeatable("Enfants"));
            MoveQuestionMacroTest.run(forms, MoveQuestionDataSet.toGroup(3, 0, 0));
            ConfigureFormOptionsMacroTest.run(forms, FormOptionsDataSet.defaults());
            PublishFormMacroTest.run(forms, PublishDataSet.defaults());

            VerifyValidationErrorFOMacroTest.run(forms, ValidationCheckDataSet.of("Courriel du responsable",
                "@@@", "parent@example.org", "Saisie invalide"));
            OpenFormFOMacroTest.run(forms);
            VerifyConditionalRevealFOMacroTest.run(forms, allergies);
            AddIterationFOMacroTest.run(forms);
            RemoveIterationFOMacroTest.run(forms);

            BackupValueDataSet observations = BackupValueDataSet.of(4, "Arrivee vers 9 h");
            LoginFrontUserFOMacroTest.run(forms, FrontUserDataSet.defaults());
            OpenFormFOMacroTest.run(forms);
            SaveBackupFOMacroTest.run(forms, observations);
            LogoutFrontUserFOMacroTest.run(forms);
            VerifyBackupFOMacroTest.run(forms, observations.absent());
            LoginFrontUserFOMacroTest.run(forms, FrontUserDataSet.defaults());
            VerifyBackupFOMacroTest.run(forms, observations);
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
