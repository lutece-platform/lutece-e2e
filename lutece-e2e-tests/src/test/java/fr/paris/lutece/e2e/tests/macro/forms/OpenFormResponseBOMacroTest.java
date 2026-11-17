package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
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
 * Brique macro : ouvrir la saisie d'une reponse par un agent (lien « Ajouter des donnees en BO » de la liste
 * des formulaires = {@code ManageFormResponse.jsp?view=stepView&id_form=F&init=true}).
 *
 * <p>Le formulaire n'a pas besoin d'etre publie. Effet prouve : le formulaire de saisie BO
 * {@code form#form-validate} (action {@code ManageFormResponse.jsp}) est affiche pour ce formulaire, sur l'etape
 * initiale ({@code id_step}). Lit : {@code ctx.formId}, {@code ctx.steps}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Saisie back-office")
@Story("Ouvrir la saisie BO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class OpenFormResponseBOMacroTest extends MacroTest {

    static final String BO_FORM = "form#form-validate[action*='ManageFormResponse.jsp']";

    @Step("Ouvrir la saisie BO du formulaire")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty(), "Un formulaire avec des etapes doit exister");
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=manageForms&items_per_page=100000");
        String href = FormsNonRegSupport.linkHref(ctx, "ManageFormResponse.jsp", "id_form", ctx.formId);
        Assertions.assertNotNull(href, "Lien « Ajouter des donnees en BO » introuvable pour le formulaire " + ctx.formId);
        FormsNonRegSupport.follow(ctx, href);

        int initial = ctx.steps.stream().filter(s -> s.initial).mapToInt(s -> s.id).findFirst().orElse(ctx.steps.get(0).id);
        assertOnBoStep(ctx, initial);
    }

    /** Le formulaire de saisie BO du formulaire courant est affiche, sur l'etape {@code stepId}. */
    static void assertOnBoStep(FormsContext ctx, int stepId) {
        Locator form = ctx.page.locator(BO_FORM + ":has(input[name='id_form'][value='" + ctx.formId + "'])");
        Assertions.assertEquals(1, form.count(), "La saisie BO du formulaire " + ctx.formId + " doit etre affichee (url "
            + ctx.page.url() + ", alertes : " + ctx.page.locator(".alert").allInnerTexts() + ")");
        Assertions.assertEquals(String.valueOf(stepId), form.locator("input[name='id_step']").inputValue(), "Etape BO affichee");
    }

    @Test
    @DisplayName("Ouvrir la saisie BO d'un formulaire non publie")
    void standaloneOpenFormResponseBO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
