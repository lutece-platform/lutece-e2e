package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionModifyDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionTargetsDataSet;
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
 * Brique macro : changer l'etape d'arrivee d'une transition.
 *
 * <p>Chemin reel : le bouton « modifier » de la transition ouvre en iframe
 * {@code ManageTransitions.jsp?view=modifyTransition&id_step=S&id_transition=T} (form {@code #modify_transition},
 * {@code select#nextStep}, bouton {@code action_modifyTransition}). Effet prouve sur la liste des transitions de
 * l'etape source : la meme transition (meme id) mene a la nouvelle etape, plus aucune ne mene a l'ancienne.</p>
 *
 * <p>Constat du 2026-10-29 : la liste {@code nextStep} exclut toute etape deja ciblee depuis l'etape source, y
 * compris la cible actuelle de la transition modifiee ({@code getTransitionTargetStepReferenceList}) ; quand
 * toutes les autres etapes sont deja liees, la liste est vide et la transition ne peut pas etre enregistree.</p>
 *
 * <p>Lit : {@code ctx.steps}. Ecrit : rien (l'id de transition ne change pas).</p>
 */
@Epic("Forms")
@Feature("Transitions")
@Story("Modifier une transition")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ModifyTransitionMacroTest extends MacroTest {

    @Step("Changer l'etape d'arrivee de la transition {data.fromStepIndex} -> {data.currentToIndex}")
    public static void run(FormsContext ctx, TransitionModifyDataSet data) {
        FormsContext.StepRef from = ctx.steps.get(data.fromStepIndex());
        FormsContext.StepRef current = ctx.steps.get(data.currentToIndex());
        FormsContext.StepRef target = ctx.steps.get(data.newToIndex());

        int transitionId = transitionTo(FormsNonRegSupport.transitionsFrom(ctx, from.id), current.title);
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageTransitions.jsp?view=modifyTransition&id_step="
            + from.id + "&id_transition=" + transitionId);
        Locator form = ctx.page.locator("form#modify_transition:has(input[name='id_transition'][value='" + transitionId + "'])");
        Assertions.assertEquals(1, form.count(), "L'ecran de modification de la transition " + transitionId + " doit etre affiche");
        Locator option = form.locator("select#nextStep option[value='" + target.id + "']");
        Assertions.assertEquals(1, option.count(), "L'etape '" + target.title + "' doit etre proposee comme arrivee ; options : "
            + form.locator("select#nextStep option").allTextContents());
        form.locator("select#nextStep").selectOption(new SelectOption().setValue(String.valueOf(target.id)));
        form.locator("button[name='action_modifyTransition']").click();
        ctx.page.waitForLoadState();

        Map<Integer, List<String>> after = FormsNonRegSupport.transitionsFrom(ctx, from.id);
        Assertions.assertTrue(after.containsKey(transitionId), "La transition " + transitionId + " doit toujours exister : " + after);
        Assertions.assertTrue(after.get(transitionId).contains(target.title),
            "La transition " + transitionId + " doit mener a '" + target.title + "' : " + after.get(transitionId));
        Assertions.assertTrue(after.values().stream().noneMatch(l -> l.contains(current.title)),
            "Plus aucune transition de '" + from.title + "' ne doit mener a '" + current.title + "' : " + after);
    }

    private static int transitionTo(Map<Integer, List<String>> transitions, String title) {
        List<Integer> ids = transitions.entrySet().stream().filter(e -> e.getValue().contains(title))
            .map(Map.Entry::getKey).toList();
        Assertions.assertEquals(1, ids.size(), "Une seule transition vers '" + title + "' attendue : " + transitions);
        return ids.get(0);
    }

    @Test
    @DisplayName("Rediriger la transition automatique A -> B vers C")
    void standaloneModifyTransition() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape B"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape C"));
            run(ctx, TransitionModifyDataSet.of(0, 1, 2));
            VerifyTransitionTargetsMacroTest.run(ctx, TransitionTargetsDataSet.of(0, 2));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
