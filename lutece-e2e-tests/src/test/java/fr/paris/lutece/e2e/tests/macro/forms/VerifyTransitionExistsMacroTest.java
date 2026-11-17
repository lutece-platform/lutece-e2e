package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionDataSet;
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
import java.util.stream.Collectors;

/**
 * Brique de verification : une transition de l'etape source vers l'etape cible existe, et une seule.
 *
 * <p>Lit les cartes de la liste des transitions de l'etape source (id par parametre strict sur le lien
 * {@code view=confirmRemoveTransition&id_transition=N}) et retient celles dont un texte feuille est
 * <b>exactement</b> le titre de l'etape cible. L'id resolu est ajoute a {@code ctx.transitionIds}.</p>
 *
 * <p>Prouve l'existence, pas la creation : la creation d'une etape lie deja l'etape precedente a la nouvelle
 * (FORMS-546), et {@code CreateTransition} vers une cible deja liee soumet une autre cible sans erreur. Derriere
 * {@code CreateTransition}, encadrer par {@code VerifyTransitionTargets} : ensemble exact sans la cible avant,
 * avec elle apres (revue adversariale du 2026-10-30, constat 4).</p>
 */
@Epic("Forms")
@Feature("Transitions")
@Story("Verifier l'existence d'une transition")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyTransitionExistsMacroTest extends MacroTest {

    @Step("Verifier la transition entre deux etapes")
    public static void run(FormsContext ctx, TransitionDataSet data) {
        Assertions.assertTrue(ctx.steps.size() > Math.max(data.fromStepIndex(), data.toStepIndex()),
            "Les etapes source et cible doivent exister (ctx.steps)");
        FormsContext.StepRef from = ctx.steps.get(data.fromStepIndex());
        FormsContext.StepRef to = ctx.steps.get(data.toStepIndex());

        Map<Integer, List<String>> transitions = FormsNonRegSupport.transitionsFrom(ctx, from.id);
        List<Integer> matching = transitions.entrySet().stream()
            .filter(e -> e.getValue().contains(to.title))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        Assertions.assertEquals(1, matching.size(),
            "Exactement une transition '" + from.title + "' -> '" + to.title + "' attendue ; transitions lues : "
                + transitions);
        Integer id = matching.get(0);
        if (!ctx.transitionIds.contains(id)) {
            ctx.transitionIds.add(id);
        }
    }

    @Test
    @DisplayName("Verifier une transition A -> C creee par CreateTransition, absente avant")
    void standaloneVerifyTransitionExists() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape B"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape C"));
            // Liaisons automatiques A -> B -> C : A -> C n'existe pas encore.
            VerifyTransitionTargetsMacroTest.run(ctx, TransitionTargetsDataSet.of(0, 1));
            CreateTransitionMacroTest.run(ctx, TransitionDataSet.of(0, 2));
            VerifyTransitionTargetsMacroTest.run(ctx, TransitionTargetsDataSet.of(0, 1, 2));
            run(ctx, TransitionDataSet.of(0, 2));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
