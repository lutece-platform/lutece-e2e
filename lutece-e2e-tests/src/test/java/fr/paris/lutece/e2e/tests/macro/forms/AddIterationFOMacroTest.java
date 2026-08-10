package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

/**
 * Brique macro (front office) : ajouter une iteration sur un groupe repetable et verifier que le
 * nombre d'iterations augmente.
 *
 * <p>Lit : {@code ctx.formId}. N'ecrit rien. Ouvre le formulaire publie en FO, compte les blocs
 * d'iteration, clique le controle "ajouter une iteration" <b>s'il est present</b> et verifie que le
 * compte a augmente.</p>
 *
 * <p><b>Fragile / fortement garde</b> : l'iteration n'existe que si le groupe est configure "repetable"
 * cote back-office (aucune brique existante ne pose ce reglage : le provisionnement standalone cree un
 * groupe simple, donc la brique s'ignore proprement le plus souvent). Absence de bloc d'iteration ou de
 * controle d'ajout declenche un {@link Assumptions} plutot qu'un echec.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Ajouter une iteration de groupe repetable en FO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddIterationFOMacroTest extends MacroTest {

    /**
     * Selecteur large de blocs d'iteration (markup FO variable selon la version du plugin).
     *
     * <p>Sur cette version, le template FO {@code view_group.html} rend chaque iteration comme
     * {@code <fieldset class="step-group-item" id="group_<id>_<index>">} : c'est le selecteur principal.
     * Les autres classes/attributs restent en repli pour d'eventuelles autres versions de markup.</p>
     */
    static final String ITERATION_BLOCK =
        ".step-group-item, .form-iteration, .iteration, [class*='iteration'], [data-iteration], [id*='iteration']";

    private static final Pattern ADD = Pattern.compile("ajouter", Pattern.CASE_INSENSITIVE);

    @Step("Ajouter une iteration de groupe repetable en front office")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire publie doit exister (ctx.formId) avant la verification FO");

        Page page = openPublishedForm(ctx);

        int before = countIterationBlocks(page);
        Assumptions.assumeTrue(before > 0,
            "Aucun bloc d'iteration detecte en FO : le formulaire n'a pas de groupe repetable, "
            + "ajout d'iteration ignore.");

        Locator addControl = iterationAddControl(page);
        Assumptions.assumeTrue(addControl != null,
            "Aucun controle 'ajouter une iteration' present en FO : ajout d'iteration ignore.");

        addControl.click();
        page.waitForLoadState();
        page.waitForTimeout(500);

        int after = countIterationBlocks(page);
        Assertions.assertTrue(after > before,
            "Le nombre de blocs d'iteration devrait augmenter apres ajout (avant=" + before
            + ", apres=" + after + ")");
    }

    // === Navigation FO ===

    static Page openPublishedForm(FormsContext ctx) {
        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId);
        Page page = ctx.page;
        page.waitForLoadState();
        dismissOffcanvas(page);
        return page;
    }

    static void dismissOffcanvas(Page page) {
        Locator backdrop = page.locator(".offcanvas-backdrop");
        if (backdrop.count() > 0) {
            page.keyboard().press("Escape");
            try {
                backdrop.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(3000));
            } catch (RuntimeException ignored) {
                // backdrop persistant : on continue au mieux
            }
        }
    }

    // === Iterations ===

    static int countIterationBlocks(Page page) {
        try {
            return page.locator(ITERATION_BLOCK).count();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Cherche un controle d'ajout d'iteration (par attributs specifiques puis par libelle). */
    static Locator iterationAddControl(Page page) {
        String[] cssCandidates = {
            "a[href*='add_iteration']", "a[href*='addIteration']",
            "button[name*='add_iteration']", "button[name*='addIteration']",
            "[class*='add-iteration']", ".forms-add-iteration"
        };
        for (String selector : cssCandidates) {
            Locator loc = page.locator(selector);
            if (loc.count() > 0 && loc.first().isVisible()) {
                return loc.first();
            }
        }
        // Repli par libelle : bouton/lien "Ajouter" situe dans un bloc d'iteration.
        Locator iterationScopedAdd = page.locator(ITERATION_BLOCK)
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ADD));
        if (iterationScopedAdd.count() > 0 && iterationScopedAdd.first().isVisible()) {
            return iterationScopedAdd.first();
        }
        Locator iterationScopedAddLink = page.locator(ITERATION_BLOCK)
            .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(ADD));
        if (iterationScopedAddLink.count() > 0 && iterationScopedAddLink.first().isVisible()) {
            return iterationScopedAddLink.first();
        }
        return null;
    }

    @Test
    @DisplayName("Ajouter une iteration en FO (provisionnement + groupe + publication ; ignore si non repetable)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question iterable"));
        // Groupe repetable + question deplacee dedans : sans membre, le groupe FO ne rend aucun bloc
        // d'iteration (view_group.html n'itere que sur les enfants du groupe).
        CreateGroupMacroTest.run(ctx, GroupDataSet.repeatable("Macro Groupe repetable"));
        MoveQuestionIntoGroupMacroTest.run(ctx, GroupTargetDataSet.of(0, 0));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        run(ctx);
    }
}
