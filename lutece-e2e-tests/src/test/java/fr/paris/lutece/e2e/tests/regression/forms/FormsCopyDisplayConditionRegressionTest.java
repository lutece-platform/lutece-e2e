package fr.paris.lutece.e2e.tests.regression.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.DisplayConditionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.forms.AddDisplayConditionMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.AddListValueMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.AddQuestionMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.CreateFormMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.CreateStepMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.DuplicateFormFromListMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.VerifyFormStructureEqualMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.VerifyQuestionsListedMacroTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cas remonte (D, sans ticket a ce jour) : la copie d'un formulaire garde, dans une condition d'affichage « Valeur
 * exacte », l'id de la valeur de liste de l'<b>original</b>. La question pilote est bien remappee, la valeur non : dans
 * la copie, la cible ne s'affiche plus en FO (constate le 2026-10-30 sur forms 4.0.2-SNAPSHOT, export JSON de la copie
 * et parcours FO). La copie passe par l'export puis l'import JSON : l'import d'un fichier a le meme defaut.
 *
 * <p>Cause probable : {@code AbstractFormJsonService.updateControlWithNewQuestion} ne remappe la valeur que si
 * {@code validator instanceof ICustomImportableControl} ; le validateur est produit par CDI
 * ({@code ValidatorProducer}, {@code @Produces @ApplicationScoped} de type {@code IValidator}), le proxy client
 * n'implemente que {@code IValidator}.</p>
 *
 * <p>Oracle : {@code VerifyFormStructureEqual}, dont la signature resout la valeur pilote dans le formulaire lu (la
 * copie affiche « valeur hors formulaire »).</p>
 *
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.regression.forms.FormsCopyDisplayConditionRegressionTest \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Forms")
@Feature("Cas remontes")
@Story("Copie d'une condition d'affichage")
@Tag("macro")
@Tag("forms")
@Tag("regression")
public class FormsCopyDisplayConditionRegressionTest extends MacroTest {

    @Test
    @DisplayName("La copie d'un formulaire garde sa condition d'affichage sur une valeur de liste")
    void copiedDisplayConditionPointsToTheCopy() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults().withTitle("Demande de rappel"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Demande"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Souhaitez-vous etre rappele"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Numero de telephone"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Oui", "oui"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Non", "non"));
            AddDisplayConditionMacroTest.run(ctx, DisplayConditionDataSet.of(0, 1, "Oui", "Non"));
            DuplicateFormFromListMacroTest.run(ctx);
            VerifyFormStructureEqualMacroTest.run(ctx, ctx.duplicatedFormId);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
