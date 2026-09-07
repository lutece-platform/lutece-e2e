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
 * Formulaire simple : une etape, une question texte (ref documentaire : MF-01).
 *
 * <p>Le socle minimal : c'est la suite a lire en premier pour comprendre la composition.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.SimpleFormSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Formulaire simple")
@Tag("macro")
@Tag("suite")
@DisplayName("MF-01 : formulaire simple")
public class SimpleFormSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Formulaire mono-etape avec une question texte")
    void formulaireSimple() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire simple"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        SetStepInitialMacroTest.run(forms, StepTargetDataSet.first());
        SetStepFinalMacroTest.run(forms, StepTargetDataSet.first());
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nom du demandeur"));
    }
}
