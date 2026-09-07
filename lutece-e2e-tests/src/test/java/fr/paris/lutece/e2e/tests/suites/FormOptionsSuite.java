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
 * Options d'un formulaire (ref documentaire : MF-04).
 *
 * <p>{@code FormOptionsDataSet.defaults()} active recapitulatif, sauvegarde/brouillon, fil
 *  * d'Ariane et une reponse par utilisateur ; {@code minimal()} les desactive tous.</p> *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormOptionsSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Options")
@Tag("macro")
@Tag("suite")
@DisplayName("MF-04 : options du formulaire")
public class FormOptionsSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Configuration des options de publication et de saisie")
    void optionsDuFormulaire() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Formulaire options"));
        ConfigureFormOptionsMacroTest.run(forms, FormOptionsDataSet.defaults());
    }
}
