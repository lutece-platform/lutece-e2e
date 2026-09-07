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
 * Scenario metier : <une phrase, en termes metier>.
 *
 * <p>Enchainement : workflow d'instruction -> formulaire publie -> soumission usager en FO.</p>
 *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.DemandeSubventionSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Demande de subvention")
@Tag("macro")
@Tag("suite")
@DisplayName("Scenario : demande de subvention avec instruction")
public class DemandeSubventionSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Workflow d'instruction + formulaire publie + soumission FO")
    void demandeSubvention() {          // nom parlant, JAMAIS scenario() (cf. Pieges)
        String suffix = newSuffix();    // UN seul suffixe partage par tous les contextes du run
        login();

        // 1) Workflow d'instruction
        WorkflowContext wf = new WorkflowContext(page, BASE_URL, suffix);
        CreateWorkflowMacroTest.run(wf, WorkflowDataSet.defaults().withName("Instruction subvention"));
        AddStateMacroTest.run(wf, StateDataSet.initial("A instruire"));
        AddStateMacroTest.run(wf, StateDataSet.of("Accordee"));
        AddActionMacroTest.run(wf, ActionDataSet.of("Accorder", 0, 1));
        ActivateWorkflowMacroTest.run(wf);

        // 2) Pont workflow -> forms : OBLIGATOIRE, les contextes ne communiquent pas seuls
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        forms.workflowId = wf.workflowId;
        forms.workflowName = wf.workflowName;

        // 3) Formulaire (la 1re etape est forcee initiale+finale a la creation, cote serveur)
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Demande de subvention"));
        CreateStepMacroTest.run(forms, StepDataSet.of("Identite du demandeur"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nom de l'association"));
        AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());

        // 4) Soumission usager en front-office
        OpenFormFOMacroTest.run(forms);
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.text("Nom de l'association", "Les Amis du Parc"));
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
    }
}
