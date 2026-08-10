package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.pages.bo.FormsCreationPage;
import fr.paris.lutece.e2e.pages.bo.FormsListPage;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : creer un formulaire.
 *
 * <p>Lit : rien. Ecrit : {@code ctx.formId}, {@code ctx.formTitle}.</p>
 */
@Epic("Forms")
@Feature("Cycle de vie du formulaire")
@Story("Creer un formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class CreateFormMacroTest extends MacroTest {

    /**
     * Cree un formulaire (titre rendu unique via le suffixe du contexte), le verifie dans la liste
     * et memorise son id dans le contexte.
     */
    @Step("Creer le formulaire")
    public static void run(FormsContext ctx, FormDataSet data) {
        String title = data.title() + " " + ctx.runSuffix;

        FormsListPage list = new FormsListPage(ctx.page, ctx.baseUrl).navigateTo();
        FormsCreationPage creation = list.clickAddForm();
        creation.fillTitle(title);
        creation.setStartDate(data.startDate());
        creation.setEndDate(data.endDate());
        if (data.workflowName() != null && !data.workflowName().isBlank()) {
            creation.selectWorkflow(data.workflowName());
        }
        creation.clickCreateForm();
        ctx.page.waitForLoadState();

        int id = MacroSupport.extractFormId(ctx, title);
        Assertions.assertTrue(id > 0,
            "Le formulaire '" + title + "' devrait apparaitre dans la liste avec un id_form");
        ctx.formId = id;
        ctx.formTitle = title;
    }

    @Test
    @DisplayName("Creer un formulaire simple")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        run(ctx, FormDataSet.defaults());
    }
}
