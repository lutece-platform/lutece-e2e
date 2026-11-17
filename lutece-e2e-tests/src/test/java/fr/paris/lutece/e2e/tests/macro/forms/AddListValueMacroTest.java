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
 * Brique macro : ajouter une reponse possible (valeur de liste) a une question a choix.
 *
 * <p>Chemin reel : sur l'ecran de modification de la question, le bouton « Creation d'une reponse » de la carte
 * {@code #manage-field-options} ouvre en iframe l'url portee par son attribut
 * {@code data-lutece-load-content-url} ({@code ModifyEntry.jsp?...view_createField=&id_question=Q}) : champs
 * {@code #title}, {@code #value}, bouton {@code action_createField}. Effet prouve par difference d'ensembles des
 * ids de valeurs de la question (exactement une nouvelle) et par son libelle « titre > valeur ».</p>
 *
 * <p>Prerequis : ids de questions resolus ({@code VerifyQuestionsListed}). Ecrit : {@code ctx.listValueIds}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Ajouter une valeur de liste")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class AddListValueMacroTest extends MacroTest {

    @Step("Ajouter la valeur de liste {data.title} a la question {data.questionIndex}")
    public static void run(FormsContext ctx, ListValueDataSet data) {
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Map<Integer, String> before = FormsNonRegSupport.listValues(ctx, question);

        String url = ctx.page.locator("#btn-q_" + question.id + "_c").getAttribute("data-lutece-load-content-url");
        Assertions.assertNotNull(url, "Bouton de creation de reponse introuvable pour la question " + question.id);
        FormsNonRegSupport.follow(ctx, url.replace("&amp;", "&"));
        ctx.page.locator("#title").fill(data.title());
        ctx.page.locator("#value").fill(data.value());
        ctx.page.locator("button[name='action_createField']").click();
        ctx.page.waitForLoadState();

        Map<Integer, String> after = FormsNonRegSupport.listValues(ctx, question);
        List<Integer> fresh = FormsNonRegSupport.newIds(after.keySet(), new ArrayList<>(before.keySet()));
        Assertions.assertEquals(1, fresh.size(), "Exactement une valeur nouvelle attendue ; avant " + before + ", apres " + after);
        Assertions.assertEquals(data.title() + " > " + data.value(), after.get(fresh.get(0)), "Libelle de la valeur creee");
        ctx.listValueIds.put(fresh.get(0), question.id);
    }

    /** Id de la n-ieme valeur creee (par les briques) sur la question. */
    static int valueId(FormsContext ctx, FormsContext.QuestionRef question, int valueIndex) {
        List<Integer> ids = ctx.listValueIds.entrySet().stream().filter(e -> e.getValue() == question.id)
            .map(Map.Entry::getKey).toList();
        Assertions.assertTrue(valueIndex >= 0 && valueIndex < ids.size(),
            "Valeur " + valueIndex + " inconnue pour la question " + question.id + " (ctx.listValueIds : " + ids + ")");
        return ids.get(valueIndex);
    }

    @Test
    @DisplayName("Ajouter deux reponses a une question radio")
    void standaloneAddListValue() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Choix"));
            VerifyQuestionsListedMacroTest.run(ctx);
            run(ctx, ListValueDataSet.create(0, "Oui", "oui"));
            run(ctx, ListValueDataSet.create(0, "Non", "non"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
