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
 * Brique macro de volume : soumettre N reponses en front-office par l'interface, parcours complet a chaque fois
 * ({@code Portal.jsp?page=forms&view=stepView&id_form=F&init=true}, etapes, recapitulatif, validation).
 *
 * <p>Le formulaire doit etre publie. N vient du dataset ({@code -Dforms.volume.fo}, defaut de la composition).
 * La brique prouve que chaque parcours aboutit sans erreur ; le nombre exact de reponses enregistrees se prouve
 * ensuite par {@code VerifyResponseRecorded(N)} (difference d'ensembles dans la multivue). Lit : {@code ctx.steps}.</p>
 */
@Epic("Forms")
@Feature("Volume")
@Story("Soumettre des reponses en lot en FO")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
@Tag("volume")
public class SubmitResponsesFOBatchMacroTest extends MacroTest {

    @Step("Soumettre {data.count} reponses en front-office")
    public static void run(FormsContext ctx, BatchSubmissionDataSet data) {
        ResponseBatchEngine.run(ctx, data,
            "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + ctx.formId + "&init=true", "Portal.jsp");
    }

    @Test
    @DisplayName("Soumettre un lot de reponses FO (forms.volume.fo, defaut 3) et les retrouver toutes")
    void standaloneSubmitResponsesFOBatch() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            BatchSubmissionDataSet data = BatchSubmissionDataSet.of("forms.volume.fo", 3, List.of(
                List.of(FieldValueDataSet.text("Nom", "Nom FO {i}")),
                List.of(FieldValueDataSet.text("Ville", "Ville FO {i}"))));
            run(ctx, data);
            VerifyResponseRecordedMacroTest.run(ctx, ResponseCheckDataSet.of(data.count()));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
