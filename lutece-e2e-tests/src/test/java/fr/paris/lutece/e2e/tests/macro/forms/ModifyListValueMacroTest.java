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
 * Brique macro : modifier le libelle et la valeur d'une reponse possible.
 *
 * <p>Chemin reel : bouton « Modifier » de la ligne (offcanvas en iframe, url {@code view=modifyField&id_field=F}
 * portee par {@code data-lutece-load-content-url}), champs {@code #title}, {@code #value}, bouton
 * {@code action_modifyField}. Effet prouve sur la carte de la question : meme id, nouveau libelle.</p>
 *
 * <p>Lit : {@code ctx.listValueIds}. Dataset : {@link ListValueDataSet#modify}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Modifier une valeur de liste")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ModifyListValueMacroTest extends MacroTest {

    @Step("Modifier la valeur {data.valueIndex} de la question {data.questionIndex}")
    public static void run(FormsContext ctx, ListValueDataSet data) {
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        int id = AddListValueMacroTest.valueId(ctx, question, data.valueIndex());
        Map<Integer, String> before = FormsNonRegSupport.listValues(ctx, question);
        Assertions.assertTrue(before.containsKey(id), "La valeur " + id + " doit etre listee : " + before);

        String url = (String) ctx.page.evaluate("id => { const a = [...document.querySelectorAll(\"#manage-field-options "
            + "[data-lutece-load-content-url*='view=modifyField']\")].find(e => new URLSearchParams(e.getAttribute("
            + "'data-lutece-load-content-url').split('?')[1]).get('id_field') === String(id)); return a ? "
            + "a.getAttribute('data-lutece-load-content-url') : null; }", id);
        Assertions.assertNotNull(url, "Bouton de modification introuvable pour la valeur " + id);
        FormsNonRegSupport.follow(ctx, url.replace("&amp;", "&"));
        Assertions.assertEquals(1, ctx.page.locator("input[name='id_field'][value='" + id + "']").count(),
            "L'ecran de modification de la valeur " + id + " doit etre affiche");
        ctx.page.locator("#title").fill(data.title());
        ctx.page.locator("#value").fill(data.value());
        ctx.page.locator("button[name='action_modifyField']").click();
        ctx.page.waitForLoadState();

        Map<Integer, String> after = FormsNonRegSupport.listValues(ctx, question);
        Assertions.assertEquals(before.keySet(), after.keySet(), "La modification ne doit ni creer ni supprimer de valeur");
        Assertions.assertEquals(data.title() + " > " + data.value(), after.get(id), "Libelle relu de la valeur " + id);
    }

    @Test
    @DisplayName("Modifier une reponse d'une question radio")
    void standaloneModifyListValue() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Choix"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Oui", "oui"));
            run(ctx, ListValueDataSet.modify(0, 0, "Oui certainement", "oui_c"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
