package fr.paris.lutece.e2e.tests.macro.unittree;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.UnittreeSupport;
import fr.paris.lutece.e2e.tests.macro.data.UnitDataSet;
import fr.paris.lutece.e2e.tests.macro.data.UserAssignmentDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Brique macro : affecter un utilisateur admin a une unite. Lit : ctx.units. */
@Epic("Unittree")
@Feature("Utilisateurs")
@Story("Affecter un utilisateur a une unite")
@Tag("macro")
@Tag("unittree")
@Tag("brick")
public class AddUsersToUnitMacroTest extends MacroTest {

    @Step("Affecter un utilisateur a une unite")
    public static void run(UnittreeContext ctx, UserAssignmentDataSet data) {
        Assertions.assertFalse(ctx.units.isEmpty(), "Une unite doit exister avant d'y affecter un utilisateur");
        UnittreeContext.UnitRef u = ctx.lastUnit();
        Page page = ctx.page;

        UnittreeSupport.navigate(ctx, UnittreeSupport.UT + "AddUsers.jsp?idUnit=" + u.id);
        // Cocher l'utilisateur cible (checkbox name='idUsers' value=<userId>).
        Locator userCb = page.locator("input[name='idUsers'][value='" + data.userId() + "']");
        Assumptions.assumeTrue(userCb.count() > 0,
            "Utilisateur id=" + data.userId() + " absent de la liste d'affectation (deja affecte ?) : ignore");
        userCb.first().check();
        page.locator("button[name='addUsers'], button[name='save']").first().click();
        page.waitForLoadState();

        Assertions.assertFalse(page.url().contains("AddUsers.jsp"),
            "L'affectation aurait du rediriger hors de AddUsers ; url: " + page.url());
    }

    @Test
    @DisplayName("Affecter un utilisateur a une unite (auto-provisionnement)")
    void standalone() {
        login();
        UnittreeContext ctx = new UnittreeContext(page, BASE_URL, newSuffix());
        CreateUnitMacroTest.run(ctx, UnitDataSet.defaults());
        run(ctx, UserAssignmentDataSet.defaults());
    }
}
