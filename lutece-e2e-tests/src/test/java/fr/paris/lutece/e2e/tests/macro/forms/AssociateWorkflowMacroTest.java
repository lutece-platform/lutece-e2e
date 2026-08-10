package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.WorkflowRefDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Brique macro : associer un workflow a un formulaire (page modifyForm, menu {@code #idWorkflow}).
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.workflowName} (preference optionnelle). Ecrit :
 * {@code ctx.workflowName} (libelle du workflow reellement associe).</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Associer un workflow")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AssociateWorkflowMacroTest extends MacroTest {

    @Step("Associer un workflow au formulaire")
    public static void run(FormsContext ctx, WorkflowRefDataSet data) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant d'associer un workflow");

        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId);

        Locator select = page.locator("#idWorkflow");
        Assumptions.assumeTrue(select.count() > 0 && select.first().isVisible(),
            "Le menu de selection du workflow (#idWorkflow) est absent : association non pilotable");

        // Preference : ctx.workflowName si deja pose, sinon la valeur du dataset
        String preferred = (data.workflowName() != null && !data.workflowName().isBlank())
            ? data.workflowName()
            : ctx.workflowName;

        List<String> options = select.locator("option").allTextContents();
        String target = chooseWorkflow(options, preferred);
        Assumptions.assumeTrue(target != null,
            "Aucun workflow disponible a associer (options: " + options + ")");
        String label = target.trim();

        select.first().selectOption(new SelectOption().setLabel(label));
        submitModify(page);
        page.waitForLoadState();

        // Verifier la persistance : recharger modifyForm et lire l'option selectionnee
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId);
        Locator reloaded = page.locator("#idWorkflow").locator("option:checked");
        Assertions.assertTrue(reloaded.count() > 0, "Une option de workflow devrait etre selectionnee apres soumission");
        String selected = safe(reloaded.first().textContent()).trim();
        Assertions.assertEquals(label, selected,
            "Le workflow associe devrait etre '" + label + "' (obtenu: '" + selected + "')");

        ctx.workflowName = label;
    }

    private static String chooseWorkflow(List<String> options, String preferred) {
        if (preferred != null && !preferred.isBlank()) {
            for (String opt : options) {
                if (opt != null && (opt.trim().equalsIgnoreCase(preferred.trim()) || opt.contains(preferred))) {
                    return opt;
                }
            }
            return null;
        }
        // Pas de preference : premier libelle non vide et non placeholder
        for (String opt : options) {
            if (opt == null) {
                continue;
            }
            String t = opt.trim();
            if (t.isEmpty() || t.equals("-") || t.startsWith("--")) {
                continue;
            }
            return opt;
        }
        return null;
    }

    private static void submitModify(Page page) {
        Locator byName = page.locator("button[name='action_modifyForm']");
        if (byName.count() > 0 && byName.first().isVisible()) {
            byName.first().click();
            return;
        }
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Modifier le formulaire")).first().click();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Test
    @DisplayName("Associer un workflow a un formulaire (auto-provisionnement du formulaire)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        run(ctx, WorkflowRefDataSet.defaults());
    }
}
