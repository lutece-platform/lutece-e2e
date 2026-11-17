package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Brique de verification : les transitions qui partent d'une etape menent exactement aux etapes attendues, une
 * transition par etape d'arrivee.
 *
 * <p>Plus forte que {@code VerifyTransitionExists}, qui prouve qu'une transition existe mais pas qu'il n'y en a
 * pas d'autre. Constat du 2026-10-29 (forms upstream {@code develop} bd4a558f) : la creation d'etape lie
 * automatiquement l'etape precedente a la nouvelle (liste « etape precedente » pre-remplie avec la derniere
 * etape, FORMS-546) ; {@code CreateTransition(A, B)} demande ensuite une cible deja liee, absente de la liste,
 * et soumet la premiere option restante : la transition creee mene a une autre etape, sans erreur. Seul un
 * oracle sur l'ensemble exact des cibles le voit.</p>
 *
 * <p>La cible d'une transition est lue sur sa carte (texte feuille egal au titre d'une etape du contexte).
 * Lit : {@code ctx.steps}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Transitions")
@Story("Verifier les transitions d'une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyTransitionTargetsMacroTest extends MacroTest {

    @Step("Verifier les etapes d'arrivee des transitions de l'etape {data.fromStepIndex}")
    public static void run(FormsContext ctx, TransitionTargetsDataSet data) {
        Assertions.assertTrue(ctx.steps.size() > data.fromStepIndex(), "L'etape source doit exister (ctx.steps)");
        FormsContext.StepRef from = ctx.steps.get(data.fromStepIndex());
        Set<String> stepTitles = ctx.steps.stream().map(s -> s.title).collect(Collectors.toSet());
        Set<String> expected = new TreeSet<>();
        for (int index : data.toStepIndices()) {
            Assertions.assertTrue(ctx.steps.size() > index, "L'etape d'arrivee " + index + " doit exister (ctx.steps)");
            expected.add(ctx.steps.get(index).title);
        }

        Map<Integer, List<String>> transitions = FormsNonRegSupport.transitionsFrom(ctx, from.id);
        Set<String> actual = new TreeSet<>();
        for (Map.Entry<Integer, List<String>> t : transitions.entrySet()) {
            List<String> targets = t.getValue().stream().filter(stepTitles::contains).distinct().collect(Collectors.toList());
            Assertions.assertEquals(1, targets.size(),
                "La transition " + t.getKey() + " doit nommer une seule etape du formulaire : " + t.getValue());
            Assertions.assertTrue(actual.add(targets.get(0)),
                "Deux transitions de '" + from.title + "' menent a '" + targets.get(0) + "' : " + transitions);
        }
        Assertions.assertEquals(expected, actual,
            "Etapes d'arrivee depuis '" + from.title + "' ; transitions lues : " + transitions);
    }

    @Test
    @DisplayName("Trois etapes creees a la suite sont liees automatiquement A -> B -> C")
    void standaloneVerifyTransitionTargets() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape B"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape C"));
            run(ctx, TransitionTargetsDataSet.of(0, 1));
            run(ctx, TransitionTargetsDataSet.of(1, 2));
            run(ctx, TransitionTargetsDataSet.of(2));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
