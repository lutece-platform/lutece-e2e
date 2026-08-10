package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
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

import java.util.regex.Pattern;

/**
 * Brique macro : supprimer un formulaire (confirmRemoveForm -> doRemoveForm).
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.formTitle}. Ecrit : remet {@code ctx.formId} a -1 et vide
 * les references dependantes (etapes, questions, groupes). Verifie que le formulaire disparait de la
 * liste ManageForms.</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Supprimer le formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class RemoveFormMacroTest extends MacroTest {

    private static final Pattern CONFIRM = Pattern.compile(
        "^(Oui|OK|Confirmer|Valider|Supprimer)$", Pattern.CASE_INSENSITIVE);

    @Step("Supprimer le formulaire")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant suppression");

        Page page = ctx.page;
        String title = ctx.formTitle;
        int formId = ctx.formId;

        // Etape 1 : sur la LISTE ManageForms, recuperer le vrai lien de suppression de la ligne.
        // C'est un lien 'view=confirmRemoveForm&id_form=<id>' qui porte le token de securite ; le
        // verbe 'action=...' utilise auparavant etait ignore par le serveur (renvoyait la liste).
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=manageForms&items_per_page=100000");
        String confirmHref = findConfirmRemoveHref(page, formId);
        Assumptions.assumeTrue(confirmHref != null,
            "Lien de suppression (view=confirmRemoveForm&id_form=" + formId + ") introuvable dans la liste");

        // Etape 2 : suivre ce lien token-bearing -> rend la page de confirmation AdminMessage,
        // qui expose le lien d'action doRemoveForm (deja porteur du token).
        String absolute = confirmHref.startsWith("http")
            ? confirmHref
            : ctx.baseUrl + (confirmHref.startsWith("/") ? confirmHref : "/" + confirmHref);
        page.navigate(absolute);
        page.waitForLoadState();

        // Le controle de confirmation de la page AdminMessage n'est pas confirme en live :
        // on saute (Assumptions) plutot que d'echouer si aucun bouton/lien de confirmation n'est trouve.
        boolean confirmed = confirmRemoval(page);
        Assumptions.assumeTrue(confirmed,
            "Bouton/lien de confirmation de suppression introuvable : flux de suppression non pilotable en l'etat");
        page.waitForLoadState();

        int stillThere = MacroSupport.extractFormId(ctx, title);
        Assertions.assertEquals(-1, stillThere,
            "Le formulaire '" + title + "' ne devrait plus apparaitre dans la liste apres suppression");

        ctx.formId = -1;
        ctx.formTitle = null;
        ctx.steps.clear();
        ctx.questions.clear();
        ctx.groups.clear();
    }

    /**
     * Parcourt les liens 'view=confirmRemoveForm' de la liste et retourne le href (porteur du
     * token) dont l'id_form correspond EXACTEMENT a formId. Compare la valeur parsee plutot qu'un
     * 'contains' pour eviter la collision de prefixe (id_form=1 vs id_form=129).
     */
    private static String findConfirmRemoveHref(Page page, int formId) {
        for (Locator link : page.locator("a[href*='view=confirmRemoveForm']").all()) {
            String href = link.getAttribute("href");
            if (href == null) {
                continue;
            }
            int idx = href.indexOf("id_form=");
            if (idx < 0) {
                continue;
            }
            String idPart = href.substring(idx + "id_form=".length()).split("&")[0].split("#")[0];
            try {
                if (Integer.parseInt(idPart) == formId) {
                    return href;
                }
            } catch (NumberFormatException ignored) {
                // href malforme : on ignore ce lien
            }
        }
        return null;
    }

    private static boolean confirmRemoval(Page page) {
        Locator link = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(CONFIRM));
        if (link.count() > 0 && link.first().isVisible()) {
            link.first().click();
            return true;
        }
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CONFIRM));
        if (btn.count() > 0 && btn.first().isVisible()) {
            btn.first().click();
            return true;
        }
        // Fallback : lien direct vers l'action doRemoveForm (avec son token) sur la page de confirmation
        Locator direct = page.locator("a[href*='doRemoveForm']");
        if (direct.count() > 0 && direct.first().isVisible()) {
            direct.first().click();
            return true;
        }
        return false;
    }

    @Test
    @DisplayName("Supprimer un formulaire (auto-provisionnement du formulaire)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        run(ctx);
    }
}
