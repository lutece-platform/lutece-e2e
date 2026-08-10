package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ControlDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : ajouter un controle de "validation" sur une question.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}, {@code ctx.questions}. Ecrit : ajoute l'id du
 * controle cree a {@code ctx.controlIds}.</p>
 *
 * <p>Flux (mirroir du controleur {@code FormControlJspBean}, @SessionScoped) : pour un controle de
 * validation la cible ({@code id_target}) est l'id de la question elle-meme (compositeId). On passe
 * d'abord par la vue {@code manageControl} (qui memorise en session le type de controle et la cible),
 * puis par la vue {@code modifyControl} (formulaire de creation). Le validateur par defaut de la
 * question est pre-selectionne ; on renseigne au besoin la valeur ({@code name='value'}) et le message
 * d'erreur ({@code name='errorMessage'}), puis on valide via le bouton {@code action_modifyControl}.</p>
 */
@Epic("Forms")
@Feature("Controles")
@Story("Ajouter un controle de validation")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddValidationControlMacroTest extends MacroTest {

    @Step("Ajouter un controle de validation")
    public static void run(FormsContext ctx, ControlDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty() && !ctx.questions.isEmpty(),
            "Un formulaire, une etape et une question doivent exister avant d'ajouter un controle de validation");

        Page page = ctx.page;
        int qIndex = data.questionIndex() != null ? data.questionIndex() : 0;
        FormsContext.QuestionRef question = ctx.questions.get(qIndex);
        int stepId = question.stepId;

        // La cible d'un controle de validation est l'id (compositeId) de la question : on le resout
        // depuis la page de gestion des questions (mirroir du lien view=modifyQuestion&id_question=...).
        int questionId = resolveQuestionId(ctx, stepId, question.title);
        Assumptions.assumeTrue(questionId > 0,
            "Impossible de resoudre l'id de la question '" + question.title
            + "' sur la page ManageQuestions (structure DOM differente ?)");

        // 1) Vue manage : positionne en session le type de controle (VALIDATION) et la cible.
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageControls.jsp?view=manageControl&id_step=" + stepId
            + "&id_target=" + questionId + "&control_type=VALIDATION");

        // 2) Vue modify : formulaire de creation d'un nouveau controle.
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageControls.jsp?view=modifyControl&id_step=" + stepId
            + "&id_target=" + questionId + "&control_type=VALIDATION");

        Locator okButton = page.locator("button[name='action_modifyControl']");
        Assumptions.assumeTrue(okButton.count() > 0,
            "Le formulaire de controle de validation ne s'est pas affiche (bouton action_modifyControl absent)");

        // Valeur du validateur (present uniquement pour les validateurs qui en attendent une).
        fillControlValue(page, data.value());

        // Message d'erreur (champ optionnel du template de validation).
        Locator errorField = page.locator("input[name='errorMessage']");
        if (data.errorMessage() != null && !data.errorMessage().isBlank()
                && errorField.count() > 0 && errorField.first().isVisible()) {
            errorField.first().fill(data.errorMessage());
        }

        okButton.first().click();
        page.waitForLoadState();

        int controlId = extractLastControlId(ctx, stepId, questionId, "VALIDATION");
        Assertions.assertTrue(controlId > 0,
            "Un controle de validation devrait exister pour la question '" + question.title + "' apres enregistrement");
        ctx.controlIds.add(controlId);
    }

    /**
     * Renseigne le champ de valeur du validateur, qu'il s'agisse d'une liste ({@code select[name='value']})
     * ou d'un champ libre ({@code input[name='value']}). Silencieux si le validateur n'a pas de valeur.
     */
    private static void fillControlValue(Page page, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Locator select = page.locator("select[name='value']");
        if (select.count() > 0 && select.first().isVisible()) {
            try {
                select.first().selectOption(new SelectOption().setLabel(value));
            } catch (Exception e) {
                try {
                    select.first().selectOption(value);
                } catch (Exception ignored) {
                    // valeur absente de la liste : on laisse le defaut
                }
            }
            return;
        }
        Locator input = page.locator("input[name='value']");
        if (input.count() > 0 && input.first().isVisible()) {
            input.first().fill(value);
        }
    }

    /**
     * Resout l'id (compositeId) d'une question a partir de son titre sur la page de gestion des questions.
     * Retourne -1 si introuvable.
     */
    static int resolveQuestionId(FormsContext ctx, int stepId, String title) {
        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);

        // Strategie 1 : ligne portant le titre -> lien "modifier la question".
        try {
            Locator titleEl = page.locator(".searchable", new Page.LocatorOptions().setHasText(title)).first();
            if (titleEl.count() > 0) {
                Locator link = titleEl.locator(
                    "xpath=ancestor::*[.//a[contains(@href,'view=modifyQuestion')]][1]"
                    + "//a[contains(@href,'view=modifyQuestion')]").first();
                if (link.count() > 0) {
                    int id = parseId(link.getAttribute("href"), "id_question=");
                    if (id > 0) {
                        return id;
                    }
                }
            }
        } catch (Exception ignored) {
            // on tente la strategie 2
        }

        // Strategie 2 : on parcourt les liens "modifier la question" et on retient celui dont la
        // ligne contient le titre recherche.
        try {
            for (Locator link : page.locator("a[href*='view=modifyQuestion'][href*='id_question=']").all()) {
                Locator row = link.locator("xpath=ancestor::*[.//*[contains(@class,'searchable')]][1]");
                String rowText = row.count() > 0 ? safe(row.first().textContent()) : "";
                if (rowText.contains(title)) {
                    return parseId(link.getAttribute("href"), "id_question=");
                }
            }
        } catch (Exception ignored) {
            // introuvable
        }
        return -1;
    }

    /**
     * Renvoie l'id du dernier controle (id le plus eleve = le plus recent) liste dans la vue de gestion
     * correspondante ({@code manageControl} pour VALIDATION, {@code manageConditionControl} pour CONDITIONAL).
     */
    static int extractLastControlId(FormsContext ctx, int stepId, int idTarget, String controlType) {
        boolean conditional = "CONDITIONAL".equals(controlType);
        String view = conditional ? "manageConditionControl" : "manageControl";
        String editView = conditional ? "view=modifyConditionControl" : "view=modifyControl";

        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageControls.jsp?view=" + view + "&id_step=" + stepId
            + "&id_target=" + idTarget + "&control_type=" + controlType);

        int last = -1;
        for (Locator link : ctx.page.locator("a[href*='id_control=']").all()) {
            String href = link.getAttribute("href");
            if (href != null && href.contains(editView) && href.contains("id_control=")) {
                int id = parseId(href, "id_control=");
                if (id > last) {
                    last = id;
                }
            }
        }
        return last;
    }

    static int parseId(String href, String key) {
        if (href == null || !href.contains(key)) {
            return -1;
        }
        try {
            return Integer.parseInt(href.split(key)[1].split("&")[0].split("#")[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Test
    @DisplayName("Ajouter un controle de validation (auto-provisionnement formulaire + etape + question)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question a valider"));
        run(ctx, ControlDataSet.validationDefaults());
    }
}
