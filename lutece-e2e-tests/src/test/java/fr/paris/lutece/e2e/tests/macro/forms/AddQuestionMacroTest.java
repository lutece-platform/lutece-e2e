package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

/**
 * Moteur générique d'ajout de question (PAS un test en soi : aucune méthode {@code @Test}).
 *
 * <p>Chaque type de question a sa propre brique {@code AddQuestion<Type>MacroTest} qui délègue ici.
 * Lit : {@code ctx.formId}, {@code ctx.steps}. Écrit : ajoute un {@link FormsContext.QuestionRef}
 * à {@code ctx.questions}. Le type et les options viennent du {@link QuestionDataSet}.</p>
 */
@Epic("Forms")
@Feature("Questions")
@Story("Ajouter une question")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddQuestionMacroTest extends MacroTest {

    /** Ajout strict : échoue (assertion) si la question ne peut pas être créée. */
    @Step("Ajouter une question")
    public static void run(FormsContext ctx, QuestionDataSet data) {
        doAdd(ctx, data);
    }

    /**
     * Ajout tolérant : tente l'ajout et retourne {@code true} s'il aboutit, {@code false} sinon
     * (sans lever). Utilisé par les briques de types "exotiques" qui exigent parfois une config
     * externe (session, créneau, géoloc, attribut MyLutece…) et s'ignorent proprement via
     * {@code Assumptions} plutôt que d'échouer.
     */
    public static boolean tryAdd(FormsContext ctx, QuestionDataSet data) {
        try {
            doAdd(ctx, data);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void doAdd(FormsContext ctx, QuestionDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && !ctx.steps.isEmpty(),
            "Un formulaire et au moins une etape doivent exister avant d'ajouter une question");

        FormsContext.StepRef step = data.stepIndex() != null ? ctx.steps.get(data.stepIndex()) : ctx.steps.get(0);
        Page page = ctx.page;

        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=manageQuestions&id_step=" + step.id);

        openAddQuestion(page);

        // Choix du type par le libelle du bouton
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(data.type().buttonLabel)).first().click();
        page.waitForLoadState();

        // Titre (obligatoire pour tous les types)
        page.getByRole(AriaRole.TEXTBOX,
            new Page.GetByRoleOptions().setName("Titre *")).first().fill(data.title());

        fillTypeSpecificFields(page, data);

        // Bouton de sauvegarde non ambigu : les types a options exposent aussi
        // "Enregistrer et gerer les reponses" (action_createQuestionAndManageEntries), ce qui rend
        // le libelle "Enregistrer" ambigu en strict mode. On cible l'action canonique.
        page.locator("button[name='action_createQuestion']").click();
        page.waitForLoadState();

        Assertions.assertTrue(page.getByText(data.title()).first().isVisible(),
            "La question '" + data.title() + "' devrait etre listee apres enregistrement");

        FormsContext.QuestionRef ref = new FormsContext.QuestionRef();
        ref.title = data.title();
        ref.type = data.type().name();
        ref.stepId = step.id;
        ctx.questions.add(ref);
    }

    /** Ouvre l'assistant d'ajout de question (gere le menu "Actions" pour les questions suivantes). */
    private static void openAddQuestion(Page page) {
        Locator addBtn = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Ajouter une question"));
        if (addBtn.count() == 0 || !addBtn.first().isVisible()) {
            Locator actions = page.locator("#question-list").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Actions"));
            if (actions.count() > 0 && actions.first().isVisible()) {
                actions.first().click();
                page.waitForLoadState();
            }
            addBtn = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Ajouter une question"));
        }
        addBtn.first().click();
        page.waitForLoadState();
    }

    private static void fillTypeSpecificFields(Page page, QuestionDataSet data) {
        switch (data.type()) {
            case TEXTAREA -> fillIfPresent(page, "#height", String.valueOf(data.height()));
            case FILE, IMAGE -> {
                fillIfPresent(page, "#file_max_size", String.valueOf(data.fileMaxSize()));
                fillIfPresent(page, "#max_files", String.valueOf(data.maxFiles()));
            }
            default -> {
                // pas de champ additionnel obligatoire pour les autres types
            }
        }
    }

    private static void fillIfPresent(Page page, String selector, String value) {
        Locator field = page.locator(selector);
        if (field.count() > 0 && field.first().isVisible()) {
            field.first().fill(value);
        }
    }
}
