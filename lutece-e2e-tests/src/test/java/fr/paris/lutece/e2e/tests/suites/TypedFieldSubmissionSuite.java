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
 * Soumission front-office d'un champ type (ref documentaire : MFQ-{type}).
 *
 * <p>Changer {@code QuestionType.TEXT} et le {@code FieldValueDataSet} pour couvrir un autre type.</p>
 * <p>{@code NextStepFO} de la chaine documentee est absent volontairement : sur un formulaire
 *  * mono-etape la brique fait un {@code assumeTrue} (« bouton absent ») qui avorterait la suite.
 *  * L'exercer demanderait une brique capable de retirer l'indicateur « finale » de la 1re etape,
 *  * que le serveur force a la creation — cette brique n'existe pas aujourd'hui.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.TypedFieldSubmissionSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Soumission FO")
@Tag("macro")
@Tag("suite")
@DisplayName("MFQ : soumission FO d'un champ type")
public class TypedFieldSubmissionSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Saisie, recapitulatif et validation en front-office")
    void soumissionChampType() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Soumission champ texte"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Champ FO"));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(forms);
        FillFieldFOMacroTest.run(forms, FieldValueDataSet.of("Champ FO", "Valeur metier"));
        ViewSummaryFOMacroTest.run(forms);
        ValidateSummaryFOMacroTest.run(forms);
    }
}
