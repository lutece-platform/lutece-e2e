package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepFlagsDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionTargetsDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Brique de verification : la soumission a produit exactement N reponses nouvelles, visibles dans la multivue.
 *
 * <p>Se place derriere {@code ValidateSummaryFO} (ou une saisie BO). Sans message de fin, Forms redirige
 * l'usager vers le formulaire : l'interface FO ne prouve donc rien, et {@code ValidateSummaryFO} laisse
 * {@code ctx.lastResponseId} vide. La preuve est cote BO : ids des lignes de la multivue
 * ({@code data-url=...id_form_response=N}), par difference avec {@code ctx.responseIds} (§4-bis.3).</p>
 *
 * <p>La multivue lit un index Lucene alimente par un demon : la brique peut d'abord lancer l'indexation
 * incrementale depuis l'ecran d'indexation (chemin reel), puis relit jusqu'a {@code timeoutSeconds}.</p>
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.responseIds}. Ecrit : ajoute les ids nouveaux a
 * {@code ctx.responseIds} et pose {@code ctx.lastResponseId} sur le plus grand.</p>
 */
@Epic("Forms")
@Feature("Reponses")
@Story("Verifier l'enregistrement d'une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyResponseRecordedMacroTest extends MacroTest {

    private static final int POLL_MS = 5_000;

    @Step("Verifier que {data.expectedNewResponses} reponse(s) nouvelle(s) apparaissent dans la multivue")
    public static void run(FormsContext ctx, ResponseCheckDataSet data) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");

        if (data.triggerIndexing()) {
            FormsNonRegSupport.triggerIncrementalIndexing(ctx);
        }
        long deadline = System.currentTimeMillis() + data.timeoutSeconds() * 1000L;
        List<Integer> fresh;
        Set<Integer> listed;
        while (true) {
            listed = FormsNonRegSupport.multiviewResponseIds(ctx);
            fresh = FormsNonRegSupport.newIds(listed, ctx.responseIds);
            if (fresh.size() >= data.expectedNewResponses() || System.currentTimeMillis() >= deadline) {
                break;
            }
            ctx.page.waitForTimeout(POLL_MS);
        }
        Assertions.assertEquals(data.expectedNewResponses(), fresh.size(),
            "Reponses nouvelles attendues dans la multivue du formulaire " + ctx.formId + " apres "
                + data.timeoutSeconds() + " s ; ids listes : " + listed + ", deja connus : " + ctx.responseIds);
        ctx.responseIds.addAll(fresh);
        if (!fresh.isEmpty()) {
            ctx.lastResponseId = Collections.max(fresh);
        }
    }

    @Test
    @DisplayName("Soumission FO d'un formulaire a deux etapes, reponse retrouvee dans la multivue")
    void standaloneVerifyResponseRecorded() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            VerifyStepFlagsMacroTest.run(ctx, StepFlagsDataSet.initialOnly(0));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            // A -> B existe deja par la liaison automatique a la creation de B (FORMS-546) : oracle exact.
            VerifyTransitionTargetsMacroTest.run(ctx, TransitionTargetsDataSet.of(0, 1));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());

            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont"));
            GoToNextStepFOMacroTest.run(ctx, StepTargetDataSet.of(1));
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Ville", "Paris"));
            ViewSummaryFOMacroTest.run(ctx);
            ValidateSummaryFOMacroTest.run(ctx);

            run(ctx, ResponseCheckDataSet.defaults());
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
