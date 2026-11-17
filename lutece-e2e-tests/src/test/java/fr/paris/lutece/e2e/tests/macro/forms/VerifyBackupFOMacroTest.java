package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.BackupValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormOptionsDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FrontUserDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
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

/**
 * Brique macro FO : rouvrir le formulaire depuis le debut ({@code init=true}) et verifier la reprise (ou l'absence)
 * du brouillon.
 *
 * <p>{@code restored=true} : la valeur sauvegardee est rechargee et « Reprise de la sauvegarde du ... » est affiche
 * (usager reconnecte, nouvelle session). {@code restored=false} : champ vide et pas de message de reprise (usager
 * deconnecte : le brouillon appartient a l'usager, pas au navigateur). Assertions strictes. Lit :
 * {@code ctx.formId}, {@code ctx.questions}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Reprendre un brouillon")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyBackupFOMacroTest extends MacroTest {

    @Step("Rouvrir le formulaire : brouillon '{data.value}' attendu restaure = {data.restored}")
    public static void run(FormsContext ctx, BackupValueDataSet data) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire publie doit exister (ctx.formId)");
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Page page = ctx.page;
        MacroSupport.navigate(ctx, "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId + "&init=true");
        MacroSupport.dismissCookieBanner(page);
        Locator field = page.locator("form#form-validate #nIt0_attribute" + question.id);
        Assertions.assertEquals(1, field.count(), "La question '" + question.title + "' doit etre affichee en FO (url " + page.url() + ")");
        boolean resumed = page.locator(".alert").allInnerTexts().stream().anyMatch(a -> a.contains(SaveBackupFOMacroTest.RESUMED));

        if (data.restored()) {
            Assertions.assertTrue(resumed, "Le message « " + SaveBackupFOMacroTest.RESUMED + " » est attendu : "
                + page.locator(".alert").allInnerTexts());
            Assertions.assertEquals(data.valueFor(ctx.runSuffix), field.inputValue(), "La valeur du brouillon doit etre restauree");
        } else {
            Assertions.assertFalse(resumed, "Aucune reprise de brouillon n'est attendue : " + page.locator(".alert").allInnerTexts());
            Assertions.assertEquals("", field.inputValue(), "Le champ doit etre vide sans brouillon");
        }
    }

    @Test
    @DisplayName("Brouillon : absent une fois deconnecte, restaure apres reconnexion")
    void standaloneVerifyBackupFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFrontUserBOMacroTest.run(ctx, FrontUserDataSet.defaults());
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Commentaire libre"));
            VerifyQuestionsListedMacroTest.run(ctx);
            ConfigureFormOptionsMacroTest.run(ctx, FormOptionsDataSet.defaults());
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            LoginFrontUserFOMacroTest.run(ctx, FrontUserDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            BackupValueDataSet draft = BackupValueDataSet.of(0, "Brouillon");
            SaveBackupFOMacroTest.run(ctx, draft);
            LogoutFrontUserFOMacroTest.run(ctx);
            run(ctx, draft.absent());
            LoginFrontUserFOMacroTest.run(ctx, FrontUserDataSet.defaults());
            run(ctx, draft);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
