package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
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

/**
 * Brique macro : remplir un champ de la saisie BO. Le rendu des champs est celui du FO (meme
 * {@code StepDisplayTree}) : la brique verifie qu'elle est sur l'ecran BO puis delegue a {@code FillFieldFO}
 * (champ trouve par son libelle, valeur relue). Lit : la page courante. Ecrit : rien.
 */
@Epic("Forms")
@Feature("Saisie back-office")
@Story("Remplir un champ en BO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class FillFieldBOMacroTest extends MacroTest {

    @Step("Remplir le champ BO {data.label}")
    public static void run(FormsContext ctx, FieldValueDataSet data) {
        Assertions.assertEquals(1, ctx.page.locator(OpenFormResponseBOMacroTest.BO_FORM).count(),
            "La saisie BO doit etre affichee (url " + ctx.page.url() + ")");
        FillFieldFOMacroTest.run(ctx, data);
    }

    @Test
    @DisplayName("Remplir un champ texte en saisie BO")
    void standaloneFillFieldBO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            OpenFormResponseBOMacroTest.run(ctx);
            run(ctx, FieldValueDataSet.text("Nom", "Dupont BO"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
