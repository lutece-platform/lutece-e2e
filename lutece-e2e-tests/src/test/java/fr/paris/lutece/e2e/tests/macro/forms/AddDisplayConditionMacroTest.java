package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.DisplayConditionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
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

import java.util.List;

/**
 * Brique macro : poser une condition d'affichage « la cible s'affiche si la pilote vaut X » (validateur
 * « Valeur exacte », {@code forms.listValueValidator}).
 *
 * <p>Chemin reel : bouton « condition » de la ligne de la cible = offcanvas en iframe
 * {@code ManageControls.jsp?view=manageConditionControl&id_step=S&id_target=<id_display de la cible>&control_type=CONDITIONAL},
 * lien « Ajouter un controle » ({@code view=modifyConditionControl}), formulaire {@code #modify_condition_control} en
 * trois temps : {@code id_question} + {@code validateQuestion}, {@code validatorName} + {@code validateValidator},
 * {@code value} (valeurs de la pilote) + {@code action_modifyControl}. Le choix est strict : validateur ou valeur
 * absents = echec, jamais de repli sur la premiere option. Pilote sur une autre etape que la cible : la liste
 * {@code id_question} ne porte que les questions de l'etape choisie, l'etape de la pilote est donc d'abord choisie
 * ({@code id_step} + {@code validateStep}, releve du 2026-11-06, scenario LUT-34462).</p>
 *
 * <p>Complement de {@code AddConditionalControl} (non modifiee) : sans choix du validateur, une pilote radio propose
 * « Nombre maximum de reponses » (champ numerique) ; une valeur absente est remplacee en silence par la premiere
 * option de la liste (constat du 2026-10-29). Effet prouve : retour sur la liste des questions avec
 * {@code forms.info.control.created}, puis la vue des conditions de la cible cite la pilote et « Valeur exacte », et
 * la fiche de la condition ({@link FormsNonRegSupport#displayConditions}) designe l'etape, la question et la valeur
 * pilotes attendues par leurs ids (revue adversariale du 2026-10-30, constat 10 : la valeur n'etait pas relue).</p>
 *
 * <p>Lit : {@code ctx.questions} (ids resolus), valeurs de liste de la pilote ({@code AddListValue}). Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Controles")
@Story("Poser une condition d'affichage")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class AddDisplayConditionMacroTest extends MacroTest {

    static final String VALIDATOR = "forms.listValueValidator";
    static final String VALIDATOR_LABEL = "Valeur exacte";

    @Step("Afficher la question {data.targetQuestionIndex} si la question {data.pilotQuestionIndex} vaut '{data.showValue}'")
    public static void run(FormsContext ctx, DisplayConditionDataSet data) {
        Assertions.assertTrue(ctx.questions.size() > Math.max(data.pilotQuestionIndex(), data.targetQuestionIndex()),
            "Les questions pilote et cible doivent exister (ctx.questions)");
        FormsContext.QuestionRef pilot = ctx.questions.get(data.pilotQuestionIndex());
        FormsContext.QuestionRef target = ctx.questions.get(data.targetQuestionIndex());
        Assertions.assertTrue(pilot.id > 0 && target.id > 0, "Les ids des questions doivent etre resolus (VerifyQuestionsListed)");
        int[] place = FormsNonRegSupport.questionPlacement(ctx, target.stepId).get(target.id);
        Assertions.assertNotNull(place, "La question cible " + target.id + " doit etre listee sur l'etape " + target.stepId);
        String manage = MacroSupport.FORMS + "ManageControls.jsp?view=manageConditionControl&id_step=" + target.stepId
            + "&id_target=" + place[0] + "&control_type=CONDITIONAL";
        Page page = ctx.page;

        MacroSupport.navigate(ctx, manage);
        Locator add = page.locator("a[href*='view=modifyConditionControl']");
        Assertions.assertEquals(1, add.count(), "Le lien « Ajouter un controle » doit etre propose (url " + page.url() + ")");
        add.click();
        page.waitForLoadState();

        Locator form = page.locator("form#modify_condition_control");
        Assertions.assertEquals(1, form.count(), "Le formulaire de condition doit etre affiche");
        if (pilot.stepId != target.stepId) {
            form.locator("select[name='id_step']").selectOption(String.valueOf(pilot.stepId));
            form.locator("button[name='view_modifyConditionControl'][value='validateStep']").click();
            page.waitForLoadState();
            form = page.locator("form#modify_condition_control");
            Assertions.assertEquals(1, form.count(), "Le formulaire de condition doit etre reaffiche apres le choix de l'etape pilote");
        }
        @SuppressWarnings("unchecked")
        List<String> pilots = (List<String>) form.locator("select[name='id_question'] option").evaluateAll("os => os.map(o => o.value)");
        Assertions.assertTrue(pilots.contains(String.valueOf(pilot.id)),
            "La question pilote '" + pilot.title + "' (" + pilot.id + ") doit etre proposee : " + pilots);
        form.locator("select[name='id_question']").selectOption(String.valueOf(pilot.id));
        form.locator("button[name='view_modifyConditionControl'][value='validateQuestion']").click();
        page.waitForLoadState();

        form = page.locator("form#modify_condition_control");
        @SuppressWarnings("unchecked")
        List<String> validators = (List<String>) form.locator("select[name='validatorName'] option")
            .evaluateAll("os => os.map(o => o.value)");
        Assertions.assertTrue(validators.contains(VALIDATOR),
            "Le validateur « Valeur exacte » doit etre propose pour la pilote '" + pilot.title + "' : " + validators);
        form.locator("select[name='validatorName']").selectOption(VALIDATOR);
        form.locator("button[name='view_modifyConditionControl'][value='validateValidator']").click();
        page.waitForLoadState();

        Locator value = page.locator("form#modify_condition_control select[name='value']");
        Assertions.assertTrue(value.locator("option").allInnerTexts().stream().map(String::trim).anyMatch(data.showValue()::equals),
            "La valeur '" + data.showValue() + "' doit etre proposee : " + value.locator("option").allInnerTexts());
        value.selectOption(new SelectOption().setLabel(data.showValue()));
        page.locator("form#modify_condition_control button[name='action_modifyControl']").click();
        page.waitForLoadState();
        Assertions.assertTrue(page.url().contains("forms.info.control.created"),
            "La creation du controle doit etre confirmee (url " + page.url() + ", alertes : "
                + page.locator(".alert, .toast").allInnerTexts() + ")");

        MacroSupport.navigate(ctx, manage);
        String body = page.locator("body").innerText();
        // La vue liste chaque condition (« Question cible : <pilote> », « Type de controle : <validateur> ») sans la
        // valeur choisie : la valeur est prouvee en FO par VerifyConditionalRevealFO.
        Assertions.assertTrue(body.contains(": " + pilot.title) && body.contains(VALIDATOR_LABEL),
            "Les conditions de la cible doivent citer la pilote '" + pilot.title + "' et « " + VALIDATOR_LABEL + " » : " + body.trim());
        List<FormsNonRegSupport.DisplayCondition> read = FormsNonRegSupport.displayConditions(ctx, target.stepId, place[0]).getValue();
        Assertions.assertTrue(read.stream().anyMatch(c -> c.pilotStepId() == pilot.stepId && c.pilotQuestionId() == pilot.id
                && data.showValue().equals(c.valueLabel())),
            "La fiche de la condition doit designer l'etape " + pilot.stepId + ", la question " + pilot.id + " et la valeur '"
                + data.showValue() + "' : " + read);
    }

    @Test
    @DisplayName("Afficher « Precisions » seulement si « Avez-vous un animal ? » vaut Oui")
    void standaloneAddDisplayCondition() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Avez-vous un animal ?"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Precisions"));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Oui", "oui"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Non", "non"));
            run(ctx, DisplayConditionDataSet.of(0, 1, "Oui", "Non"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }

    @Test
    @DisplayName("Afficher « Preciser Autre » (etape 2) seulement si « Votre statut ? » (etape 1) vaut Autre")
    void standaloneAddDisplayConditionFromPreviousStep() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Statut"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Precisions"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Votre statut ?"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Preciser Autre").onStep(1));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Fonctionnaire", "fonctionnaire"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Autre", "autre"));
            run(ctx, DisplayConditionDataSet.of(0, 1, "Autre", "Fonctionnaire"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
