package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
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
 * Brique macro : verifier que l'historique porte le lien ramenant l'usager sur sa reponse.
 *
 * <p>Lit : la reponse instruite. Ecrit : rien dans le contexte.</p>
 *
 * <p>Une demande de correction ou de complement n'a d'effet que si l'usager peut y repondre. Le
 * lien qui l'y ramene est depose dans l'historique de la reponse au moment ou l'action s'execute :
 * son absence signale une demande qui ne menera nulle part, alors meme que l'etat a change et que
 * l'historique parait complet.</p>
 */
@Epic("Forms")
@Feature("Réponses")
@Story("Verifier le lien de retour en front office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyResponseFoLinkMacroTest extends MacroTest {

    @Step("Verifier le lien de retour vers le front office")
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) FormsContext ctx) {
        boolean ouvert = OpenResponseDetailMacroTest.reopenLastResponse(ctx)
            || OpenResponseDetailMacroTest.openFirstResponseDetail(ctx);
        Assumptions.assumeTrue(ouvert,
            "aucune reponse a inspecter : lien de retour non verifiable");

        Locator liens = ctx.page.locator("a[href]");
        int total = liens.count();
        StringBuilder vus = new StringBuilder();
        for (int i = 0; i < total; i++) {
            String href = liens.nth(i).getAttribute("href");
            if (href == null || !estLienFrontOffice(href)) {
                continue;
            }
            if (href.contains("forms")) {
                return;
            }
            vus.append(href, 0, Math.min(70, href.length())).append(' ');
        }
        Assertions.fail("L'historique de la reponse devrait porter un lien ramenant l'usager sur sa "
            + "reponse en front office apres une demande de correction ou de complement. "
            + "Liens front-office vus : " + (vus.length() == 0 ? "aucun" : vus.toString()));
    }

    /**
     * Indique si un lien pointe vers le front office du site.
     *
     * @param href cible du lien
     * @return true s'il s'agit d'une adresse du front office
     */
    private static boolean estLienFrontOffice(String href) {
        return href.contains("/jsp/site/") || href.contains("Portal.jsp");
    }

    @Test
    @DisplayName("Verifier le lien de retour en front office (ignore si la multivue est vide)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        run(ctx);
    }
}
