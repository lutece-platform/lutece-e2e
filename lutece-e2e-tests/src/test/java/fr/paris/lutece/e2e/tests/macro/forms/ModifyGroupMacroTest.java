package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupModifyDataSet;
import fr.paris.lutece.e2e.tests.macro.data.MoveQuestionDataSet;
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
 * Brique macro : renommer un groupe.
 *
 * <p>Chemin reel : bouton « modifier » de la carte du groupe ({@code #btn-modify-group-D}), qui ouvre en iframe
 * l'url de son {@code data-lutece-load-content-url} ({@code view=modifyGroup&id_step=S&id_group=G}) : champ
 * {@code #group-title}, bouton {@code action_modifyGroup}. Effet prouve sur la liste des questions de l'etape :
 * la carte {@code #group-D} (meme id_display) porte le nouveau titre.</p>
 *
 * <p>Lit et ecrit : {@code ctx.groups[].title}.</p>
 */
@Epic("Forms")
@Feature("Groupes")
@Story("Modifier un groupe")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class ModifyGroupMacroTest extends MacroTest {

    @Step("Renommer le groupe {data.groupIndex}")
    public static void run(FormsContext ctx, GroupModifyDataSet data) {
        FormsContext.GroupRef group = ctx.groups.get(data.groupIndex());
        String title = data.newTitle() + " " + ctx.runSuffix;
        Map<Integer, String> before = FormsNonRegSupport.groupsOfStep(ctx, group.stepId);
        Assertions.assertTrue(before.containsKey(group.id), "Le groupe " + group.id + " doit etre liste : " + before);

        String url = ctx.page.locator("#btn-modify-group-" + group.id).getAttribute("data-lutece-load-content-url");
        Assertions.assertNotNull(url, "Bouton de modification introuvable pour le groupe " + group.id);
        FormsNonRegSupport.follow(ctx, url.replace("&amp;", "&"));
        ctx.page.locator("#group-title").fill(title);
        ctx.page.locator("button[name='action_modifyGroup']").click();
        ctx.page.waitForLoadState();

        Map<Integer, String> after = FormsNonRegSupport.groupsOfStep(ctx, group.stepId);
        Assertions.assertEquals(before.keySet(), after.keySet(), "La modification ne doit ni creer ni supprimer de groupe");
        Assertions.assertEquals(title, after.get(group.id), "Titre relu du groupe " + group.id);
        group.title = title;
    }

    @Test
    @DisplayName("Renommer un groupe")
    void standaloneModifyGroup() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            CreateGroupMacroTest.run(ctx, GroupDataSet.of("Groupe source"));
            run(ctx, GroupModifyDataSet.of(0, "Groupe renomme"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
