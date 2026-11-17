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

import java.util.List;

/**
 * Non-regression Forms 5/6 — volume : N reponses soumises en FO puis M saisies en BO, toutes par l'interface,
 * nombre exact retrouve en multivue apres chaque lot, puis export CSV des reponses.
 *
 * <p>N et M : {@code -Dforms.volume.fo} et {@code -Dforms.volume.bo} (defaut 20 chacun ; ≈ 1,5 s par
 * soumission sur le banc local).</p>
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegVolumeSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true \
 *     -Dforms.volume.fo=50 -Dforms.volume.bo=50
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Volume")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : volume de reponses FO et BO")
public class FormsNonRegVolumeSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Soumettre N reponses en FO et M en BO, les compter exactement, exporter")
    void volumeDeReponses() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Inscription periscolaire"));
            CreateStepMacroTest.run(forms, StepDataSet.of("Enfant"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Responsable"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Prenom de l'enfant"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nom du responsable").onStep(1));
            PublishFormMacroTest.run(forms, PublishDataSet.defaults());

            BatchSubmissionDataSet fo = BatchSubmissionDataSet.of("forms.volume.fo", 20, List.of(
                List.of(FieldValueDataSet.text("Prenom de l'enfant", "Enfant FO {i}")),
                List.of(FieldValueDataSet.text("Nom du responsable", "Responsable FO {i}"))));
            SubmitResponsesFOBatchMacroTest.run(forms, fo);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.of(fo.count()));
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.batch(fo));

            BatchSubmissionDataSet bo = BatchSubmissionDataSet.of("forms.volume.bo", 20, List.of(
                List.of(FieldValueDataSet.text("Prenom de l'enfant", "Enfant BO {i}")),
                List.of(FieldValueDataSet.text("Nom du responsable", "Responsable BO {i}"))));
            SubmitResponsesBOBatchMacroTest.run(forms, bo);
            VerifyResponseRecordedMacroTest.run(forms, ResponseCheckDataSet.of(bo.count()));
            VerifyResponseValuesMacroTest.run(forms, ResponseValuesDataSet.batch(bo));

            ExportResponsesMacroTest.run(forms, ExportDataSet.csv());
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
