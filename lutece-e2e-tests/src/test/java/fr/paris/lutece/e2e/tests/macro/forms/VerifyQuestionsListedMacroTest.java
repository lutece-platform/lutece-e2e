package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Brique de verification : chaque question du contexte est listee, une seule fois, dans son etape ; son id
 * est resolu.
 *
 * <p>Se place derriere {@code AddQuestion*}, qui enregistre la question avec {@code id = 0} (constate le
 * 2026-10-27 ; {@code view=modifyQuestion&id_question=0} repond « Internal error »). Les briques qui ciblent
 * une question par son id ({@code DuplicateQuestion}, ...) exigent donc cette resolution d'abord. Lecture
 * par titre exact sur la liste des questions de l'etape ; deux questions de meme titre dans une etape font
 * echouer la brique (identite ambigue).</p>
 *
 * <p>Lit et ecrit : {@code ctx.questions[].id}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Verifier les questions listees")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyQuestionsListedMacroTest extends MacroTest {

    @Step("Verifier que chaque question du contexte est listee dans son etape et resoudre son id")
    public static void run(FormsContext ctx) {
        Assertions.assertFalse(ctx.questions.isEmpty(), "Au moins une question doit exister (ctx.questions)");
        Map<Integer, Map<Integer, String>> byStep = new HashMap<>();
        for (FormsContext.QuestionRef q : ctx.questions) {
            Assertions.assertTrue(q.stepId > 0, "La question '" + q.title + "' doit connaitre son etape");
            Map<Integer, String> listed = byStep.computeIfAbsent(q.stepId, s -> FormsNonRegSupport.questionsOfStep(ctx, s));
            List<Integer> ids = listed.entrySet().stream()
                .filter(e -> e.getValue().equals(q.title)).map(Map.Entry::getKey).collect(Collectors.toList());
            Assertions.assertEquals(1, ids.size(),
                "La question '" + q.title + "' doit etre listee exactement une fois dans l'etape " + q.stepId
                    + " ; liste : " + listed);
            if (q.id > 0) {
                Assertions.assertEquals(q.id, ids.get(0).intValue(), "Id de la question '" + q.title + "'");
            }
            q.id = ids.get(0);
        }
    }

    @Test
    @DisplayName("Deux questions ajoutees a une etape sont listees et leurs ids resolus")
    void standaloneVerifyQuestionsListed() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Choix"));
            run(ctx);
            Assertions.assertNotEquals(ctx.questions.get(0).id, ctx.questions.get(1).id, "Ids distincts");
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
