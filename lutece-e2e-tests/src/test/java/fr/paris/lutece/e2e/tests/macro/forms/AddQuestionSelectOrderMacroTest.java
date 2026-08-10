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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : ajouter une question de type {@code SELECT_ORDER} (liste triable).
 *
 * <p>Type "exotique" : peut exiger une configuration externe (session, creneau, geoloc, attribut
 * MyLutece, editeur riche...). L'ajout est tente en mode tolerant ; s'il n'aboutit pas, la brique
 * s'ignore proprement via {@link Assumptions} plutot que d'echouer. Delegue au moteur
 * {@link AddQuestionMacroTest}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Ajouter une question liste triable")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddQuestionSelectOrderMacroTest extends MacroTest {

    private static final QuestionType TYPE = QuestionType.SELECT_ORDER;
    private static final String TITLE = "Question liste triable";

    @Step("Ajouter une question liste triable")
    public static void run(FormsContext ctx) {
        Assumptions.assumeTrue(
            AddQuestionMacroTest.tryAdd(ctx, QuestionDataSet.of(TYPE, TITLE)),
            "Type " + TYPE + " non ajoutable en solo (configuration externe requise ?) : brique ignoree.");
    }

    @Test
    @DisplayName("Ajouter une question liste triable (auto-provisionnement ; ignoree si type non pilotable)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        run(ctx);
    }
}
