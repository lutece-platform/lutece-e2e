package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionTargetsDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Brique macro FO : revenir a une etape deja parcourue par le fil des etapes du formulaire.
 *
 * <p>Le FO n'a pas de bouton « Etape precedente » : chaque etape deja validee est un bouton
 * {@code action_doGoToStep} du formulaire {@code #form-validate}, libelle par le titre de l'etape, valeur = rang dans
 * le parcours (releve du 2026-11-12, forms 4.0.2-SNAPSHOT). Le bouton est vise par son libelle visible exact (son
 * {@code textContent} porte davantage que le titre, et depuis forms 4.1 un texte {@code .visually-hidden}) ; effet prouve par
 * le champ cache {@code id_step}. Les saisies de l'etape quittee sont conservees sans validation (releve : une saisie
 * invalide est reaffichee au retour sur l'etape).</p>
 *
 * <p>Lit : {@code ctx.steps}. Dataset : {@link StepTargetDataSet} = etape a rouvrir.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Revenir a une etape precedente")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class GoToStepFOMacroTest extends MacroTest {

    private static final String FO_FORM = "form#form-validate";

    /**
     * Libelle visible de chaque bouton : son {@code innerText} sans le texte des {@code .visually-hidden}. Depuis forms
     * 4.1 (LUT-34135, releve du 2026-11-17) le bouton porte pour les lecteurs d'ecran « (etape N sur M, terminee) »,
     * que {@code innerText} rend aussi.
     */
    private static final String VISIBLE_LABELS_JS = "els => els.map(b => { let t = b.innerText;"
        + " b.querySelectorAll('.visually-hidden').forEach(v => { t = t.replace(v.innerText, ''); });"
        + " return t.replace(/\\s+/g, ' ').trim(); })";

    @Step("Revenir en FO a l'etape {data.stepIndex}")
    public static void run(FormsContext ctx, StepTargetDataSet data) {
        Assertions.assertTrue(ctx.steps.size() > data.stepIndex(), "L'etape a rouvrir doit exister (ctx.steps)");
        FormsContext.StepRef target = ctx.steps.get(data.stepIndex());
        Page page = ctx.page;
        MacroSupport.dismissCookieBanner(page);

        Locator form = page.locator(FO_FORM);
        Assertions.assertEquals(1, form.count(), "Le formulaire FO #form-validate doit etre affiche (url " + page.url() + ")");
        String before = form.locator("input[name='id_step']").inputValue();
        Locator buttons = form.locator("button[name='action_doGoToStep']");
        @SuppressWarnings("unchecked")
        List<String> labels = ((List<Object>) buttons.evaluateAll(VISIBLE_LABELS_JS)).stream().map(String::valueOf)
            .collect(Collectors.toList());
        Assertions.assertEquals(1, labels.stream().filter(target.title::equals).count(),
            "L'etape FO " + before + " doit proposer un seul bouton de retour vers '" + target.title + "' : " + labels);
        buttons.nth(labels.indexOf(target.title)).click();
        page.waitForLoadState();

        Assertions.assertEquals(1, page.locator(FO_FORM).count(), "Le formulaire FO doit etre affiche apres le retour (url " + page.url() + ")");
        Assertions.assertEquals(String.valueOf(target.id), page.locator(FO_FORM + " input[name='id_step']").inputValue(),
            "Etape FO attendue '" + target.title + "' (id " + target.id + ") apres le retour depuis l'etape " + before);
    }

    @Test
    @DisplayName("Revenir de l'etape B a l'etape A en FO, saisie de A conservee")
    void standaloneGoToStepFO() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            VerifyQuestionsListedMacroTest.run(ctx);
            VerifyTransitionTargetsMacroTest.run(ctx, TransitionTargetsDataSet.of(0, 1));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont"));
            GoToNextStepFOMacroTest.run(ctx, StepTargetDataSet.of(1));
            run(ctx, StepTargetDataSet.of(0));
            Assertions.assertEquals("Dupont", ctx.page.locator(FO_FORM + " #nIt0_attribute" + ctx.questions.get(0).id).inputValue(),
                "La saisie de l'etape A doit etre conservee au retour");
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
