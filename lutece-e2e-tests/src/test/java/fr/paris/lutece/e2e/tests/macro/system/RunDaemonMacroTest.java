package fr.paris.lutece.e2e.tests.macro.system;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.DaemonDataSet;
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
 * Brique macro : declencher immediatement un daemon Lutece.
 *
 * <p>Lit : la page courante. Ecrit : rien dans le contexte.</p>
 *
 * <p><b>A quoi cela sert dans un parcours.</b> Plusieurs vues du back office ne lisent pas la base
 * mais un index Lucene alimente par un daemon. C'est le cas de la multivue des reponses : une
 * reponse soumise en front office n'y apparait qu'une fois {@code formsIndexerDaemon} passe. Son
 * intervalle de declenchement n'a aucune raison de coincider avec l'execution d'un test, d'ou ce
 * declenchement explicite plutot qu'une attente aveugle.</p>
 *
 * <p>Le declenchement passe par l'IHM et non par un appel direct a {@code DoDaemonAction.jsp} :
 * l'action est protegee par un jeton de securite porte par le formulaire de la page.</p>
 */
@Epic("Systeme")
@Feature("Daemons")
@Story("Declencher un daemon")
@Tag("macro")
@Tag("system")
@Tag("brick")
public class RunDaemonMacroTest extends MacroTest {

    /**
     * Page de gestion des daemons.
     *
     * <p>Le parametre {@code plugin_name} vide est celui que produit le menu d'administration :
     * la page s'attend a le recevoir pour presenter l'ensemble des daemons, tous plugins
     * confondus.</p>
     */
    private static final String MANAGE_DAEMONS = "/jsp/admin/system/ManageDaemons.jsp?plugin_name=";

    /** Delai laisse au daemon pour produire son effet avant la suite du scenario, en ms. */
    private static final double SETTLE_MS = 2000;

    @Step("Declencher le daemon")
    public static void run(FormsContext ctx, DaemonDataSet data) {
        Page page = ctx.page;
        MacroSupport.navigate(ctx, MANAGE_DAEMONS);

        Locator marqueur = page.locator("input[name='daemon'][value='" + data.daemonKey() + "']");
        boolean present = marqueur.count() > 0;
        Assumptions.assumeTrue(present,
            "Daemon '" + data.daemonKey() + "' absent de la page de gestion des daemons : "
                + "declenchement ignore (plugin non installe sur ce site ?)");

        // Le bouton d'execution vit dans le formulaire qui porte la cle du daemon : on remonte au
        // formulaire plutot que de cibler un bouton par son libelle, identique pour tous les daemons.
        Locator formulaire = marqueur.first().locator("xpath=ancestor::form[1]");
        Locator executer = formulaire.locator("button[type='submit'], input[type='submit']").last();
        executer.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE).setTimeout(10_000));
        executer.click();
        page.waitForLoadState();
        page.waitForTimeout(SETTLE_MS);

        Assertions.assertTrue(page.url().contains("Daemon") || page.url().contains("ManageDaemons"),
            "Le declenchement du daemon aurait du ramener sur la gestion des daemons ; url: " + page.url());
    }

    @Test
    @DisplayName("Declencher le daemon d'indexation des reponses de formulaire")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        run(ctx, DaemonDataSet.formsIndexer());
    }
}
