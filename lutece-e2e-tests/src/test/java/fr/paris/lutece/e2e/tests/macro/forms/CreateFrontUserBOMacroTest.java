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
 * Brique macro : creer un usager front-office MyLutece (module database) depuis le back-office.
 *
 * <p>Chemin reel : « Gestion des utilisateurs du site (Database) », ecran {@code CreateUser.jsp} (formulaire
 * {@code DoCreateUser.jsp} : login, e-mail, mot de passe et confirmation, nom, prenom). Les champs sont vises par leur
 * {@code name} : le template porte un id {@code first_password} en double (champ et jauge de complexite). Effet prouve :
 * l'usager est retrouve par la recherche par login de la liste, avec son id.</p>
 *
 * <p>Le mot de passe pose par l'administrateur est expire des la premiere connexion FO (changement impose, gere par
 * {@code LoginFrontUserFO}). Ecrit : {@code ctx.frontUserLogin}, {@code ctx.frontUserPassword}, {@code ctx.frontUserId}.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Creer un usager front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class CreateFrontUserBOMacroTest extends MacroTest {

    @Step("Creer l'usager front-office {data.loginPrefix}")
    public static void run(FormsContext ctx, FrontUserDataSet data) {
        String login = data.loginPrefix() + ctx.runSuffix;
        Assertions.assertEquals(-1, FormsNonRegSupport.frontUserId(ctx, login), "L'usager " + login + " ne doit pas encore exister");

        MacroSupport.navigate(ctx, FormsNonRegSupport.MYLUTECE_DB + "CreateUser.jsp?plugin_name=mylutece-database");
        Locator form = ctx.page.locator("form[action*='DoCreateUser']");
        Assertions.assertEquals(1, form.count(), "Le formulaire de creation d'usager doit etre affiche (url " + ctx.page.url() + ")");
        form.locator("input[name='login']").fill(login);
        form.locator("input[name='email']").fill(login + "@example.org");
        form.locator("input[name='first_password']").fill(data.password());
        form.locator("input[name='second_password']").fill(data.password());
        form.locator("input[name='last_name']").fill("Usager");
        form.locator("input[name='first_name']").fill("E2E");
        form.locator("button[type='submit']").click();
        ctx.page.waitForLoadState();

        int id = FormsNonRegSupport.frontUserId(ctx, login);
        Assertions.assertTrue(id > 0, "L'usager " + login + " doit etre liste apres creation (alertes : "
            + ctx.page.locator(".alert").allInnerTexts() + ")");
        ctx.frontUserLogin = login;
        ctx.frontUserPassword = data.password();
        ctx.frontUserId = id;
    }

    @Test
    @DisplayName("Creer puis supprimer un usager front-office")
    void standaloneCreateFrontUserBO() {
        FormsContext ctx = newLoggedInContext();
        try {
            run(ctx, FrontUserDataSet.defaults());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
