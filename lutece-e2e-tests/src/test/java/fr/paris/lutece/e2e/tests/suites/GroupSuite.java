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
 * Groupe de questions et hierarchie (ref documentaire : MF-03).
 *
 * <p>Verifie que la question deplacee est bien rattachee au groupe.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.GroupSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Groupes")
@Tag("macro")
@Tag("suite")
@DisplayName("MF-03 : groupe de questions")
public class GroupSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Deux questions dont une deplacee dans un groupe")
    void groupeDeQuestions() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire avec groupe"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        CreateGroupMacroTest.run(forms, GroupDataSet.of("Coordonnees"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Adresse"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Ville"));
        MoveQuestionIntoGroupMacroTest.run(forms, GroupTargetDataSet.of(0, 0));
        VerifyGroupHierarchyMacroTest.run(forms, GroupTargetDataSet.of(0, 0));
    }
}
