package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Brique macro : supprimer une reponse possible (bouton corbeille de la ligne =
 * {@code ModifyEntry.jsp?view=confirmRemoveField&id_question=Q&id_field=F}, puis confirmation AdminMessage
 * {@code action=removeField}). Effet prouve par l'absence de l'id, les autres valeurs restant en place.
 *
 * <p>Lit et ecrit : {@code ctx.listValueIds}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Supprimer une valeur de liste")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class RemoveListValueMacroTest extends MacroTest {

    @Step("Supprimer la valeur {data.valueIndex} de la question {data.questionIndex}")
    public static void run(FormsContext ctx, ListValueDataSet data) {
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        int id = AddListValueMacroTest.valueId(ctx, question, data.valueIndex());
        Map<Integer, String> before = FormsNonRegSupport.listValues(ctx, question);
        Assertions.assertTrue(before.containsKey(id), "La valeur " + id + " doit etre listee : " + before);

        String href = FormsNonRegSupport.linkHref(ctx, "view=confirmRemoveField", "id_field", id);
        Assertions.assertNotNull(href, "Bouton de suppression introuvable pour la valeur " + id);
        FormsNonRegSupport.follow(ctx, href.replaceAll("#.*$", ""));
        FormsNonRegSupport.confirmAdminMessage(ctx, "removeField");

        Map<Integer, String> after = FormsNonRegSupport.listValues(ctx, question);
        Map<Integer, String> expected = new java.util.LinkedHashMap<>(before);
        expected.remove(id);
        Assertions.assertEquals(expected, after, "Seule la valeur " + id + " doit avoir disparu");
        ctx.listValueIds.remove(id);
    }

    @Test
    @DisplayName("Supprimer une reponse d'une question radio")
    void standaloneRemoveListValue() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Choix"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Oui", "oui"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Non", "non"));
            run(ctx, ListValueDataSet.target(0, 0));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
