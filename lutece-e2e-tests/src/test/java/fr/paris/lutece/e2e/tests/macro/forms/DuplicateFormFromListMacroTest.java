package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brique macro : dupliquer le formulaire depuis la liste (menu Actions, entree « Dupliquer » = formulaire POST
 * {@code action=duplicateForm}, {@code id_form}, {@code _csrftoken}).
 *
 * <p>Effet prouve par difference d'ensembles des ids de la liste (exactement un formulaire nouveau) ; son titre
 * est relu. La conformite du contenu se prouve ensuite par {@code VerifyFormStructureEqual}. Complement de
 * {@code DuplicateForm}, qui cherche un lien GET et se met en skip sur forms upstream {@code develop}
 * (LUT-34208, constate le 2026-10-29).</p>
 *
 * <p>Lit : {@code ctx.formId}. Ecrit : {@code ctx.duplicatedFormId}, {@code ctx.createdForms}.</p>
 */
@Epic("Forms")
@Feature("Copie")
@Story("Dupliquer un formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class DuplicateFormFromListMacroTest extends MacroTest {

    @Step("Dupliquer le formulaire depuis la liste")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        Set<Integer> before = FormsNonRegSupport.formIds(ctx);
        Assertions.assertTrue(before.contains(ctx.formId), "Le formulaire " + ctx.formId + " doit etre liste");

        FormsNonRegSupport.clickInDropdown(ctx, ctx.page.locator("form:has(input[name='action'][value='duplicateForm'])"
            + ":has(input[name='id_form'][value='" + ctx.formId + "']) button[type='submit']"), "Dupliquer " + ctx.formId);

        List<Integer> fresh = FormsNonRegSupport.newIds(FormsNonRegSupport.formIds(ctx), new ArrayList<>(before));
        FormsNonRegSupport.registerCreatedForms(ctx, fresh);
        Assertions.assertEquals(1, fresh.size(), "Exactement un formulaire nouveau attendu apres duplication : " + fresh);
        int copy = fresh.get(0);
        Assertions.assertTrue(ctx.createdForms.containsKey(copy), "Titre de la copie " + copy + " derive de '" + ctx.formTitle
            + "' attendu : '" + FormsNonRegSupport.formTitle(ctx, copy) + "'");
        ctx.duplicatedFormId = copy;
    }

    @Test
    @DisplayName("Dupliquer un formulaire a deux etapes depuis la liste")
    void standaloneDuplicateFormFromList() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Identite"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Coordonnees"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Civilite"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            run(ctx);
            VerifyFormStructureEqualMacroTest.run(ctx, ctx.duplicatedFormId);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
