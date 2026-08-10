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

/** Brique macro : renommer une unite. Lit : ctx.units. Ecrit : met a jour le libelle. */
@Epic("Unittree")
@Feature("Cycle de vie de l'unite")
@Story("Modifier une unite")
@Tag("macro")
@Tag("unittree")
@Tag("brick")
public class ModifyUnitMacroTest extends MacroTest {

    @Step("Modifier une unite")
    public static void run(UnittreeContext ctx, UnitDataSet data) {
        Assertions.assertFalse(ctx.units.isEmpty(), "Une unite doit exister avant modification");
        UnittreeContext.UnitRef u = ctx.lastUnit();
        String newLabel = data.label() + " " + ctx.runSuffix;
        Page page = ctx.page;

        UnittreeSupport.navigate(ctx, UnittreeSupport.UT + "ModifyUnit.jsp?idUnit=" + u.id);
        // Le formulaire charge les valeurs courantes (code/label/description non vides) ; on change le libelle.
        page.locator("input[name='label']").fill(newLabel);
        page.locator("button[name='save']").first().click();
        page.waitForLoadState();

        Assertions.assertFalse(page.url().contains("ModifyUnit.jsp"),
            "La modification de l'unite aurait du rediriger hors de ModifyUnit ; url: " + page.url());
        u.label = newLabel;
    }

    @Test
    @DisplayName("Modifier une unite (auto-provisionnement)")
    void standalone() {
        login();
        UnittreeContext ctx = new UnittreeContext(page, BASE_URL, newSuffix());
        CreateUnitMacroTest.run(ctx, UnitDataSet.defaults());
        run(ctx, UnitDataSet.of("Service Renomme"));
    }
}
