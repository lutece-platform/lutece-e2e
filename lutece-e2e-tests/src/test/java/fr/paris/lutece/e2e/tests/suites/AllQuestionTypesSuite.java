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
 * Tous les types de question ajoutables (ref documentaire : MFQ-ALL).
 *
 * <p>15 types sur 19. {@code COMMENT}, {@code GEOLOCATION}, {@code IMAGE} et
 *  * {@code TERMS_OF_SERVICE} sont exclus : ils exigent une configuration additionnelle (verifie :
 *  * en mode strict, TERMS_OF_SERVICE n'est pas liste apres enregistrement). Leurs briques dediees
 * font un {@code assumeTrue}, ce qui avorterait la suite entiere.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.AllQuestionTypesSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Types de question")
@Tag("macro")
@Tag("suite")
@DisplayName("MFQ-ALL : tous les types de question")
public class AllQuestionTypesSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Les 15 types ajoutables sans configuration externe")
    void tousLesTypesDeQuestion() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Tous types de question"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Question TEXT"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXTAREA, "Question TEXTAREA"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.NUMBER, "Question NUMBER"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.DATE, "Question DATE"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Question RADIO"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.CHECKBOX, "Question CHECKBOX"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT, "Question SELECT"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT_ORDER, "Question SELECT_ORDER"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.FILE, "Question FILE"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.CAMERA, "Question CAMERA"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.NUMBERING, "Question NUMBERING"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TELEPHONE, "Question TELEPHONE"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SLOT, "Question SLOT"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SESSION, "Question SESSION"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Question MYLUTECE_ATTRIBUTE"));
    }
}
