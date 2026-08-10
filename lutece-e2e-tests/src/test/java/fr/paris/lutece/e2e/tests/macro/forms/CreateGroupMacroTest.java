package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : creer un groupe (regroupement) dans une etape.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}. Ecrit : ajoute un {@link FormsContext.GroupRef}
 * a {@code ctx.groups} (l'id memorise est l'id d'affichage {@code formDisplay.id} du groupe, celui
 * qui sert de {@code id_parent} lors d'un deplacement).</p>
 */
@Epic("Forms")
@Feature("Groupes")
@Story("Creer un groupe (regroupement)")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class CreateGroupMacroTest extends MacroTest {

    @Step("Creer un groupe (regroupement)")
    public static void run(FormsContext ctx, GroupDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty(),
            "Un formulaire et au moins une etape doivent exister avant de creer un groupe");

        FormsContext.StepRef step = ctx.lastStep();
        String title = data.title() + " " + ctx.runSuffix;
        Page page = ctx.page;

        // Navigation par URL vers le formulaire de creation de groupe. Le BO expose aussi un bouton
        // offcanvas "Ajouter un Regroupement", mais il charge ce meme formulaire dans une iframe :
        // la navigation directe est plus fiable et respecte la convention "navigation entre pages = URL".
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=createGroup&id_step=" + step.id);

        page.locator("#group-title").fill(title);

        // Groupe repetable : ouvrir l'accordeon "Gerer l'iteration du groupe" et poser le nombre maximum
        // d'iterations. Le DOM de createGroup expose l'accordeon #iterate_group (toggle ciblant
        // #iterate_groupChild) et l'input #iterationMax. iterationMax=1 (defaut) => groupe simple : on ne
        // touche a rien pour preserver le comportement historique des appelants.
        if (data.iterationMax() > 1) {
            Locator iterationToggle = page.locator("button[data-bs-target='#iterate_groupChild']");
            if (iterationToggle.count() > 0) {
                iterationToggle.first().click();
            }
            // fill attend l'actionnabilite (visibilite apres deploiement de l'accordeon).
            page.locator("#iterationMax").fill(String.valueOf(data.iterationMax()));
        }

        // Soumission : le bouton OK porte le name action_<okAction> (convention MVC Lutece = action_createGroup).
        clickCreateGroup(page);
        page.waitForLoadState();

        // Retour sur la liste des questions/groupes de l'etape : la carte du groupe doit etre presente.
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageQuestions.jsp?view=manageQuestions&id_step=" + step.id);
        Locator groupCard = page.locator("div.card-question")
            .filter(new Locator.FilterOptions().setHasText(title));
        Assertions.assertTrue(groupCard.count() > 0 && groupCard.first().isVisible(),
            "Le groupe '" + title + "' devrait apparaitre comme carte de regroupement");

        int groupDisplayId = extractGroupDisplayId(groupCard.first());
        ctx.groups.add(new FormsContext.GroupRef(groupDisplayId, title, step.id));
    }

    /** Valide le formulaire de creation : privilegie le name deterministe, sinon retombe sur le libelle. */
    private static void clickCreateGroup(Page page) {
        Locator byName = page.locator("button[name='action_createGroup']");
        if (byName.count() > 0) {
            byName.first().click();
            return;
        }
        Locator ok = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("OK"));
        if (ok.count() > 0 && ok.first().isVisible()) {
            ok.first().click();
            return;
        }
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Enregistrer")).first().click();
    }

    /** L'id de la carte de groupe a la forme "group-&lt;id_display&gt;". */
    private static int extractGroupDisplayId(Locator groupCard) {
        String id = groupCard.getAttribute("id");
        if (id != null && id.startsWith("group-")) {
            try {
                return Integer.parseInt(id.substring("group-".length()));
            } catch (NumberFormatException ignored) {
                // id non numerique : on retombe sur -1
            }
        }
        return -1;
    }

    @Test
    @DisplayName("Creer un groupe (auto-provisionnement formulaire + etape)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        run(ctx, GroupDataSet.defaults());
    }
}
