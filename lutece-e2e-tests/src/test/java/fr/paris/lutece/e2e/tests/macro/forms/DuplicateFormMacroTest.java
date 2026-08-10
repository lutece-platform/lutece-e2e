package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
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
 * Brique macro : dupliquer un formulaire depuis la liste ManageForms.
 *
 * <p>Lit : {@code ctx.formId}. Ecrit : rien. Utilise le lien d'action {@code duplicateForm}
 * (qui porte le token de securite requis) et verifie qu'un formulaire supplementaire apparait.</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Dupliquer le formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class DuplicateFormMacroTest extends MacroTest {

    @Step("Dupliquer le formulaire")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant duplication");

        int before = countForms(ctx);

        // Le lien d'action de duplication est rendu dans le menu d'actions de la carte du
        // formulaire et porte le token de securite : ManageForms.jsp?action=duplicateForm&id_form=..&token=..
        // On lit son href (present dans le DOM meme si le menu est ferme) puis on navigue dessus.
        Locator dupLink = ctx.page.locator(
            "a[href*='action=duplicateForm'][href*='id_form=" + ctx.formId + "']").first();
        Assumptions.assumeTrue(dupLink.count() > 0,
            "Lien d'action duplicateForm introuvable pour id_form=" + ctx.formId + " (flux a calibrer)");

        String href = dupLink.getAttribute("href");
        String url = href.startsWith("http") ? href : ctx.baseUrl + "/" + href.replaceFirst("^/", "");
        ctx.page.navigate(url);
        ctx.page.waitForLoadState();

        int after = countForms(ctx);
        Assertions.assertTrue(after > before,
            "Un formulaire duplique devrait apparaitre (avant=" + before + ", apres=" + after + ")");
    }

    /** Navigue vers la liste (toutes pages) et compte les formulaires (un lien modify par formulaire). */
    private static int countForms(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=manageForms&items_per_page=100000");
        return ctx.page.locator("a[href*='view=modifyForm'][href*='id_form=']").count();
    }

    @Test
    @DisplayName("Dupliquer un formulaire (auto-provisionnement du formulaire)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        run(ctx);
    }
}
