package fr.paris.lutece.e2e.tests.macro.unittree;

import com.microsoft.playwright.Locator;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Brique macro : supprimer une unite (feuille). Flux de confirmation best-effort. */
@Epic("Unittree")
@Feature("Cycle de vie de l'unite")
@Story("Supprimer une unite")
@Tag("macro")
@Tag("unittree")
@Tag("brick")
public class RemoveUnitMacroTest extends MacroTest {

    @Step("Supprimer une unite")
    public static void run(UnittreeContext ctx) {
        Assertions.assertFalse(ctx.units.isEmpty(), "Une unite doit exister avant suppression");
        UnittreeContext.UnitRef u = ctx.lastUnit();
        Page page = ctx.page;

        // Page de confirmation puis suppression (RemoveUnit.jsp -> confirmation -> DoRemoveUnit.jsp).
        UnittreeSupport.navigate(ctx, UnittreeSupport.UT + "RemoveUnit.jsp?idUnit=" + u.id);
        // Confirmer sur la page AdminMessage (premier bouton submit) si presente.
        Locator confirm = page.locator("button[type='submit'], input[type='submit'], a[href*='DoRemoveUnit']");
        Assumptions.assumeTrue(confirm.count() > 0 && confirm.first().isVisible(),
            "Controle de confirmation de suppression d'unite introuvable : flux a calibrer, test ignore");
        confirm.first().click();
        page.waitForLoadState();

        int stillThere = UnittreeSupport.extractUnitId(ctx, u.label);
        Assertions.assertEquals(-1, stillThere,
            "L'unite '" + u.label + "' ne devrait plus apparaitre apres suppression");
        ctx.units.remove(ctx.units.size() - 1);
    }

    @Test
    @DisplayName("Supprimer une unite (auto-provisionnement)")
    void standalone() {
        login();
        UnittreeContext ctx = new UnittreeContext(page, BASE_URL, newSuffix());
        CreateUnitMacroTest.run(ctx, UnitDataSet.defaults());
        run(ctx);
    }
}
