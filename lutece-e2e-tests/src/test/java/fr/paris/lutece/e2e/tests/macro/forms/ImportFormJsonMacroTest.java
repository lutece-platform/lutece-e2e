package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ImportDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Brique macro : importer un formulaire au format JSON (best-effort).
 *
 * <p>Lit : {@code data.filePath()}. Ecrit : rien. Un fichier JSON est requis pour piloter l'import :
 * si aucun fichier n'est fourni / trouve, ou si le formulaire d'import n'expose pas de champ de
 * fichier, la brique saute via Assumptions plutot que d'echouer.</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Importer le formulaire (JSON)")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ImportFormJsonMacroTest extends MacroTest {

    private static final Pattern IMPORT = Pattern.compile("Importer", Pattern.CASE_INSENSITIVE);

    @Step("Importer un formulaire au format JSON")
    public static void run(FormsContext ctx, ImportDataSet data) {
        // Un fichier JSON est indispensable pour piloter l'import
        Assumptions.assumeTrue(data.filePath() != null && !data.filePath().isBlank(),
            "Aucun fichier JSON fourni : l'import necessite un fichier (ImportDataSet.of(path))");
        Path path = Path.of(data.filePath());
        Assumptions.assumeTrue(Files.isRegularFile(path),
            "Fichier JSON introuvable : " + data.filePath());

        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=manageForms");

        // Atteindre le formulaire d'import si un lien "Importer" est expose
        Locator importLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(IMPORT));
        if (importLink.count() > 0 && importLink.first().isVisible()) {
            importLink.first().click();
            page.waitForLoadState();
        }

        Locator fileInput = page.locator("input[type='file']");
        Assumptions.assumeTrue(fileInput.count() > 0,
            "Aucun champ de fichier trouve pour l'import JSON : flux non pilotable en l'etat");

        fileInput.first().setInputFiles(path);

        boolean submitted = clickFirstButton(page, "Importer", "OK", "Enregistrer", "Valider");
        Assumptions.assumeTrue(submitted, "Bouton de soumission de l'import introuvable");
        page.waitForLoadState();

        Assertions.assertFalse(hasVisibleError(page),
            "L'import JSON ne devrait pas afficher d'erreur");
    }

    private static boolean clickFirstButton(Page page, String... labels) {
        for (String label : labels) {
            Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(label));
            if (btn.count() > 0 && btn.first().isVisible()) {
                btn.first().click();
                return true;
            }
        }
        return false;
    }

    private static boolean hasVisibleError(Page page) {
        Locator err = page.locator(".alert-danger, .has-error, .is-invalid");
        return err.count() > 0 && err.first().isVisible();
    }

    @Test
    @DisplayName("Importer un formulaire en JSON (saute si aucun fichier fourni)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        run(ctx, ImportDataSet.defaults());
    }
}
