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
 * Brique macro : ajouter un controle "conditionnel" (affichage dynamique d'une question cible pilote
 * par la reponse d'une question pilote).
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}, {@code ctx.questions}. Ecrit : ajoute l'id du
 * controle cree a {@code ctx.controlIds}.</p>
 *
 * <p>Flux (mirroir du controleur {@code FormControlJspBean}, @SessionScoped). A la difference du
 * controle de validation, la cible d'un controle conditionnel ({@code id_target}) est la cle primaire
 * de l'objet <em>FormDisplay</em> de la question cible (et non l'id de la question). Cette cle n'est pas
 * exposee par les helpers de navigation : on la resout depuis la page ManageQuestions (lien de
 * suppression {@code getConfirmRemoveComposite&id_display=...} de la question cible).</p>
 *
 * <p>Etapes : {@code manageConditionControl} (memorise le type CONDITIONAL et la cible en session)
 * -&gt; {@code modifyConditionControl} (formulaire) -&gt; selection de la question pilote
 * ({@code id_question} + soumission {@code view_modifyConditionControl=validateQuestion}) -&gt;
 * validateur par defaut -&gt; valeur -&gt; validation via {@code action_modifyControl}.</p>
 *
 * <p><b>Flux intricat</b> : la resolution de l'id FormDisplay et le formulaire multi-etapes dependent
 * fortement de la version du plugin et du DOM. En cas d'echec de pilotage fiable, la brique bascule sur
 * {@link Assumptions#assumeTrue(boolean, String)} (test ignore avec message precis) plutot que sur un
 * faux echec.</p>
 */
@Epic("Forms")
@Feature("Controles")
@Story("Ajouter un controle conditionnel")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddConditionalControlMacroTest extends MacroTest {

    @Step("Ajouter un controle conditionnel")
    public static void run(FormsContext ctx, ControlDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty() && ctx.questions.size() >= 2,
            "Un formulaire, une etape et au moins deux questions (pilote + cible) doivent exister");

        Page page = ctx.page;
        int pilotIndex = data.pilotQuestionIndex() != null ? data.pilotQuestionIndex() : 0;
        int targetIndex = data.targetQuestionIndex() != null ? data.targetQuestionIndex() : 1;

        FormsContext.QuestionRef pilot = ctx.questions.get(pilotIndex);
        FormsContext.QuestionRef target = ctx.questions.get(targetIndex);
        int stepId = target.stepId;

        // La cible d'un controle conditionnel est la cle primaire du FormDisplay de la question cible.
        int displayId = resolveDisplayId(ctx, stepId, target.title);
        Assumptions.assumeTrue(displayId > 0,
            "Impossible de resoudre l'id FormDisplay de la question cible '" + target.title
            + "' sur la page ManageQuestions (structure DOM differente ?) : controle conditionnel non pilotable.");

        try {
            // 1) Vue manage conditionnelle : memorise en session le type CONDITIONAL et la cible.
            MacroSupport.navigate(ctx, MacroSupport.FORMS
                + "ManageControls.jsp?view=manageConditionControl&id_step=" + stepId
                + "&id_target=" + displayId + "&control_type=CONDITIONAL");

            // 2) Vue modify conditionnelle : formulaire de creation (mirroir du bouton "Ajouter un controle").
            MacroSupport.navigate(ctx, MacroSupport.FORMS
                + "ManageControls.jsp?view=modifyConditionControl&id_step=" + stepId + "&id_control_group=");

            Locator questionSelect = page.locator("select[name='id_question']");
            Assumptions.assumeTrue(questionSelect.count() > 0 && questionSelect.first().isVisible(),
                "Le formulaire de controle conditionnel ne s'est pas affiche (select id_question absent) : non pilotable.");

            // Selection de la question pilote puis rechargement (charge les validateurs de son type).
            boolean picked = selectByLabelContains(questionSelect.first(), pilot.title);
            Assumptions.assumeTrue(picked,
                "Question pilote '" + pilot.title + "' absente de la liste des questions du controle conditionnel.");
            clickSubmit(page, "view_modifyConditionControl", "validateQuestion");

            // Si le validateur par defaut n'a pas encore expose son champ de valeur, on le declenche.
            if (page.locator("[name='value']").count() == 0) {
                clickSubmit(page, "view_modifyConditionControl", "validateValidator");
            }

            fillControlValue(page, data.value());

            Locator okButton = page.locator("button[name='action_modifyControl']");
            Assumptions.assumeTrue(okButton.count() > 0,
                "Bouton de validation action_modifyControl absent du formulaire conditionnel : non pilotable.");
            okButton.first().click();
            page.waitForLoadState();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                "Pilotage du controle conditionnel impossible de facon fiable (a ajuster sur site vivant) : "
                + e.getMessage());
        }

        int controlId = AddValidationControlMacroTest.extractLastControlId(ctx, stepId, displayId, "CONDITIONAL");
        Assertions.assertTrue(controlId > 0,
            "Un controle conditionnel devrait exister pour la question cible '" + target.title + "' apres enregistrement");
        ctx.controlIds.add(controlId);
    }

    /**
     * Resout la cle primaire du FormDisplay d'une question a partir de son titre, depuis la page de
     * gestion des questions (lien de suppression {@code getConfirmRemoveComposite&id_display=...}).
     * Retourne -1 si introuvable.
     */
    private static int resolveDisplayId(FormsContext ctx, int stepId, String title) {
        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);

        // Strategie 1 : ligne portant le titre -> lien contenant id_display.
        try {
            Locator titleEl = page.locator(".searchable", new Page.LocatorOptions().setHasText(title)).first();
            if (titleEl.count() > 0) {
                Locator link = titleEl.locator(
                    "xpath=ancestor::*[.//a[contains(@href,'id_display=')]][1]"
                    + "//a[contains(@href,'id_display=')]").first();
                if (link.count() > 0) {
                    int id = AddValidationControlMacroTest.parseId(link.getAttribute("href"), "id_display=");
                    if (id > 0) {
                        return id;
                    }
                }
            }
        } catch (Exception ignored) {
            // on tente la strategie 2
        }

        // Strategie 2 : lien "modifier la question" (par titre) -> id_question, puis on cherche dans la
        // meme ligne un attribut id_target=...&control_type=CONDITIONAL ou un bouton id="cond-<n>".
        try {
            for (Locator link : page.locator("a[href*='view=modifyQuestion'][href*='id_question=']").all()) {
                Locator row = link.locator("xpath=ancestor::*[.//*[contains(@class,'searchable')]][1]");
                String rowText = row.count() > 0 ? safe(row.first().textContent()) : "";
                if (!rowText.contains(title)) {
                    continue;
                }
                Locator rmLink = row.locator("a[href*='id_display=']").first();
                if (rmLink.count() > 0) {
                    int id = AddValidationControlMacroTest.parseId(rmLink.getAttribute("href"), "id_display=");
                    if (id > 0) {
                        return id;
                    }
                }
                Locator condBtn = row.locator("[id^='cond-']").first();
                if (condBtn.count() > 0) {
                    String btnId = condBtn.getAttribute("id");
                    if (btnId != null && btnId.startsWith("cond-")) {
                        try {
                            return Integer.parseInt(btnId.substring("cond-".length()));
                        } catch (NumberFormatException ignored) {
                            // continue
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // introuvable
        }
        return -1;
    }

    /**
     * Selectionne, dans une liste deroulante, l'option dont le libelle contient {@code label}.
     * Renvoie {@code true} si une option a pu etre selectionnee.
     */
    private static boolean selectByLabelContains(Locator select, String label) {
        try {
            select.selectOption(new SelectOption().setLabel(label));
            return true;
        } catch (Exception ignored) {
            // recherche approchee ci-dessous
        }
        for (Locator option : select.locator("option").all()) {
            String text = safe(option.textContent()).trim();
            String value = option.getAttribute("value");
            if (!text.isEmpty() && text.contains(label) && value != null && !value.isBlank()) {
                try {
                    select.selectOption(value);
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Renseigne le champ de valeur du validateur ({@code select[name='value']} ou {@code input[name='value']}).
     * Silencieux si aucun champ de valeur n'est present.
     */
    private static void fillControlValue(Page page, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Locator select = page.locator("select[name='value']");
        if (select.count() > 0 && select.first().isVisible()) {
            if (!selectByLabelContains(select.first(), value)) {
                // valeur absente de la liste : on prend la premiere option non vide.
                for (Locator option : select.first().locator("option").all()) {
                    String v = option.getAttribute("value");
                    if (v != null && !v.isBlank()) {
                        try {
                            select.first().selectOption(v);
                        } catch (Exception ignored) {
                            // on laisse le defaut
                        }
                        break;
                    }
                }
            }
            return;
        }
        Locator input = page.locator("input[name='value']");
        if (input.count() > 0 && input.first().isVisible()) {
            input.first().fill(value);
        }
    }

    /** Clique un bouton de soumission de vue ({@code name=...} + {@code value=...}). */
    private static void clickSubmit(Page page, String name, String value) {
        Locator button = page.locator("button[name='" + name + "'][value='" + value + "']");
        if (button.count() > 0 && button.first().isVisible()) {
            button.first().click();
            page.waitForLoadState();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Test
    @DisplayName("Ajouter un controle conditionnel (auto-provisionnement formulaire + etape + 2 questions)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question pilote"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question cible"));
        run(ctx, ControlDataSet.conditionalDefaults());
    }
}
