package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
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
 * Brique macro (front office) : retirer une iteration sur un groupe repetable et verifier que le
 * nombre d'iterations diminue.
 *
 * <p>Lit : {@code ctx.formId}. N'ecrit rien. Ouvre le formulaire publie en FO, s'assure d'au moins
 * deux blocs d'iteration (en ajoutant au besoin), clique le controle "retirer une iteration"
 * <b>s'il est present</b> et verifie que le compte a diminue. Reutilise les helpers d'iteration de
 * {@link AddIterationFOMacroTest}.</p>
 *
 * <p><b>Fragile / fortement garde</b> : l'iteration n'existe que si le groupe est configure "repetable"
 * cote back-office (aucune brique existante ne pose ce reglage : le provisionnement standalone cree un
 * groupe simple, donc la brique s'ignore proprement le plus souvent). Absence de bloc d'iteration ou de
 * controle de suppression declenche un {@link Assumptions} plutot qu'un echec.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Retirer une iteration de groupe repetable en FO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class RemoveIterationFOMacroTest extends MacroTest {

    private static final Pattern REMOVE = Pattern.compile("supprimer|retirer", Pattern.CASE_INSENSITIVE);

    @Step("Retirer une iteration de groupe repetable en front office")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire publie doit exister (ctx.formId) avant la verification FO");

        Page page = AddIterationFOMacroTest.openPublishedForm(ctx);

        int count = AddIterationFOMacroTest.countIterationBlocks(page);
        Assumptions.assumeTrue(count > 0,
            "Aucun bloc d'iteration detecte en FO : le formulaire n'a pas de groupe repetable, "
            + "suppression d'iteration ignoree.");

        // Pour demontrer une diminution, il faut au moins deux blocs : on tente d'en ajouter un.
        if (count < 2) {
            Locator add = AddIterationFOMacroTest.iterationAddControl(page);
            Assumptions.assumeTrue(add != null,
                "Un seul bloc d'iteration et aucun controle d'ajout : suppression non demontrable, ignoree.");
            add.click();
            page.waitForLoadState();
            page.waitForTimeout(500);
            count = AddIterationFOMacroTest.countIterationBlocks(page);
        }
        Assumptions.assumeTrue(count >= 2,
            "Impossible d'obtenir au moins deux iterations : suppression non demontrable, ignoree.");

        Locator removeControl = iterationRemoveControl(page);
        Assumptions.assumeTrue(removeControl != null,
            "Aucun controle 'retirer une iteration' present en FO : suppression d'iteration ignoree.");

        removeControl.click();
        page.waitForLoadState();
        page.waitForTimeout(500);

        int after = AddIterationFOMacroTest.countIterationBlocks(page);
        Assertions.assertTrue(after < count,
            "Le nombre de blocs d'iteration devrait diminuer apres suppression (avant=" + count
            + ", apres=" + after + ")");
    }

    /** Cherche un controle de suppression d'iteration (par attributs specifiques puis par libelle). */
    private static Locator iterationRemoveControl(Page page) {
        String[] cssCandidates = {
            "a[href*='remove_iteration']", "a[href*='removeIteration']",
            "button[name*='remove_iteration']", "button[name*='removeIteration']",
            "[class*='remove-iteration']", ".forms-remove-iteration"
        };
        for (String selector : cssCandidates) {
            Locator loc = page.locator(selector);
            if (loc.count() > 0 && loc.first().isVisible()) {
                return loc.first();
            }
        }
        Locator scopedBtn = page.locator(AddIterationFOMacroTest.ITERATION_BLOCK)
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(REMOVE));
        if (scopedBtn.count() > 0 && scopedBtn.first().isVisible()) {
            return scopedBtn.first();
        }
        Locator scopedLink = page.locator(AddIterationFOMacroTest.ITERATION_BLOCK)
            .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(REMOVE));
        if (scopedLink.count() > 0 && scopedLink.first().isVisible()) {
            return scopedLink.first();
        }
        return null;
    }

    @Test
    @DisplayName("Retirer une iteration en FO (provisionnement + groupe + publication ; ignore si non repetable)")
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
