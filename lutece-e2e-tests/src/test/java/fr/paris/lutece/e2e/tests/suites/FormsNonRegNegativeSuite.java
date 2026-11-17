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
 * Non-regression Forms — refus : chaque garde-fou du plugin est franchi a tort une fois, et doit refuser
 * sans rien enregistrer : derniere etape finale, question obligatoire vide en FO, suppression d'une question puis
 * d'un groupe portant une reponse, formulaire au nombre maximum de reponses en FO, formulaire depublie en FO,
 * import d'un fichier qui n'est pas du JSON.
 *
 * <p>Le cas d'import d'un JSON sans formulaire (erreur interne constatee le 2026-10-29) n'est pas ici : il est
 * rouge tant que le defaut existe, voir {@code regression.forms.FormsImportJsonWithoutFormRegressionTest}.</p>
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegNegativeSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Refus")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : refus et garde-fous")
public class FormsNonRegNegativeSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Chaque garde-fou refuse sans rien enregistrer")
    void gardeFousForms() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Inscription atelier"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Participant"));
            RefuseUnsetLastFinalStepMacroTest.run(forms, StepTargetDataSet.of(0));

            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nom du participant"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Commune"));
            VerifyQuestionsListedMacroTest.run(forms);
            SetQuestionMandatoryMacroTest.run(forms, QuestionTargetDataSet.of(0));
            CreateGroupMacroTest.run(forms, GroupDataSet.of("Adresse"));
            MoveQuestionMacroTest.run(forms, MoveQuestionDataSet.toGroup(1, 0, 0));
            PublishFormMacroTest.run(forms, PublishDataSet.defaults());

            OpenFormFOMacroTest.run(forms);
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text("Commune", "Paris"));
            RefuseMissingMandatoryFOMacroTest.run(forms, QuestionTargetDataSet.of(0));
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text("Nom du participant", "Martin"));
            ViewSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Commune", "Paris", "Nom du participant", "Martin"));

            RefuseRemoveCompositeWithResponsesMacroTest.run(forms, CompositeTargetDataSet.question(0));
            RefuseRemoveCompositeWithResponsesMacroTest.run(forms, CompositeTargetDataSet.group(0));

            // Une reponse enregistree : limite a 1, nouvelle saisie refusee, puis limite levee (0) et formulaire rouvert
            SetMaxResponsesMacroTest.run(forms, MaxResponsesDataSet.of(1));
            RefuseMaxResponsesReachedFOMacroTest.run(forms);
            SetMaxResponsesMacroTest.run(forms, MaxResponsesDataSet.unlimited());
            OpenFormFOMacroTest.run(forms);

            UnpublishFormMacroTest.run(forms);
            RefuseUnavailableFormFOMacroTest.run(forms);
            RefuseImportInvalidJsonMacroTest.run(forms, InvalidJsonDataSet.malformed());
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
