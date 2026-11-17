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

import java.util.List;
import java.util.Map;

/**
 * Brique macro : terminer la saisie BO depuis l'etape finale : « Voir le recapitulatif »
 * ({@code action_formResponseSummary}), puis « Valider le recapitulatif » ({@code form#summary_validate},
 * {@code action_doSaveResponseSummary}).
 *
 * <p>Effet visible : le recapitulatif disparait sans message d'erreur et l'agent revient sur une saisie vierge de
 * l'etape initiale (au moins un champ, aucun texte saisi, aucune case cochee : un test sur zero champ ne prouve rien,
 * revue du 2026-10-30, constat 12). Le BO n'affiche aucun message de confirmation (releve du 2026-10-28) :
 * l'enregistrement de la reponse se prouve ensuite dans la multivue ({@code VerifyResponseRecorded}) et ses valeurs
 * dans son detail ({@code VerifyResponseValues}). Lit : {@code ctx.steps}.</p>
 */
@Epic("Forms")
@Feature("Saisie back-office")
@Story("Valider la saisie BO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ValidateResponseBOMacroTest extends MacroTest {

    @Step("Afficher et valider le recapitulatif de la saisie BO")
    public static void run(FormsContext ctx) {
        Locator summary = ctx.page.locator(OpenFormResponseBOMacroTest.BO_FORM + " button[name='action_formResponseSummary']");
        Assertions.assertEquals(1, summary.count(), "L'etape BO courante doit proposer « Voir le recapitulatif »");
        summary.click();
        ctx.page.waitForLoadState();
        Locator validate = ctx.page.locator("form#summary_validate:has(input[name='id_form'][value='" + ctx.formId + "'])"
            + " button[name='action_doSaveResponseSummary']");
        Assertions.assertEquals(1, validate.count(), "Le recapitulatif BO doit proposer « Valider le recapitulatif » (alertes : "
            + ctx.page.locator(".alert").allInnerTexts() + ")");
        validate.click();
        ctx.page.waitForLoadState();

        Assertions.assertEquals(0, ctx.page.locator("form#summary_validate").count(),
            "Le recapitulatif ne doit plus etre affiche apres validation (alertes : " + ctx.page.locator(".alert").allInnerTexts() + ")");
        Assertions.assertEquals(0, ctx.page.locator(".alert-danger:visible").count(),
            "Aucune erreur attendue apres validation : " + ctx.page.locator(".alert-danger").allInnerTexts());
        int initial = ctx.steps.stream().filter(s -> s.initial).mapToInt(s -> s.id).findFirst().orElse(ctx.steps.get(0).id);
        OpenFormResponseBOMacroTest.assertOnBoStep(ctx, initial);
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) ctx.page.locator(OpenFormResponseBOMacroTest.BO_FORM).evaluate(
            "f => { const vis = e => !!(e.offsetWidth || e.offsetHeight);"
                + " const inputs = [...f.querySelectorAll('input, textarea, select')].filter(e => e.type !== 'hidden' && vis(e));"
                + " return { count: inputs.length,"
                + "  filled: inputs.filter(e => ['text', 'number', 'email', 'tel', 'date', 'textarea'].includes(e.type) && e.value !== '').map(e => e.name),"
                + "  checked: inputs.filter(e => (e.type === 'radio' || e.type === 'checkbox') && e.checked).map(e => e.name) }; }");
        Assertions.assertTrue(((Number) fields.get("count")).intValue() > 0, "La nouvelle saisie BO doit proposer au moins un champ");
        Assertions.assertTrue(((List<?>) fields.get("filled")).isEmpty() && ((List<?>) fields.get("checked")).isEmpty(),
            "La nouvelle saisie BO doit etre vierge : " + fields);
    }

    @Test
    @DisplayName("Saisir une reponse en BO sur deux etapes, retrouvee dans la multivue")
    void standaloneValidateResponseBO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            OpenFormResponseBOMacroTest.run(ctx);
            FillFieldBOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont BO"));
            GoToNextStepBOMacroTest.run(ctx, StepTargetDataSet.of(1));
            FillFieldBOMacroTest.run(ctx, FieldValueDataSet.text("Ville", "Paris BO"));
            run(ctx);
            VerifyResponseRecordedMacroTest.run(ctx, ResponseCheckDataSet.defaults());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
