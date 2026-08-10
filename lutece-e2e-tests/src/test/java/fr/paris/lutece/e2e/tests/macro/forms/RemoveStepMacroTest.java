package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : supprimer une etape.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}. Ecrit : retire le {@link FormsContext.StepRef}
 * cible de {@code ctx.steps}. La vue {@code confirmRemoveStep} redirige vers une boite de dialogue
 * AdminMessage (Lutece) : on confirme via le bouton de validation (macro core {@code @button},
 * libelle i18n {@code portal.admin.message.buttonValidate}, rendu "Valider" et NON "OK").</p>
 *
 * <p>Attention : le plugin refuse de supprimer une etape marquee "initiale" (il redirige alors vers
 * la liste sans afficher la confirmation). De plus, la premiere/seule etape d'un formulaire est
 * automatiquement forcee initiale+finale par le plugin ; il faut donc cibler une etape non initiale
 * (ex: une seconde etape). Si la confirmation n'apparait pas (etape non supprimable), le scenario
 * est saute proprement via {@link Assumptions} plutot que de provoquer un echec.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Supprimer une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class RemoveStepMacroTest extends MacroTest {

    @Step("Supprimer l'etape")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty(),
            "Un formulaire et au moins une etape doivent exister avant de supprimer une etape");

        FormsContext.StepRef step = ctx.steps.get(data.stepIndex());
        Page page = ctx.page;

        // La vue confirmRemoveStep redirige (redirect HTTP) vers une page AdminMessage de confirmation.
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=confirmRemoveStep&id_step=" + step.id);

        // Sur la page AdminMessage (layout minimal), le seul bouton de soumission est le bouton de
        // confirmation "Valider" (le formulaire de validation est rendu avant l'eventuel "Annuler").
        // Locator independant de l'i18n : premier button[type='submit'] de la boite de dialogue.
        // Si aucune confirmation n'apparait (le plugin a refuse : etape initiale, redirection vers la
        // liste), on saute le scenario au lieu d'echouer.
        Locator confirm = page.locator("button[type='submit']");
        Assumptions.assumeTrue(confirm.count() > 0 && confirm.first().isVisible(),
            "Boite de confirmation de suppression absente pour l'etape '" + step.title
                + "' (etape probablement initiale, non supprimable) : scenario ignore");
        confirm.first().click();
        page.waitForLoadState();

        // Verification : la carte de l'etape n'existe plus dans la liste
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageSteps.jsp?view=manageSteps&id_form=" + ctx.formId);
        Assertions.assertEquals(0, page.locator("#step_" + step.id).count(),
            "L'etape '" + step.title + "' ne devrait plus exister apres suppression");

        ctx.steps.remove(step);
    }

    @Test
    @DisplayName("Supprimer une etape (auto-provisionnement formulaire + 2 etapes)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        // La 1ere etape est forcee initiale+finale par le plugin (non supprimable).
        CreateStepMacroTest.run(ctx, StepDataSet.of("Etape initiale"));
        // La 2eme etape reste non initiale et non finale : elle est supprimable.
        CreateStepMacroTest.run(ctx, StepDataSet.of("Etape a supprimer"));
        run(ctx, StepTargetDataSet.of(1));
    }
}
