package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.Evidence;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.NotificationDataSet;
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
 * Brique macro : verifier le contenu d'une notification dans l'historique d'une reponse.
 *
 * <p>Lit : le detail de la reponse que l'execution courante a instruite. Ecrit : rien. Se saute si
 * aucune reponse n'a ete instruite : il n'y a alors aucune notification a attendre.</p>
 *
 * <p>Constater qu'une notification figure dans l'historique ne dit pas ce qu'elle contient : une
 * tache mal parametree depose une notification vide, ou porteuse du message d'une autre action.
 * Le detail n'affiche pas le corps du message directement — il le garde sur le declencheur qui
 * l'ouvre, dont on lit l'attribut plutot que de simuler l'ouverture, ce qui rendrait le controle
 * tributaire de l'animation du composant.</p>
 */
@Epic("Forms")
@Feature("Notifications")
@Story("Verifier le contenu d'une notification")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyNotificationMacroTest extends MacroTest {

    /** Attribut portant le corps de la notification. */
    private static final String ATTRIBUT_CONTENU = "data-bs-content";

    /** Attribut portant le libelle du canal. */
    private static final String ATTRIBUT_CANAL = "data-bs-original-title";

    @Step("Verifier le contenu de la notification deposee sur la reponse")
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) FormsContext ctx, NotificationDataSet data) {
        // Une notification n'existe que sur une reponse qu'une action est venue instruire. Hors
        // parcours d'instruction, la brique ouvrirait la premiere reponse de la multivue — celle
        // d'un autre parcours, qu'aucune action n'a touchee — et conclurait a une tache de
        // notification defaillante sur un site parfaitement sain.
        Assumptions.assumeTrue(ctx.lastResponseId != null,
            "aucune reponse instruite par cette execution : notification non verifiable");
        OpenResponseDetailMacroTest.reopenLastResponse(ctx);

        Locator declencheurs = ctx.page.locator("[" + ATTRIBUT_CONTENU + "]");
        int total = declencheurs.count();
        Assertions.assertTrue(total > 0,
            "Le detail de la reponse ne porte aucune notification : la tache de notification n'a "
                + "rien produit lors de l'execution de l'action");

        StringBuilder lus = new StringBuilder();
        for (int i = 0; i < total; i++) {
            String canal = declencheurs.nth(i).getAttribute(ATTRIBUT_CANAL);
            String contenu = texteLisible(declencheurs.nth(i).getAttribute(ATTRIBUT_CONTENU));
            lus.append('[').append(canal).append(" : ").append(contenu).append(']');
            if (canal != null && canal.contains(data.canal())
                && contenu.contains(data.message())) {
                Evidence.texte("Notification " + canal, contenu);
                return;
            }
        }
        Evidence.texte("Notifications lues sur la reponse", lus.toString());
        Assertions.fail("Aucune notification '" + data.canal() + "' ne porte le message '"
            + data.message() + "'. Notifications lues : " + lus);
    }

    /**
     * Texte lisible d'un corps de notification.
     *
     * <p>Le corps est stocke sous forme de balisage, apostrophes et chevrons compris, tels que les
     * a produits l'editeur ou qu'ils ont ete echappes a l'affichage. Comparer la chaine brute
     * ferait echouer la verification sur la seule apostrophe d'un message pourtant conforme.</p>
     *
     * @param brut contenu de l'attribut, tel que lu
     * @return le texte correspondant, sans balise ni entite
     */
    private static String texteLisible(String brut) {
        if (brut == null) {
            return "";
        }
        return brut.replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replaceAll("<[^>]*>", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    @Test
    @DisplayName("Verifier le contenu d'une notification (ignore hors parcours d'instruction)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        run(ctx, NotificationDataSet.of("Agent", ""));
    }
}
