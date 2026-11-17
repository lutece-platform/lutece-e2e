package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.InvalidJsonDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Brique negative : l'import d'un fichier JSON qui ne decrit pas un formulaire est refuse proprement.
 *
 * <p>Chemin reel : bouton « Importer » de la liste ({@code #btn-import-json}, ou {@code [data-bs-target=#import-json]} depuis forms 4.1), formulaire {@code #importJsonModal},
 * champ {@code json_file}, bouton {@code action_doImportJson}. Condition reelle ({@code FormJspBean.doImportJson}) :
 * une {@code JsonProcessingException} donne l'erreur {@code forms.error.form.not.imported} sur la liste
 * (« Erreur lors de l'import du formulaire »). Effet prouve : retour sur la liste avec ce message, pas de page
 * « Internal error », aucun formulaire nouveau (difference d'ensembles des ids).</p>
 *
 * <p>Constat du 2026-10-29 (bd4a558f) : un JSON valide sans cle {@code form} ({@link InvalidJsonDataSet#withoutForm()})
 * leve une {@code NullPointerException} dans {@code FormJsonService.importForm} et affiche « Internal error » : ce cas
 * est rouge, voir le cas remonte {@code FormsImportJsonWithoutFormRegressionTest}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Copie")
@Story("Refus : import JSON invalide")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("negative")
@Tag("nonreg")
public class RefuseImportInvalidJsonMacroTest extends MacroTest {

    static final String EXPECTED_ERROR = "Erreur lors de l'import du formulaire";

    @Step("Refus d'import du fichier {data.label}")
    public static void run(FormsContext ctx, InvalidJsonDataSet data) {
        Path file;
        try {
            file = Files.createTempFile(data.label() + "-", ".json");
            Files.writeString(file, data.content(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Set<Integer> before = FormsNonRegSupport.formIds(ctx);

        // bouton de l'offcanvas (forms 4.0) ou de la modale #import-json (forms 4.1, LUT-34136, releve du 2026-11-17)
        ctx.page.locator("#btn-import-json, button[data-bs-target='#import-json']").first().click();
        Locator form = ctx.page.locator("form#importJsonModal");
        form.locator("input[name='json_file']").setInputFiles(file);
        form.locator("button[name='action_doImportJson']").click();
        ctx.page.waitForLoadState();
        List<String> alerts = ctx.page.locator(".alert").allInnerTexts();
        String url = ctx.page.url();

        // Un formulaire cree a tort est enregistre pour le nettoyage avant toute assertion.
        List<Integer> fresh = FormsNonRegSupport.newIds(FormsNonRegSupport.formIds(ctx), new ArrayList<>(before));
        fresh.forEach(id -> ctx.createdForms.put(id, FormsNonRegSupport.formTitle(ctx, id)));

        Assertions.assertTrue(alerts.stream().noneMatch(a -> a.contains("Internal error")),
            "L'import de '" + data.label() + "' ne doit pas provoquer d'erreur interne : " + alerts);
        Assertions.assertTrue(alerts.stream().anyMatch(a -> a.contains(EXPECTED_ERROR)),
            "Message « " + EXPECTED_ERROR + " » attendu apres import de '" + data.label() + "' : " + alerts
                + " (url " + url + ")");
        Assertions.assertTrue(fresh.isEmpty(), "Aucun formulaire ne doit etre cree par l'import refuse : " + fresh);
    }

    @Test
    @DisplayName("Refus : importer un fichier qui n'est pas du JSON")
    void standaloneRefuseImportInvalidJson() {
        FormsContext ctx = newLoggedInContext();
        try {
            run(ctx, InvalidJsonDataSet.malformed());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
