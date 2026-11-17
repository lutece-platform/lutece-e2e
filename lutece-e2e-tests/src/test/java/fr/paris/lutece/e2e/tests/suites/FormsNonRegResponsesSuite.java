package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.workflow.*;
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
 * Non-regression Forms 6/6 — traitement des reponses : workflow d'instruction associe, reponse FO retrouvee en
 * multivue, detail, action de workflow, export.
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegResponsesSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Reponses")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : traitement des reponses par workflow")
public class FormsNonRegResponsesSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Workflow associe, reponse FO, detail, action de workflow, export")
    void traitementDesReponses() {
        String suffix = newSuffix();
        login();
        WorkflowContext wf = new WorkflowContext(page, BASE_URL, suffix);
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName("Instruction signalement"));
            AddStateMacroTest.run(wf, StateDataSet.initial("A traiter"));
            AddStateMacroTest.run(wf, StateDataSet.of("Traite"));
            AddActionMacroTest.run(wf, ActionDataSet.of("Traiter", 0, 1));
            ActivateWorkflowMacroTest.run(wf);
            forms.workflowId = wf.workflowId;
            forms.workflowName = wf.workflowName;

            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Signalement proprete"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Signalement"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Lieu"));
            AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
            PublishFormMacroTest.run(forms, PublishDataSet.defaults());

            OpenFormFOMacroTest.run(forms);
            FillFieldFOMacroTest.run(forms, FieldValueDataSet.text("Lieu", "Square du Temple"));
            ViewSummaryFOMacroTest.run(forms);
            ValidateSummaryFOMacroTest.run(forms);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Lieu", "Square du Temple"));

            OpenMultiviewMacroTest.run(forms);
            OpenResponseDetailMacroTest.run(forms);
            ProcessWorkflowActionMacroTest.run(forms, WorkflowProcessDataSet.of("Traiter", "Traite"));
            ExportResponsesMacroTest.run(forms, ExportDataSet.csv());
        } finally {
            FormsNonRegSupport.cleanup(forms, wf);
        }
    }
}
