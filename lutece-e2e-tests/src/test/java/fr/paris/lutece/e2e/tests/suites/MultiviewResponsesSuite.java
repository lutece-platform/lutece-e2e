package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import fr.paris.lutece.e2e.tests.macro.workflow.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Traitement des reponses en back-office (ref documentaire : MFR-01/02/03).
 *
 * <p>Le workflow est indispensable : sans action workflow associee, aucune action n'est
 *  * declenchable sur la reponse.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.MultiviewResponsesSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Reponses")
@Tag("macro")
@Tag("suite")
@DisplayName("MFR-01/02/03 : multivue des reponses")
public class MultiviewResponsesSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Reponse soumise, ouverte, traitee et exportee")
    void traitementDesReponses() {
        String suffix = newSuffix();
        login();
        WorkflowContext wf = new WorkflowContext(page, BASE_URL, suffix);
        CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName("Traitement reponses"));
        AddStateMacroTest.run(wf, StateDataSet.initial("Recue"));
        AddStateMacroTest.run(wf, StateDataSet.of("Traitee"));
        AddActionMacroTest.run(wf, ActionDataSet.of("Traiter", 0, 1));
        ActivateWorkflowMacroTest.run(wf);

        // Pont workflow -> forms
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        forms.workflowId = wf.workflowId;
        forms.workflowName = wf.workflowName;

        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire reponses"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());

        // Une reponse est necessaire : sans elle la multivue est vide et les briques avortent.
        // On la produit avec les briques FO publiques plutot qu'avec un helper interne au package.
        OpenFormFOMacroTest.run(forms);
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.of("Question texte", "Reponse metier"));
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);

        OpenMultiviewMacroTest.run(forms);
        OpenResponseDetailMacroTest.run(forms);
        RunWorkflowActionOnResponseMacroTest.run(forms, ResponseActionDataSet.defaults());
        ExportResponsesMacroTest.run(forms, ExportDataSet.csv());
    }
}
