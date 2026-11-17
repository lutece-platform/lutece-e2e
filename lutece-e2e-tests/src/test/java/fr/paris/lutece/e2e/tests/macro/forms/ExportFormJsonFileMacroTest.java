package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brique macro : exporter le formulaire en JSON par le lien « Telecharger » de la liste
 * ({@code export/DoExportFormJson.jsp?id_form=F&token=T}) et garder le fichier.
 *
 * <p>Effet prouve sur le fichier telecharge : JSON valide, et chaque titre d'etape et de question du contexte y
 * figure. Complement de {@code ExportFormJson}, qui attend {@code action=doExportJson} et se met en skip sur
 * forms upstream {@code develop} (constate le 2026-10-29). Ecrit : {@code ctx.lastExportedJson}
 * ({@code target/forms-export/<id>.json}).</p>
 */
@Epic("Forms")
@Feature("Copie")
@Story("Exporter un formulaire en JSON")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ExportFormJsonFileMacroTest extends MacroTest {

    @Step("Exporter le formulaire en JSON")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        FormsNonRegSupport.formIds(ctx);
        String href = FormsNonRegSupport.linkHref(ctx, "DoExportFormJson.jsp", "id_form", ctx.formId);
        Assertions.assertNotNull(href, "Lien « Telecharger » introuvable pour le formulaire " + ctx.formId);
        Locator link = ctx.page.locator("a[href='" + href + "']").first();
        Download download = ctx.page.waitForDownload(() -> link.evaluate("a => a.click()"));
        try {
            Path file = Path.of("target", "forms-export", ctx.formId + ".json");
            Files.createDirectories(file.getParent());
            download.saveAs(file);
            String json = Files.readString(file);
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(json);
            Assertions.assertTrue(root.isJsonObject(), "L'export doit etre un objet JSON");
            for (FormsContext.StepRef step : ctx.steps) {
                Assertions.assertTrue(json.contains("\"title\":\"" + step.title + "\""), "L'export doit contenir l'etape '" + step.title + "'");
            }
            for (FormsContext.QuestionRef question : ctx.questions) {
                Assertions.assertTrue(json.contains("\"title\":\"" + question.title + "\""), "L'export doit contenir la question '" + question.title + "'");
            }
            ctx.lastExportedJson = file;
        } catch (java.io.IOException e) {
            Assertions.fail("Export JSON illisible : " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Exporter en JSON un formulaire a deux etapes")
    void standaloneExportFormJsonFile() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Identite"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Coordonnees"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Civilite"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
