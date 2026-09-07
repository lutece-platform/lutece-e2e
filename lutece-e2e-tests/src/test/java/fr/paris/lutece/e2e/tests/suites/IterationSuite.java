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
 * Groupe repetable et iteration en front-office (ref documentaire : MFS-03).
 *
 * <p>L'ordre compte : la question doit etre creee puis deplacee dans le groupe repetable, sinon
 *  * le FO n'affiche aucun bloc d'iteration et la brique avorte la suite.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.IterationSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Iterations")
@Tag("macro")
@Tag("suite")
@DisplayName("MFS-03 : iteration")
public class IterationSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Ajout d'une iteration sur un groupe repetable")
    void iteration() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire iterable"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Question iterable"));
        CreateGroupMacroTest.run(forms, GroupDataSet.repeatable("Groupe repetable"));
        MoveQuestionIntoGroupMacroTest.run(forms, GroupTargetDataSet.of(0, 0));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
        AddIterationFOMacroTest.run(forms);
    }
}
