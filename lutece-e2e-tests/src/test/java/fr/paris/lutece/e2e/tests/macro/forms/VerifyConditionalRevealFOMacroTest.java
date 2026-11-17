package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.DisplayConditionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
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
 * Brique macro FO : la question cible d'une condition d'affichage apparait et disparait selon la reponse de la pilote.
 *
 * <p>Sur l'etape FO affichee : le bloc de la cible ({@code .display_field_<id>_0}) est d'abord masque, devient visible
 * quand la valeur {@code showValue} de la pilote est cochee, et se masque de nouveau avec {@code hideValue}. Les
 * valeurs sont cochees par leur libelle dans le bloc de la pilote, comme l'usager. Assertions strictes (pas
 * d'{@code Assumptions}).</p>
 *
 * <p>Complement de {@code VerifyConditionalDisplayFO} (non modifiee), qui se met en skip quand le reveal n'a pas
 * lieu : constat du 2026-10-29, ce n'etait pas une sensibilite a la version mais une condition posee sur une pilote
 * texte avec une valeur absente (cf. {@code AddDisplayCondition}). Prealable : formulaire publie et ouvert en FO
 * ({@code OpenFormFO}). Lit : {@code ctx.questions}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Affichage conditionnel")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyConditionalRevealFOMacroTest extends MacroTest {

    private static final String FO_FORM = "form#form-validate";
    private static final double WAIT_MS = 5000;

    @Step("Verifier en FO que la question {data.targetQuestionIndex} suit la reponse de la question {data.pilotQuestionIndex}")
    public static void run(FormsContext ctx, DisplayConditionDataSet data) {
        FormsContext.QuestionRef pilot = ctx.questions.get(data.pilotQuestionIndex());
        FormsContext.QuestionRef target = ctx.questions.get(data.targetQuestionIndex());
        Assertions.assertTrue(pilot.id > 0 && target.id > 0, "Les ids des questions doivent etre resolus (VerifyQuestionsListed)");
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);
        Assertions.assertEquals(1, page.locator(FO_FORM).count(), "Le formulaire FO #form-validate doit etre affiche (url " + page.url() + ")");

        Locator pilotBlock = page.locator(FO_FORM + " .display_field_" + pilot.id + "_0");
        Locator targetBlock = page.locator(FO_FORM + " .display_field_" + target.id + "_0");
        Assertions.assertEquals(1, pilotBlock.count(), "La question pilote '" + pilot.title + "' doit etre sur l'etape FO");
        Assertions.assertEquals(1, targetBlock.count(), "La question cible '" + target.title + "' doit etre rendue (masquee) sur l'etape FO");

        expectState(targetBlock, false, "avant toute reponse a '" + pilot.title + "'");
        pick(pilotBlock, data.showValue());
        expectState(targetBlock, true, "apres le choix '" + data.showValue() + "'");
        pick(pilotBlock, data.hideValue());
        expectState(targetBlock, false, "apres le choix '" + data.hideValue() + "'");
    }

    private static void pick(Locator pilotBlock, String label) {
        Locator choice = pilotBlock.getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
        Assertions.assertEquals(1, choice.count(), "La valeur '" + label + "' doit etre proposee par la pilote");
        choice.check();
    }

    private static void expectState(Locator block, boolean visible, String when) {
        try {
            block.waitFor(new Locator.WaitForOptions()
                .setState(visible ? WaitForSelectorState.VISIBLE : WaitForSelectorState.HIDDEN).setTimeout(WAIT_MS));
        } catch (PlaywrightException e) {
            Assertions.fail("La question cible doit etre " + (visible ? "affichee" : "masquee") + " " + when
                + " (style=" + block.getAttribute("style") + ")");
        }
    }

    @Test
    @DisplayName("FO : « Precisions » apparait avec Oui et disparait avec Non")
    void standaloneVerifyConditionalRevealFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Avez-vous un animal ?"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Precisions"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Oui", "oui"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Non", "non"));
            DisplayConditionDataSet condition = DisplayConditionDataSet.of(0, 1, "Oui", "Non");
            AddDisplayConditionMacroTest.run(ctx, condition);
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            run(ctx, condition);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
