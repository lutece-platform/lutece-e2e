package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brique macro : renommer une etape par ses parametres (lien « Parametres de l'etape » =
 * {@code ManageTransitions.jsp?view=manageTransitions&id_step=S}, formulaire {@code #modify_step}, champ
 * {@code #step-title}, bouton {@code action_modifyStep}). Effet prouve sur la liste des etapes : meme id, nouveau
 * titre, les autres etapes inchangees.
 *
 * <p>Complement de {@code ModifyStep}, dont l'oracle ({@code getByText} sur la carte) echoue sur forms upstream
 * {@code develop} alors que le renommage est bien enregistre (constate le 2026-10-29). Dataset :
 * {@link StepTargetDataSet#of(int, String)} ; le suffixe d'execution n'est pas ajoute (titre d'etape metier).
 * Ecrit : {@code ctx.steps[i].title}.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Renommer une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class RenameStepMacroTest extends MacroTest {

    @Step("Renommer l'etape {data.stepIndex} en {data.newTitle}")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        FormsContext.StepRef step = ctx.steps.get(data.stepIndex());
        Map<Integer, String> before = FormsNonRegSupport.stepsOfForm(ctx);

        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + step.id);
        Locator form = ctx.page.locator("form#modify_step:has(input[name='id_step'][value='" + step.id + "'])");
        Assertions.assertEquals(1, form.count(), "Les parametres de l'etape " + step.id + " doivent etre affiches");
        form.locator("#step-title").fill(data.newTitle());
        form.locator("button[name='action_modifyStep']").click();
        ctx.page.waitForLoadState();

        Map<Integer, String> after = FormsNonRegSupport.stepsOfForm(ctx);
        Map<Integer, String> expected = new java.util.LinkedHashMap<>(before);
        expected.put(step.id, data.newTitle());
        Assertions.assertEquals(expected, after, "Seul le titre de l'etape " + step.id + " doit avoir change");
        step.title = data.newTitle();
    }

    @Test
    @DisplayName("Renommer une etape par ses parametres")
    void standaloneRenameStep() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape source"));
            run(ctx, StepTargetDataSet.of(0, "Etape renommee"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
