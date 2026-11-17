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
 * Non-regression Forms 3/6 — front-office : publication, parcours usager sur deux etapes, recapitulatif,
 * validation, reponse retrouvee en multivue, puis depublication (formulaire indisponible en FO).
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegFrontOfficeSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Front-office")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : parcours usager en front-office")
public class FormsNonRegFrontOfficeSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Publier, remplir deux etapes, valider, retrouver la reponse, depublier")
    void parcoursUsagerFrontOffice() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Signalement voirie"));
            CreateStepMacroTest.run(forms, StepDataSet.of("Localisation"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Description"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Adresse"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nature du probleme").onStep(1));
            VerifyTransitionTargetsMacroTest.run(forms, TransitionTargetsDataSet.of(0, 1));
            PublishFormMacroTest.run(forms, PublishDataSet.defaults());

            OpenFormFOMacroTest.run(forms);
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text("Adresse", "12 rue de Rivoli"));
            GoToNextStepFOMacroTest.run(forms, StepTargetDataSet.of(1));
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text("Nature du probleme", "Nid de poule"));
            ViewSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Adresse", "12 rue de Rivoli", "Nature du probleme", "Nid de poule"));

            UnpublishFormMacroTest.run(forms);
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
