package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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
 * Brique macro FO : connecter l'usager front-office du contexte (MyLutece database).
 *
 * <p>Chemin reel : {@code Portal.jsp?page=mylutece&action=login}, formulaire {@code DoMyLuteceLogin.jsp}
 * ({@code username}, {@code password}). Si le mot de passe est expire (premiere connexion d'un usager cree par
 * l'administrateur, constat du 2026-10-29), le site affiche « Votre mot de passe n'est plus valide » : la brique
 * suit le bouton « Valider » ({@code action=changePassword}), remplit {@code DoChangePassword.jsp} avec
 * {@code newPassword} et exige « Le mot de passe a ete change avec succes ». Effet prouve : la page de connexion
 * propose ensuite la deconnexion ({@code DoMyLuteceLogout.jsp}).</p>
 *
 * <p>Lit : {@code ctx.frontUserLogin}, {@code ctx.frontUserPassword}. Ecrit : {@code ctx.frontUserPassword} apres
 * changement impose. Dataset : seul {@code newPassword} est utilise.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Connecter un usager front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class LoginFrontUserFOMacroTest extends MacroTest {

    static final String LOGIN_PAGE = "/jsp/site/Portal.jsp?page=mylutece&action=login";
    static final String LOGOUT_LINK = "a[href*='DoMyLuteceLogout']";

    @Step("Connecter l'usager front-office")
    public static void run(FormsContext ctx, FrontUserDataSet data) {
        Assertions.assertNotNull(ctx.frontUserLogin, "Un usager front-office doit exister (CreateFrontUserBO)");
        Page page = ctx.page;
        MacroSupport.navigate(ctx, LOGIN_PAGE);
        MacroSupport.dismissCookieBanner(page);
        Locator form = page.locator("form[action*='DoMyLuteceLogin']");
        Assertions.assertEquals(1, form.count(), "Le formulaire de connexion FO doit etre affiche");
        form.locator("input[name='username']").fill(ctx.frontUserLogin);
        form.locator("input[name='password']").fill(ctx.frontUserPassword);
        form.locator("button[type='submit']").click();
        page.waitForLoadState();

        Locator expired = page.locator("form[action*='action=changePassword'] button[type='submit']");
        if (expired.count() > 0) {
            expired.click();
            page.waitForLoadState();
            Locator change = page.locator("form[action*='DoChangePassword']");
            Assertions.assertEquals(1, change.count(), "Le formulaire de changement de mot de passe doit etre affiche");
            change.locator("input[name='old_password']").fill(ctx.frontUserPassword);
            change.locator("input[name='new_password']").fill(data.newPassword());
            change.locator("input[name='confirmation_password']").fill(data.newPassword());
            change.locator("button[name='changePassword']").click();
            page.waitForLoadState();
            Assertions.assertTrue(page.locator(".alert").allInnerTexts().stream().anyMatch(a -> a.contains("change avec succes")
                    || a.contains("changé avec succès")),
                "Le changement de mot de passe impose doit reussir : " + page.locator(".alert").allInnerTexts());
            ctx.frontUserPassword = data.newPassword();
        }

        MacroSupport.navigate(ctx, LOGIN_PAGE);
        Assertions.assertTrue(page.locator(LOGOUT_LINK).count() > 0,
            "L'usager " + ctx.frontUserLogin + " doit etre connecte (lien de deconnexion attendu, url " + page.url() + ")");
    }

    @Test
    @DisplayName("Connecter un usager FO cree en BO (changement de mot de passe impose)")
    void standaloneLoginFrontUserFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFrontUserBOMacroTest.run(ctx, FrontUserDataSet.defaults());
            run(ctx, FrontUserDataSet.defaults());
            LogoutFrontUserFOMacroTest.run(ctx);
            run(ctx, FrontUserDataSet.defaults());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
