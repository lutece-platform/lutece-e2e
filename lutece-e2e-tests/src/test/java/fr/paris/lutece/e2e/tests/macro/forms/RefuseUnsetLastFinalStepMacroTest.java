package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
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

import java.util.Set;

/**
 * Brique negative : retirer le statut « Finale » de la seule etape finale du formulaire est refuse.
 *
 * <p>Condition reelle ({@code FormStepJspBean.doModifyStep}) : si l'etape n'est plus finale et qu'aucune autre
 * etape du formulaire ne l'est, {@code addError(forms.error.step.missing.final)} et retour sur
 * {@code view=modifyStep}, sans enregistrement. Le message est rendu en toast Bootstrap
 * ({@code formsToastr}, {@code .toast-body}), pas en {@code .alert}. Effet prouve : le toast d'erreur est
 * affiche et l'etape porte toujours le badge « Finale » dans la liste des etapes.</p>
 *
 * <p>Lit : {@code ctx.steps}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Refus : retirer la derniere etape finale")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("negative")
@Tag("nonreg")
public class RefuseUnsetLastFinalStepMacroTest extends MacroTest {

    static final String EXPECTED_ERROR = "aucune étape finale";

    @Step("Refus de retirer le statut final de la derniere etape finale")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && ctx.steps.size() > data.stepIndex(),
            "Le formulaire et l'etape ciblee doivent exister");
        FormsContext.StepRef step = ctx.steps.get(data.stepIndex());
        Assertions.assertTrue(FormsNonRegSupport.stepBadges(ctx, step.id).contains("Finale"),
            "Precondition : l'etape '" + step.title + "' doit etre finale");
        Page page = ctx.page;

        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + step.id);
        Locator form = page.locator("form#modify_step");
        Assertions.assertEquals(1, form.count(), "Le formulaire de modification de l'etape doit etre affiche");
        form.locator("#final").uncheck();
        form.locator("button[name='action_modifyStep']").click();
        page.waitForLoadState();

        Locator toast = page.locator(".toast.bg-danger .toast-body");
        Assertions.assertEquals(1, toast.count(), "Un toast d'erreur doit signaler le refus (url " + page.url()
            + ", alertes : " + page.locator(".alert, .toast").allInnerTexts() + ")");
        Assertions.assertTrue(toast.innerText().toLowerCase().contains(EXPECTED_ERROR),
            "Message de refus attendu (« " + EXPECTED_ERROR + " ») : '" + toast.innerText() + "'");

        Set<String> badges = FormsNonRegSupport.stepBadges(ctx, step.id);
        Assertions.assertTrue(badges.contains("Finale"),
            "Le refus ne doit rien enregistrer : l'etape '" + step.title + "' reste finale, badges lus : " + badges);
    }

    @Test
    @DisplayName("Refus : decocher « Finale » sur l'unique etape finale")
    void standaloneRefuseUnsetLastFinalStep() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            run(ctx, StepTargetDataSet.of(0));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
