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
 * Controle de validation en front-office (ref documentaire : MFC-02).
 *
 * <p>Verifie qu'une valeur invalide declenche bien le message de contrainte en front-office.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.ValidationControlSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Controles")
@Tag("macro")
@Tag("suite")
@DisplayName("MFC-02 : controle de validation")
public class ValidationControlSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Message d'erreur attendu sur une saisie invalide")
    void controleDeValidation() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Controle de validation"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Question a valider"));
        AddValidationControlMacroTest.run(forms, ControlDataSet.validationDefaults());
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
        VerifyValidationErrorFOMacroTest.run(forms, ValidationCheckDataSet.defaults());
    }
}
