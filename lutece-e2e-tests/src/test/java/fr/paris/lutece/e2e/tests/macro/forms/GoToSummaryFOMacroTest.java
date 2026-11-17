package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
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
 * Brique macro FO : quitter la derniere etape pour le recapitulatif.
 *
 * <p>Complement de {@code ViewSummaryFO} (non modifiee), qui se met en skip si le bouton manque et ne verifie le
 * recapitulatif qu'au mieux. Ici le bouton est vise par son nom d'action {@code action_formResponseSummary}, en
 * excluant le doublon {@code aria-hidden} place avant lui. Effet prouve : la page affiche le bouton de validation
 * {@code action_doSaveResponseSummary} et plus le formulaire de l'etape. Un refus laisse l'usager sur l'etape :
 * l'echec nomme alors l'etape et les messages visibles (releve du 2026-11-06 sur forms bd4a558f : avec une question
 * obligatoire masquee par une condition d'affichage dont la pilote est a l'etape precedente, l'etape est reaffichee
 * sans message, LUT-34462, corrige par LUT-34348).</p>
 *
 * <p>Prealable : formulaire ouvert en FO sur sa derniere etape. Lit : rien. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Afficher le recapitulatif")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class GoToSummaryFOMacroTest extends MacroTest {

    /** Champs marques invalides, visibles ou non : une erreur sur une question masquee bloque sans rien montrer. */
    private static final String INVALID_JS = "els => els.map(e => e.name + (e.offsetParent === null ? ' (masque)' : '')"
        + " + ' : ' + ((e.closest('[class*=display_field_]') || e.parentElement).querySelector('.invalid-feedback') || {}).textContent.trim())";

    private static final String FO_FORM = "form#form-validate";

    @Step("Afficher le recapitulatif depuis la derniere etape FO")
    public static void run(FormsContext ctx) {
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);
        Locator form = page.locator(FO_FORM);
        Assertions.assertEquals(1, form.count(), "Le formulaire FO #form-validate doit etre affiche (url " + page.url() + ")");
        String step = form.locator("input[name='id_step']").inputValue();
        Locator summary = form.locator("button[name='action_formResponseSummary']:not([aria-hidden='true'])");
        Assertions.assertEquals(1, summary.count(), "L'etape FO " + step + " doit proposer exactement un bouton visible de recapitulatif");
        summary.click();
        page.waitForLoadState();

        Locator validate = page.locator("form button[name='action_doSaveResponseSummary']:not([aria-hidden='true'])");
        if (validate.count() != 1) {
            Locator still = page.locator(FO_FORM + " input[name='id_step']");
            Assertions.fail("Le recapitulatif doit s'afficher ; l'usager reste sur l'etape "
                + (still.count() == 1 ? still.inputValue() : "?") + " (messages visibles : "
                + page.locator(".alert:visible, .invalid-feedback:visible, .toast-body:visible").allInnerTexts()
                + " ; champs en erreur : " + page.locator(FO_FORM + " .is-invalid").evaluateAll(INVALID_JS) + ")");
        }
        Assertions.assertEquals(0, page.locator(FO_FORM + " input[name='id_step']").count(),
            "Le recapitulatif ne doit plus afficher le formulaire de saisie de l'etape");
    }

    @Test
    @DisplayName("FO : derniere etape remplie, recapitulatif affiche")
    void standaloneGoToSummaryFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont"));
            run(ctx);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
