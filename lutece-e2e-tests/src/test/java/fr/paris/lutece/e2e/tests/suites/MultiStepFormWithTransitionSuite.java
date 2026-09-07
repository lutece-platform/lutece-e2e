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
 * Formulaire multi-etapes avec transition (ref documentaire : MF-02).
 *
 * <p>{@code VerifyTransition} n'est pas enchainee : sur ce markup Forms la brique ne sait pas
 * scanner manageTransitions et s'auto-ignore, ce qui avorterait la suite ; la creation est de
 * toute facon deja assertee par {@code CreateTransition}.</p>
 *
 * <p>Couvre la structure multi-etapes en back-office. La navigation FO correspondante
 *  * (bouton « Etape suivante ») n'est pas couverte ici : voir la note de TypedFieldSubmissionSuite.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.MultiStepFormWithTransitionSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Formulaire multi-etapes")
@Tag("macro")
@Tag("suite")
@DisplayName("MF-02 : formulaire multi-etapes avec transition")
public class MultiStepFormWithTransitionSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Deux etapes reliees par une transition verifiee")
    void formulaireMultiEtapes() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire multi-etapes"));
        CreateStepMacroTest.run(forms, StepDataSet.of("Identite"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Recapitulatif"));
        SetStepInitialMacroTest.run(forms, StepTargetDataSet.of(0));
        SetStepFinalMacroTest.run(forms, StepTargetDataSet.of(1));
        CreateTransitionMacroTest.run(forms, TransitionDataSet.of(0, 1));
    }
}
