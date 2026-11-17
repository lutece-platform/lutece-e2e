package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FrontUserDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro FO : deconnecter l'usager front-office par le lien de deconnexion de la page ({@code DoMyLuteceLogout.jsp}),
 * comme l'usager, et non plus en reconstruisant son URL (revue du 2026-10-30, constat 14).
 * Effet prouve : la page de connexion propose de nouveau le formulaire et plus la deconnexion. La session
 * d'administration du meme navigateur n'est pas touchee. Ecrit : rien.
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Deconnecter un usager front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class LogoutFrontUserFOMacroTest extends MacroTest {

    @Step("Deconnecter l'usager front-office")
    public static void run(FormsContext ctx) {
        MacroSupport.navigate(ctx, LoginFrontUserFOMacroTest.LOGIN_PAGE);
        Assertions.assertTrue(ctx.page.locator(LoginFrontUserFOMacroTest.LOGOUT_LINK).count() > 0,
            "Un usager front-office doit etre connecte avant la deconnexion");
        Locator logout = ctx.page.locator(LoginFrontUserFOMacroTest.LOGOUT_LINK + ":visible");
        if (logout.count() == 0) {
            // Lien range dans un menu deroulant : ouvrir le menu qui le contient, comme l'usager.
            ctx.page.locator("[data-bs-toggle='dropdown']:has(~ * " + LoginFrontUserFOMacroTest.LOGOUT_LINK + ")").first().click();
        }
        Assertions.assertTrue(logout.count() >= 1, "Le lien de deconnexion doit etre accessible a l'usager");
        logout.first().click();
        ctx.page.waitForLoadState();

        MacroSupport.navigate(ctx, LoginFrontUserFOMacroTest.LOGIN_PAGE);
        Assertions.assertEquals(0, ctx.page.locator(LoginFrontUserFOMacroTest.LOGOUT_LINK).count(),
            "Apres deconnexion, la page ne doit plus proposer la deconnexion");
        Assertions.assertEquals(1, ctx.page.locator("form[action*='DoMyLuteceLogin']").count(),
            "Apres deconnexion, le formulaire de connexion doit etre affiche");
    }

    @Test
    @DisplayName("Deconnecter un usager FO")
    void standaloneLogoutFrontUserFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFrontUserBOMacroTest.run(ctx, FrontUserDataSet.defaults());
            LoginFrontUserFOMacroTest.run(ctx, FrontUserDataSet.defaults());
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
