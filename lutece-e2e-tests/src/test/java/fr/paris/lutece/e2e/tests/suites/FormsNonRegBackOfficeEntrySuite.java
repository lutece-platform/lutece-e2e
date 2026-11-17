package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
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
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Non-regression Forms 4/6 — saisie par un agent en back-office (formulaire non publie) : deux reponses
 * saisies sur deux etapes, chacune retrouvee en multivue.
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegBackOfficeEntrySuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Saisie back-office")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : saisie de reponses par un agent")
public class FormsNonRegBackOfficeEntrySuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Saisir deux reponses en BO sur deux etapes et les retrouver en multivue")
    void saisieAgentBackOffice() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Accueil au guichet"));
            CreateStepMacroTest.run(forms, StepDataSet.of("Usager"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Demande"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nom de l'usager"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Objet de la visite").onStep(1));

            for (String[] visite : new String[][] {{"Martin", "Carte d'identite"}, {"Bernard", "Acte de naissance"}}) {
                OpenFormResponseBOMacroTest.run(forms);
                FillFieldBOMacroTest.run(forms, FieldValueDataSet.text("Nom de l'usager", visite[0]));
                GoToNextStepBOMacroTest.run(forms, StepTargetDataSet.of(1));
                FillFieldBOMacroTest.run(forms, FieldValueDataSet.text("Objet de la visite", visite[1]));
                ValidateResponseBOMacroTest.run(forms);
                VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.defaults());
                VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.of("Nom de l'usager", visite[0], "Objet de la visite", visite[1]));
            }
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
