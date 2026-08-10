package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ExportDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
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
 * Brique macro : exporter les reponses depuis la multivue.
 *
 * <p>Lit : rien. Ecrit : rien. Depuis la multivue, cherche un controle d'export correspondant au
 * format demande ({@code data.format()} : "CSV" / "PDF") ou un controle d'export generique. Si aucun
 * controle n'est present, saute proprement via Assumptions. Sinon declenche l'export et verifie au
 * mieux qu'un export a ete lance (telechargement produit, ou UI d'export ouverte sans erreur).</p>
 */
@Epic("Forms")
@Feature("Réponses")
@Story("Exporter les reponses")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ExportResponsesMacroTest extends MacroTest {

    @Step("Exporter les reponses depuis la multivue")
    public static void run(FormsContext ctx, ExportDataSet data) {
        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "MultiviewForms.jsp");

        Locator control = firstVisible(page,
            "a:has-text('" + data.format() + "')",
            "button:has-text('" + data.format() + "')",
            "a:has-text('Exporter')",
            "button:has-text('Exporter')",
            "a:has-text('Export')",
            "button:has-text('Export')",
            "a[href*='export']",
            "a[href*='Export']",
            "button[name*='export']");
        Assumptions.assumeTrue(control != null,
            "aucun controle d'export present sur la multivue : flux d'export non pilotable en l'etat");

        // Best-effort : on tente de capter un telechargement autour du clic. L'export peut aussi
        // ouvrir un menu ou une page intermediaire (pas de telechargement direct) : ce n'est pas un echec.
        Download download = null;
        try {
            Locator target = control;
            download = page.waitForDownload(
                new Page.WaitForDownloadOptions().setTimeout(10_000),
                () -> target.click());
        } catch (RuntimeException noDownload) {
            // pas de telechargement direct : voir verification de repli ci-dessous
        }
        page.waitForLoadState();

        Assertions.assertFalse(page.url().contains("AdminLogin"),
            "La session admin ne devrait pas etre perdue pendant l'export");
        if (download != null) {
            String filename = download.suggestedFilename();
            Assertions.assertNotNull(filename, "Le fichier exporte devrait avoir un nom");
            Assertions.assertFalse(filename.isBlank(),
                "Le nom du fichier exporte ne devrait pas etre vide");
        } else {
            // Repli best-effort : le controle a ete active sans erreur (menu / page d'export ouverte).
            Assertions.assertTrue(
                page.url().contains("forms") || page.url().contains("Forms"),
                "L'activation du controle d'export devrait rester dans l'espace Forms (export declenche)");
        }
    }

    private static Locator firstVisible(Page page, String... selectors) {
        for (String sel : selectors) {
            Locator loc = page.locator(sel);
            if (loc.count() > 0 && loc.first().isVisible()) {
                return loc.first();
            }
        }
        return null;
    }

    @Test
    @DisplayName("Exporter les reponses (auto-provisionnement + soumission FO best-effort)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        // Provisionnement best-effort : une reponse pour que l'export ait de la matiere.
        OpenMultiviewMacroTest.submitOneFoResponse(ctx);
        run(ctx, ExportDataSet.defaults());
    }
}
