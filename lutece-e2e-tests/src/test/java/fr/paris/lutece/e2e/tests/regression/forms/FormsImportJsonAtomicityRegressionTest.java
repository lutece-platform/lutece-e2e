package fr.paris.lutece.e2e.tests.regression.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.InvalidJsonDataSet;
import fr.paris.lutece.e2e.tests.macro.forms.RefuseImportInvalidJsonMacroTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cas remonte (LUT-34429, portage v8 de LUT-34387) : un import JSON qui echoue apres l'insertion du formulaire doit etre refuse
 * proprement et ne laisser aucun formulaire. Le JSON {@link InvalidJsonDataSet#formWithoutMessage()} fait inserer le
 * formulaire par {@code FormJsonService.importForm}, puis echouer sur le {@code formMessage} absent.
 *
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.regression.forms.FormsImportJsonAtomicityRegressionTest \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Forms")
@Feature("Cas remontes")
@Story("Import JSON interrompu")
@Issue("LUT-34429")
@Tag("macro")
@Tag("forms")
@Tag("regression")
public class FormsImportJsonAtomicityRegressionTest extends MacroTest {

    @Test
    @DisplayName("Import interrompu apres l'insertion du formulaire : refus propre, aucun formulaire laisse")
    void importJsonFailingAfterFormInsertLeavesNothing() {
        FormsContext ctx = newLoggedInContext();
        try {
            RefuseImportInvalidJsonMacroTest.run(ctx, InvalidJsonDataSet.formWithoutMessage());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
