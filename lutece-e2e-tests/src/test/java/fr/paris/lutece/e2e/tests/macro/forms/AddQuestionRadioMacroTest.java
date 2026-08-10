package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : ajouter une question de type {@code RADIO} (bouton radio).
 *
 * <p>Type de base : l'ajout DOIT reussir. Delegue au moteur {@link AddQuestionMacroTest}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Ajouter une question bouton radio")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddQuestionRadioMacroTest extends MacroTest {

    private static final QuestionType TYPE = QuestionType.RADIO;
    private static final String TITLE = "Question bouton radio";

    @Step("Ajouter une question bouton radio")
    public static void run(FormsContext ctx) {
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(TYPE, TITLE));
    }

    @Test
    @DisplayName("Ajouter une question bouton radio (auto-provisionnement formulaire + etape)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        run(ctx);
    }
}
