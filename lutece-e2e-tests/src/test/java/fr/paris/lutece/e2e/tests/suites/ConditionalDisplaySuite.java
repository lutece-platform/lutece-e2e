package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
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

/**
 * Affichage conditionnel en front-office (ref documentaire : MFC-01).
 *
 * <p>La revelation cote FO est sensible a la version du plugin forms : cette suite peut se
 *  * terminer en {@code skipped} sur certaines images. Un skip signifie ici « scenario non joue ».</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.ConditionalDisplaySuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Controles")
@Tag("macro")
@Tag("suite")
@DisplayName("MFC-01 : affichage conditionnel")
public class ConditionalDisplaySuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Une question revelee par la valeur d'une autre")
    void affichageConditionnel() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Affichage conditionnel"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Question pilote"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Question cible"));
        AddConditionalControlMacroTest.run(forms, ControlDataSet.conditionalDefaults());
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
        VerifyConditionalDisplayFOMacroTest.run(forms, ConditionalCheckDataSet.defaults());
    }
}
