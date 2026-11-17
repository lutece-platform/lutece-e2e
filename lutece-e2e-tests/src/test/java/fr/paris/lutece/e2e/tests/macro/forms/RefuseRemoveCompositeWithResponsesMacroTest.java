package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.CompositeTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.MoveQuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Brique negative : supprimer une question ou un groupe auxquels des reponses sont rattachees est refuse.
 *
 * <p>Condition reelle ({@code FormQuestionJspBean.getConfirmRemoveComposite}) : si
 * {@code existCompositeResponses(display)}, AdminMessage de type STOP
 * ({@code forms.message.cantRemoveEntry.resourceAttached} pour une question,
 * {@code cantRemoveGroup.resourceAttached} pour un groupe) au lieu de la confirmation. Chemin reel : bouton corbeille
 * de la ligne ({@code view=getConfirmRemoveComposite&id_display=D}). Effet prouve : message de refus affiche, aucune
 * confirmation {@code action=removeComposite} proposee, l'element est toujours liste sur son etape.</p>
 *
 * <p>Lit : {@code ctx.questions} / {@code ctx.groups}. Prealable : au moins une reponse enregistree qui porte
 * l'element ({@code VerifyResponseRecorded}). Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Refus : supprimer un element qui a des reponses")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("negative")
@Tag("nonreg")
public class RefuseRemoveCompositeWithResponsesMacroTest extends MacroTest {

    static final String QUESTION_REFUSAL = "Vous ne pouvez pas supprimer cette question, des réponses utilisent déjà ce champ";
    static final String GROUP_REFUSAL = "Vous ne pouvez pas supprimer ce groupe, des réponses existent déjà";

    @Step("Refus de supprimer un element qui a des reponses")
    public static void run(FormsContext ctx, CompositeTargetDataSet data) {
        Assertions.assertFalse(ctx.responseIds.isEmpty(), "Precondition : une reponse doit etre enregistree (VerifyResponseRecorded)");
        int stepId;
        int displayId;
        String label;
        if (data.isGroup()) {
            Assertions.assertTrue(ctx.groups.size() > data.groupIndex(), "Le groupe cible doit exister (ctx.groups)");
            FormsContext.GroupRef group = ctx.groups.get(data.groupIndex());
            stepId = group.stepId;
            displayId = group.id;
            label = "groupe " + group.id;
            Assertions.assertTrue(FormsNonRegSupport.groupsOfStep(ctx, stepId).containsKey(displayId),
                "Le groupe " + displayId + " doit etre liste sur l'etape " + stepId);
        } else {
            Assertions.assertTrue(ctx.questions.size() > data.questionIndex(), "La question ciblee doit exister (ctx.questions)");
            FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
            Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu (VerifyQuestionsListed)");
            stepId = question.stepId;
            int[] place = FormsNonRegSupport.questionPlacement(ctx, stepId).get(question.id);
            Assertions.assertNotNull(place, "La question " + question.id + " doit etre listee sur l'etape " + stepId);
            displayId = place[0];
            label = "question '" + question.title + "'";
        }
        String expected = data.isGroup() ? GROUP_REFUSAL : QUESTION_REFUSAL;

        String href = FormsNonRegSupport.linkHref(ctx, "view=getConfirmRemoveComposite", "id_display", displayId);
        Assertions.assertNotNull(href, "Bouton de suppression introuvable pour la " + label);
        FormsNonRegSupport.follow(ctx, href);

        String body = ctx.page.locator("body").innerText();
        Assertions.assertTrue(body.contains(expected),
            "Refus attendu pour la " + label + " (« " + expected + " ») ; page " + ctx.page.url() + " : " + body.trim());
        Assertions.assertEquals(0, ctx.page.locator("form[action*='action=removeComposite']").count(),
            "Aucune confirmation de suppression ne doit etre proposee pour la " + label);

        if (data.isGroup()) {
            Assertions.assertTrue(FormsNonRegSupport.groupsOfStep(ctx, stepId).containsKey(displayId),
                "Le groupe " + displayId + " doit toujours etre liste");
        } else {
            Map<Integer, int[]> after = FormsNonRegSupport.questionPlacement(ctx, stepId);
            Assertions.assertTrue(after.containsKey(ctx.questions.get(data.questionIndex()).id),
                "La " + label + " doit toujours etre listee");
        }
    }

    @Test
    @DisplayName("Refus : supprimer une question puis un groupe portant une reponse")
    void standaloneRefuseRemoveCompositeWithResponses() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville"));
            VerifyQuestionsListedMacroTest.run(ctx);
            CreateGroupMacroTest.run(ctx, GroupDataSet.of("Adresse"));
            MoveQuestionMacroTest.run(ctx, MoveQuestionDataSet.toGroup(1, 0, 0));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont"));
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Ville", "Paris"));
            ViewSummaryFOMacroTest.run(ctx);
            ValidateSummaryFOMacroTest.run(ctx);
            VerifyResponseRecordedMacroTest.run(ctx, ResponseCheckDataSet.defaults());
            run(ctx, CompositeTargetDataSet.question(0));
            run(ctx, CompositeTargetDataSet.group(0));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
