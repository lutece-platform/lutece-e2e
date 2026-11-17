package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.MaxResponsesDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : regler le nombre maximum de reponses du formulaire (0 = pas de limite).
 *
 * <p>Chemin reel : ecran de modification du formulaire ({@code view=modifyForm}, form {@code #modify_form}), champ
 * « Nombre maximum de reponses » ({@code #maxNumberResponse}), bouton « Modifier le formulaire »
 * ({@code action_modifyForm}) ; retour sur la liste avec « Formulaire modifie » (releve du 2026-11-09). Effet prouve en
 * rouvrant l'ecran : la valeur enregistree est relue et le titre est inchange.</p>
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.formTitle}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Formulaire")
@Story("Limiter le nombre de reponses")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class SetMaxResponsesMacroTest extends MacroTest {

    @Step("Regler le nombre maximum de reponses du formulaire a {data.max}")
    public static void run(FormsContext ctx, MaxResponsesDataSet data) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        Locator form = openModifyForm(ctx);
        form.locator("#maxNumberResponse").fill(String.valueOf(data.max()));
        form.locator("button[name='action_modifyForm']").click();
        ctx.page.waitForLoadState();
        Assertions.assertTrue(ctx.page.locator(".alert").allInnerTexts().stream().anyMatch(t -> t.contains("Formulaire modifi")),
            "La modification du formulaire doit etre confirmee (url " + ctx.page.url() + ", alertes : "
                + ctx.page.locator(".alert").allInnerTexts() + ")");

        form = openModifyForm(ctx);
        Assertions.assertEquals(String.valueOf(data.max()), form.locator("#maxNumberResponse").inputValue(),
            "Nombre maximum de reponses relu du formulaire " + ctx.formId);
        Assertions.assertEquals(ctx.formTitle, form.locator("#title").inputValue(), "Le titre du formulaire doit etre inchange");
    }

    private static Locator openModifyForm(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + ctx.formId);
        Locator form = ctx.page.locator("form#modify_form:has(input[name='id_form'][value='" + ctx.formId + "'])");
        Assertions.assertEquals(1, form.count(), "L'ecran de modification du formulaire " + ctx.formId + " doit etre affiche");
        return form;
    }

    @Test
    @DisplayName("Limiter un formulaire a 3 reponses puis lever la limite")
    void standaloneSetMaxResponses() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            run(ctx, MaxResponsesDataSet.of(3));
            run(ctx, MaxResponsesDataSet.unlimited());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
