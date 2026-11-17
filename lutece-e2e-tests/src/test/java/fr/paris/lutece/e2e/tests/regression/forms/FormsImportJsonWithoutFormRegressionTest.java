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
 * Cas remonte (LUT-34429, point C de {@code docs/TESTS-MACRO-FORMS-NONREG.md}) : importer un JSON valide
 * qui ne contient pas de cle {@code form} provoque une {@code NullPointerException}
 * ({@code FormJsonService.importForm}, {@code form.getTitle()}) et la page « Internal error », au lieu du refus
 * propre « Erreur lors de l'import du formulaire ».
 *
 * <p>Constate le 2026-10-29 sur forms upstream {@code develop} bd4a558f. Rouge tant que le defaut existe. Les PR v7
 * #689 (LUT-34387) et #690 (LUT-34403) ne le corrigent pas : {@code doImportJson} n'attrape pas l'{@code AppException}
 * qu'elles relancent. Le portage v8 LUT-34429 (branche {@code LUT-34429}, 2026-10-30) l'attrape : vert avec ce
 * correctif sur le banc.</p>
 *
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.regression.forms.FormsImportJsonWithoutFormRegressionTest \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Forms")
@Feature("Cas remontes")
@Story("Import JSON sans formulaire")
@Issue("LUT-34429")
@Tag("macro")
@Tag("forms")
@Tag("regression")
public class FormsImportJsonWithoutFormRegressionTest extends MacroTest {

    @Test
    @DisplayName("Import d'un JSON sans cle form : refus propre, pas d'erreur interne")
    void importJsonWithoutFormIsRefusedCleanly() {
        FormsContext ctx = newLoggedInContext();
        try {
            RefuseImportInvalidJsonMacroTest.run(ctx, InvalidJsonDataSet.withoutForm());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
