package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.BatchSubmissionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Brique macro de volume : saisir N reponses en back-office par l'interface ({@code ManageFormResponse.jsp},
 * parcours complet a chaque fois). Le formulaire n'a pas besoin d'etre publie. N vient du dataset
 * ({@code -Dforms.volume.bo}). Le nombre exact se prouve par {@code VerifyResponseRecorded(N)}.
 */
@Epic("Forms")
@Feature("Volume")
@Story("Saisir des reponses en lot en BO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
@Tag("volume")
public class SubmitResponsesBOBatchMacroTest extends MacroTest {

    @Step("Saisir {data.count} reponses en back-office")
    public static void run(FormsContext ctx, BatchSubmissionDataSet data) {
        ResponseBatchEngine.run(ctx, data,
            MacroSupport.FORMS + "ManageFormResponse.jsp?view=stepView&id_form=" + ctx.formId + "&init=true", "ManageFormResponse.jsp");
    }

    @Test
    @DisplayName("Saisir un lot de reponses BO (forms.volume.bo, defaut 3) et les retrouver toutes")
    void standaloneSubmitResponsesBOBatch() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            BatchSubmissionDataSet data = BatchSubmissionDataSet.of("forms.volume.bo", 3, List.of(
                List.of(FieldValueDataSet.text("Nom", "Nom BO {i}")),
                List.of(FieldValueDataSet.text("Ville", "Ville BO {i}"))));
            run(ctx, data);
            VerifyResponseRecordedMacroTest.run(ctx, ResponseCheckDataSet.of(data.count()));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
