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
 * Brique macro : importer le dernier export JSON comme nouveau formulaire (bouton « Importer » de la liste =
 * offcanvas {@code #import-json} (modale depuis forms 4.1), formulaire {@code #importJsonModal}, champ {@code json_file}, bouton
 * {@code action_doImportJson}).
 *
 * <p>Effet prouve par difference d'ensembles des ids de la liste (exactement un formulaire nouveau). La
 * conformite se prouve ensuite par {@code VerifyFormStructureEqual}. Complement de {@code ImportFormJson}, qui
 * se met en skip sans fichier. Lit : {@code ctx.lastExportedJson}. Ecrit : {@code ctx.createdForms} ; renvoie
 * l'id du formulaire importe.</p>
 */
@Epic("Forms")
@Feature("Copie")
@Story("Importer un formulaire JSON")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ImportFormJsonFileMacroTest extends MacroTest {

    @Step("Importer le dernier export JSON")
    public static int run(FormsContext ctx) {
        Assertions.assertNotNull(ctx.lastExportedJson, "Un export JSON doit exister (ExportFormJsonFile)");
        Set<Integer> before = FormsNonRegSupport.formIds(ctx);

        // bouton de l'offcanvas (forms 4.0) ou de la modale #import-json (forms 4.1, LUT-34136, releve du 2026-11-17)
        ctx.page.locator("#btn-import-json, button[data-bs-target='#import-json']").first().click();
        Locator form = ctx.page.locator("form#importJsonModal");
        form.locator("input[name='json_file']").setInputFiles(ctx.lastExportedJson);
        form.locator("button[name='action_doImportJson']").click();
        ctx.page.waitForLoadState();

        List<Integer> fresh = FormsNonRegSupport.newIds(FormsNonRegSupport.formIds(ctx), new ArrayList<>(before));
        FormsNonRegSupport.registerCreatedForms(ctx, fresh);
        Assertions.assertEquals(1, fresh.size(), "Exactement un formulaire nouveau attendu apres import : " + fresh
            + " (alertes : " + ctx.page.locator(".alert").allInnerTexts() + ")");
        int imported = fresh.get(0);
        Assertions.assertTrue(ctx.createdForms.containsKey(imported), "Titre du formulaire importe " + imported
            + " derive de '" + ctx.formTitle + "' attendu : '" + FormsNonRegSupport.formTitle(ctx, imported) + "'");
        return imported;
    }

    @Test
    @DisplayName("Exporter puis reimporter un formulaire : meme structure")
    void standaloneImportFormJsonFile() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Identite"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Coordonnees"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Civilite"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            ExportFormJsonFileMacroTest.run(ctx);
            int imported = run(ctx);
            VerifyFormStructureEqualMacroTest.run(ctx, imported);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
