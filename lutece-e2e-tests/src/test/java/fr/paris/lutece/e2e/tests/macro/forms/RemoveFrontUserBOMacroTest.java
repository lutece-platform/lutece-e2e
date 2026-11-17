package fr.paris.lutece.e2e.tests.macro.forms;

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
 * Brique macro : supprimer l'usager front-office du contexte.
 *
 * <p>Chemin reel : lien « Supprimer l'utilisateur » de sa ligne ({@code RemoveUser.jsp?mylutece_database_user_id=N}),
 * confirmation AdminMessage ({@code DoRemoveUser.jsp}). Effet prouve : la recherche par login ne le trouve plus.
 * Appelee aussi par {@code FormsNonRegSupport.cleanup}. Lit / remet a zero : {@code ctx.frontUser*}.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Supprimer un usager front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class RemoveFrontUserBOMacroTest extends MacroTest {

    @Step("Supprimer l'usager front-office")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.frontUserId > 0, "Un usager front-office doit exister (CreateFrontUserBO)");
        Assertions.assertEquals(ctx.frontUserId, FormsNonRegSupport.frontUserId(ctx, ctx.frontUserLogin),
            "L'usager " + ctx.frontUserLogin + " doit etre liste avant suppression");
        String href = FormsNonRegSupport.linkHref(ctx, "RemoveUser.jsp", "mylutece_database_user_id", ctx.frontUserId);
        Assertions.assertNotNull(href, "Lien de suppression introuvable pour l'usager " + ctx.frontUserId);
        MacroSupport.navigate(ctx, "/" + href);
        ctx.page.locator("form[action*='DoRemoveUser.jsp'] button[type='submit']").click();
        ctx.page.waitForLoadState();

        Assertions.assertEquals(-1, FormsNonRegSupport.frontUserId(ctx, ctx.frontUserLogin),
            "L'usager " + ctx.frontUserLogin + " ne doit plus etre liste");
        ctx.frontUserId = -1;
        ctx.frontUserLogin = null;
        ctx.frontUserPassword = null;
    }

    @Test
    @DisplayName("Supprimer un usager front-office cree pour l'occasion")
    void standaloneRemoveFrontUserBO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFrontUserBOMacroTest.run(ctx, FrontUserDataSet.defaults());
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
