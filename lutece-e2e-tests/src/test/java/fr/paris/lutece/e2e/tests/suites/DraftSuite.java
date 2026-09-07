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
 * Sauvegarde et reprise d'un brouillon (ref documentaire : MFS-02).
 *
 * <p>{@code FormOptionsDataSet.defaults()} active l'option « Sauvegarde » : sans elle, la brique
 *  * de brouillon ne trouve aucun controle en FO et avorte la suite.</p>
 * <p>La brique {@code SaveDraftFO} realise elle-meme l'ouverture FO, la saisie, la sauvegarde et
 *  * la relecture apres rechargement.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.DraftSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Brouillon")
@Tag("macro")
@Tag("suite")
@DisplayName("MFS-02 : brouillon")
public class DraftSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Saisie sauvegardee puis restauree en front-office")
    void brouillon() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire brouillon"));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Question brouillon"));
        ConfigureFormOptionsMacroTest.run(forms, FormOptionsDataSet.defaults());
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
        SaveDraftFOMacroTest.run(forms);
    }
}
