package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : supprimer toutes les liaisons sortantes d'une etape.
 *
 * <p>Lit : {@code ctx.steps}. Ecrit : rien dans le contexte.</p>
 *
 * <p><b>Pourquoi vider avant de construire.</b> Creer une etape ne la laisse pas isolee : Lutece
 * la relie automatiquement a celle qui la precede, de sorte qu'un formulaire a peine cree possede
 * deja un enchainement lineaire. Un scenario qui pose ses propres liaisons par-dessus obtient donc
 * un graphe different de celui qu'il decrit — avec, en premiere position, une liaison qu'il n'a
 * jamais demandee et qui l'emporte sur les siennes, puisque les liaisons sont examinees par ordre
 * de priorite. Repartir d'une etape sans liaison est le seul moyen d'obtenir le parcours voulu.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Vider les liaisons d'une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ClearStepTransitionsMacroTest extends MacroTest {

    /** Nombre maximal de liaisons sortantes traitees pour une meme etape. */
    private static final int MAX_LIAISONS = 20;

    @Step("Supprimer les liaisons sortantes de l'etape")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && ctx.steps.size() > data.stepIndex(),
            "Un formulaire et l'etape visee doivent exister avant de vider ses liaisons");

        FormsContext.StepRef step = ctx.steps.get(data.stepIndex());
        Page page = ctx.page;

        // Le nombre de passages est borne : une suppression qui n'aboutirait pas laisserait le
        // compte inchange, et la boucle tournerait sans fin au lieu d'echouer. L'assertion qui suit
        // signale alors proprement les liaisons restantes.
        for (int passage = 0; passage < MAX_LIAISONS; passage++) {
            if (supprimerUneLiaison(ctx, step.id) <= 0) {
                break;
            }
        }

        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + step.id);
        Assertions.assertEquals(0, page.locator("a[href*='confirmRemoveTransition']").count(),
            "L'etape '" + step.title + "' devrait n'avoir plus aucune liaison sortante");
    }

    /**
     * Supprime la premiere liaison sortante de l'etape, s'il en reste une.
     *
     * <p>Les liaisons sont reprises a chaque passage plutot que collectees d'avance : leurs
     * identifiants et leurs priorites sont reattribues a chaque suppression.</p>
     *
     * @param ctx    contexte formulaire courant
     * @param stepId identifiant de l'etape
     * @return le nombre de liaisons restantes avant cette suppression, 0 s'il n'y en avait aucune
     */
    private static int supprimerUneLiaison(FormsContext ctx, int stepId) {
        Page page = ctx.page;
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + stepId);

        Locator liens = page.locator("a[href*='confirmRemoveTransition']");
        int total = liens.count();
        if (total == 0) {
            return 0;
        }
        String href = liens.first().getAttribute("href");
        MacroSupport.navigate(ctx, "/" + href.replaceFirst("^/", ""));
        confirmer(page);
        page.waitForLoadState();
        return total - 1;
    }

    /**
     * Confirme la suppression sur la page de message de Lutece.
     *
     * <p>Le bouton de confirmation y est rendu tantot comme lien, tantot comme bouton selon les
     * versions : les deux sont tentes.</p>
     *
     * @param page page de confirmation
     */
    private static void confirmer(Page page) {
        Locator lien = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("OK"));
        if (lien.count() > 0 && lien.first().isVisible()) {
            lien.first().click();
            return;
        }
        Locator bouton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("OK"));
        Assertions.assertTrue(bouton.count() > 0,
            "La page de confirmation de suppression ne propose aucun bouton de validation");
        bouton.first().click();
    }

    @Test
    @DisplayName("Vider les liaisons d'une etape (auto-provisionnement : trois etapes enchainees)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.of("Premiere"));
        CreateStepMacroTest.run(ctx, StepDataSet.of("Deuxieme"));
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Troisieme"));
        run(ctx, StepTargetDataSet.of(1));
    }
}
