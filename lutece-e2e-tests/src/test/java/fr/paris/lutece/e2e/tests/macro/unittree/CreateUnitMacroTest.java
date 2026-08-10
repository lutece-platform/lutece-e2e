package fr.paris.lutece.e2e.tests.macro.unittree;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.UnittreeSupport;
import fr.paris.lutece.e2e.tests.macro.data.UnitDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : creer une unite (sous une unite parente ; racine = 0).
 *
 * <p>Lit : rien. Ecrit : ajoute un {@link UnittreeContext.UnitRef} a {@code ctx.units}.</p>
 */
@Epic("Unittree")
@Feature("Cycle de vie de l'unite")
@Story("Creer une unite")
@Tag("macro")
@Tag("unittree")
@Tag("brick")
public class CreateUnitMacroTest extends MacroTest {

    @Step("Creer une unite")
    public static void run(UnittreeContext ctx, UnitDataSet data) {
        String label = data.label() + " " + ctx.runSuffix;
        // Le code d'unite doit etre UNIQUE : on combine suffixe + index d'unite du run (plusieurs
        // unites creees dans le meme run partagent le suffixe, d'ou l'ajout de l'index).
        String code = data.code() + ctx.runSuffix + "-" + ctx.units.size();
        Page page = ctx.page;

        UnittreeSupport.navigate(ctx, UnittreeSupport.UT + "CreateUnit.jsp?idParent=" + data.parentId());
        page.locator("input[name='code']").fill(code);
        page.locator("input[name='label']").fill(label);
        page.locator("input[name='description']").fill(data.description());
        // Bouton submit name='save' (icone + title, pas de texte).
        page.locator("button[name='save']").first().click();
        page.waitForLoadState();

        // Succes = redirection hors de CreateUnit (DoCreateUnit redirige vers ManageUnits).
        Assertions.assertFalse(page.url().contains("CreateUnit.jsp"),
            "La creation de l'unite '" + label + "' aurait du rediriger hors de CreateUnit ; url: " + page.url());

        int id = UnittreeSupport.extractUnitId(ctx, label);
        Assertions.assertTrue(id > 0, "L'unite '" + label + "' devrait apparaitre dans l'arborescence");
        ctx.units.add(new UnittreeContext.UnitRef(id, label, data.parentId()));
    }

    @Test
    @DisplayName("Creer une unite (sous la racine)")
    void standalone() {
        login();
        UnittreeContext ctx = new UnittreeContext(page, BASE_URL, newSuffix());
        run(ctx, UnitDataSet.defaults());
    }
}
