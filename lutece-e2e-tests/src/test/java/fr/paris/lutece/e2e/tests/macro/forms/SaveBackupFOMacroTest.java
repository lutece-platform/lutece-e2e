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
 * Brique macro FO : saisir une valeur et « Sauvegarder l'avancement » (brouillon d'un usager connecte).
 *
 * <p>Condition reelle ({@code FormXPage.doSaveForBackup}, template {@code step_view.html}) : le bouton n'existe que
 * pour un usager connecte sur un formulaire dont la sauvegarde est activee ({@code ConfigureFormOptions}) ; un
 * anonyme est renvoye vers la connexion. Chemin reel : bouton {@code action_doSaveForBackup} de l'etape FO. Effet
 * prouve : l'etape est reaffichee avec « Reprise de la sauvegarde du ... », la valeur saisie et le bouton
 * « Supprimer l'avancement » ({@code action_doResetBackup}).</p>
 *
 * <p>Complement de {@code SaveDraftFO} (non modifiee), qui se met en skip faute d'usager connecte. Prealable :
 * {@code LoginFrontUserFO} puis {@code OpenFormFO}. Lit : {@code ctx.questions}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Sauvegarder l'avancement")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class SaveBackupFOMacroTest extends MacroTest {

    static final String RESUMED = "Reprise de la sauvegarde";

    @Step("Sauvegarder l'avancement avec '{data.value}' dans la question {data.questionIndex}")
    public static void run(FormsContext ctx, BackupValueDataSet data) {
        FormsContext.QuestionRef question = ctx.questions.get(data.questionIndex());
        Assertions.assertTrue(question.id > 0, "L'id de la question doit etre resolu (VerifyQuestionsListed)");
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);
        Locator field = page.locator("form#form-validate #nIt0_attribute" + question.id);
        Assertions.assertEquals(1, field.count(), "La question '" + question.title + "' doit etre affichee en FO (url " + page.url() + ")");
        Locator save = page.locator("form#form-validate button[name='action_doSaveForBackup']:not([aria-hidden='true'])");
        Assertions.assertEquals(1, save.count(),
            "Le bouton « Sauvegarder l'avancement » doit etre propose (usager connecte, sauvegarde activee)");
        String value = data.valueFor(ctx.runSuffix);
        field.fill(value);
        save.click();
        page.waitForLoadState();

        Assertions.assertTrue(page.locator(".alert").allInnerTexts().stream().anyMatch(a -> a.contains(RESUMED)),
            "La sauvegarde doit etre confirmee (« " + RESUMED + " ») : " + page.locator(".alert").allInnerTexts());
        Assertions.assertEquals(value, page.locator("form#form-validate #nIt0_attribute" + question.id).inputValue(),
            "La valeur sauvegardee doit etre reaffichee");
        Assertions.assertTrue(page.locator("form#form-validate button[name='action_doResetBackup']").count() > 0,
            "Une sauvegarde existante doit proposer « Supprimer l'avancement »");
    }

    @Test
    @DisplayName("Usager connecte : sauvegarder l'avancement d'un formulaire")
    void standaloneSaveBackupFO() {
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
            run(ctx, BackupValueDataSet.of(0, "Brouillon"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
