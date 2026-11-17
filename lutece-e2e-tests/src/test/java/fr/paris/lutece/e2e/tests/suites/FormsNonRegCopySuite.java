package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Non-regression Forms 2/6 — copie : duplication d'etape, de question et de formulaire, export JSON puis
 * import ; la copie et l'import sont compares a l'original (etapes, composants avec types, obligation, valeurs de
 * liste et groupe, transitions par etape d'arrivee). La copie d'une condition d'affichage est un cas remonte a part
 * ({@code FormsCopyDisplayConditionRegressionTest}) : rouge sur forms 4.0.2-SNAPSHOT, il rendrait la suite rouge.
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegCopySuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Copie")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : copie, duplication, export et import")
public class FormsNonRegCopySuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Dupliquer etape, question et formulaire ; exporter puis importer ; structures identiques")
    void copieDuFormulaire() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Inscription atelier"));
            CreateStepMacroTest.run(forms, StepDataSet.of("Participant"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Atelier"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nom du participant"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Creneau"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Atelier choisi").onStep(1));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Contraintes horaires"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Telephone").onStep(1));
            VerifyQuestionsListedMacroTest.run(forms);
            AddListValueMacroTest.run(forms, ListValueDataSet.create(1, "Matin", "matin"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(1, "Apres-midi", "aprem"));
            SetQuestionMandatoryMacroTest.run(forms, QuestionTargetDataSet.of(0));
            CreateGroupMacroTest.run(forms, GroupDataSet.repeatable("Personne a prevenir"));
            MoveQuestionMacroTest.run(forms, MoveQuestionDataSet.toGroup(4, 1, 0));

            DuplicateQuestionMacroTest.run(forms, QuestionTargetDataSet.of(0));
            DuplicateStepMacroTest.run(forms, StepTargetDataSet.of(0));

            DuplicateFormFromListMacroTest.run(forms);
            VerifyFormStructureEqualMacroTest.run(forms, forms.duplicatedFormId);

            ExportFormJsonFileMacroTest.run(forms);
            int imported = ImportFormJsonFileMacroTest.run(forms);
            VerifyFormStructureEqualMacroTest.run(forms, imported);
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
