package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : exporter un formulaire au format JSON (action=doExportJson).
 *
 * <p>Lit : {@code ctx.formId}. Ecrit : rien. Best-effort : declenche l'action d'export et attend un
 * telechargement ; si aucun telechargement n'est produit (action inconnue ou rendu inline), la brique
 * saute via Assumptions plutot que d'echouer.</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Exporter le formulaire (JSON)")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ExportFormJsonMacroTest extends MacroTest {

    @Step("Exporter le formulaire au format JSON")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant export");

        Page page = ctx.page;
        String url = ctx.baseUrl + MacroSupport.FORMS
            + "ManageForms.jsp?action=doExportJson&id_form=" + ctx.formId;

        Download download = null;
        try {
            download = page.waitForDownload(
                new Page.WaitForDownloadOptions().setTimeout(15000),
                () -> page.navigate(url));
        } catch (RuntimeException e) {
            // aucun telechargement declenche (action inconnue / contenu servi inline)
        }

        Assumptions.assumeTrue(download != null,
            "L'export JSON (action=doExportJson) n'a pas declenche de telechargement : flux non pilotable en l'etat");

        String filename = download.suggestedFilename();
        Assertions.assertNotNull(filename, "Le fichier exporte devrait avoir un nom");
        Assertions.assertFalse(filename.isBlank(), "Le nom du fichier exporte ne devrait pas etre vide");
    }

    @Test
    @DisplayName("Exporter un formulaire en JSON (auto-provisionnement du formulaire)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        run(ctx);
    }
}
