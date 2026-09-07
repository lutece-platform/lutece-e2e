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
 * Formulaire associe a un workflow et publie (ref documentaire : MF-05/06).
 *
 * <p>Ecart assume par rapport a la chaine documentee : une etape et une question sont ajoutees
 *  * avant publication, un formulaire sans etape n'etant pas exploitable en front-office.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormWithWorkflowSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Formulaire + workflow")
@Tag("macro")
@Tag("suite")
@DisplayName("MF-05/06 : formulaire associe a un workflow")
public class FormWithWorkflowSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Workflow actif associe a un formulaire publie")
    void formulaireAvecWorkflow() {
        String suffix = newSuffix();
        login();
        WorkflowContext wf = new WorkflowContext(page, BASE_URL, suffix);
        CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName("Instruction"));
        AddStateMacroTest.run(wf, StateDataSet.initial("A instruire"));
        AddStateMacroTest.run(wf, StateDataSet.of("Traitee"));
        AddActionMacroTest.run(wf, ActionDataSet.of("Traiter", 0, 1));
        ActivateWorkflowMacroTest.run(wf);

        // Pont workflow -> forms
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        forms.workflowId = wf.workflowId;
        forms.workflowName = wf.workflowName;

        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire avec workflow"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Objet de la demande"));
        AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
    }
}
