package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : creer une transition entre une etape source et une etape cible.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}. Ecrit : ajoute (si derivable) un id a
 * {@code ctx.transitionIds}. Porte le flux CDI {@code FormsPage.configureStepTransitionDirect}
 * (navigation directe vers {@code createTransition}, selection de l'etape suivante, clic OK) puis
 * verifie l'effet via la logique de {@link VerifyTransitionMacroTest}.</p>
 */
@Epic("Forms")
@Feature("Transitions")
@Story("Creer une transition")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class CreateTransitionMacroTest extends MacroTest {

    @Step("Creer une transition entre deux etapes")
    public static void run(FormsContext ctx, TransitionDataSet data) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId) avant de creer une transition");
        int maxIndex = Math.max(data.fromStepIndex(), data.toStepIndex());
        Assertions.assertTrue(ctx.steps.size() > maxIndex,
            "Les etapes source et cible doivent exister (ctx.steps) avant de creer une transition");

        FormsContext.StepRef fromStep = ctx.steps.get(data.fromStepIndex());
        FormsContext.StepRef toStep = ctx.steps.get(data.toStepIndex());
        Page page = ctx.page;

        // IMPORTANT : le FormTransitionJspBean est session-scoped et ne peuple son champ _form que
        // via la vue liste (getManageTransitions lit id_step et derive id_form de l'etape). Sans cet
        // amorcage, detectCircularTransition() leve un NullPointerException (Internal error) a la
        // creation. On visite donc d'abord la liste de l'etape source.
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + fromStep.id);

        // Assistant de creation de transition de l'etape source.
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=createTransition&id_step=" + fromStep.id);

        // Le select 'nextStep' (etape cible) est OBLIGATOIRE : sa valeur est l'id de l'etape.
        selectTargetStep(page, toStep);

        // Bouton de validation du formulaire de transition (okAction='createTransition').
        page.locator("button[name='action_createTransition']").click();
        page.waitForLoadState();

        // Assertion de l'effet : Lutece confirme la creation par le message info "Liaison creee".
        // (La vue liste ne rend pas de carte scannable de facon fiable en 4.0.2 ; ce message est le
        // signal applicatif direct et robuste.)
        String body = page.locator("body").innerText();
        Assertions.assertFalse(body.contains("Internal error"),
            "La creation de transition a provoque une erreur serveur (Internal error)");
        Assertions.assertTrue(page.getByText("Liaison").first().isVisible(),
            "La creation de transition aurait du afficher la confirmation 'Liaison creee'");

        // Best-effort : memoriser l'id de la transition si un lien l'expose.
        Integer transitionId = deriveTransitionId(page);
        if (transitionId != null) {
            ctx.transitionIds.add(transitionId);
        }
    }

    /**
     * Selectionne l'etape cible dans le select obligatoire {@code name='nextStep'} de la page
     * createTransition. La valeur des options est l'id de l'etape (ReferenceList code/name), on
     * selectionne donc par valeur = id, avec repli par libelle.
     */
    private static void selectTargetStep(Page page, FormsContext.StepRef toStep) {
        Locator select = page.locator("select[name='nextStep']");
        select.waitFor(new Locator.WaitForOptions().setTimeout(10_000));
        try {
            select.selectOption(new SelectOption().setValue(String.valueOf(toStep.id)));
            return;
        } catch (RuntimeException byValueFailed) {
            // repli : selection par libelle contenant le titre de l'etape cible
        }
        for (String option : select.locator("option").allTextContents()) {
            if (option != null && option.contains(toStep.title)) {
                select.selectOption(new SelectOption().setLabel(option));
                return;
            }
        }
    }

    /** Extrait, au mieux, un id de transition depuis les liens de la liste des transitions. */
    private static Integer deriveTransitionId(Page page) {
        for (Locator link : page.locator("a[href*='id_transition=']").all()) {
            String href = link.getAttribute("href");
            if (href == null || !href.contains("id_transition=")) {
                continue;
            }
            try {
                String idStr = href.split("id_transition=")[1].split("&")[0].split("#")[0];
                return Integer.parseInt(idStr);
            } catch (RuntimeException ignored) {
                // lien mal forme : on continue
            }
        }
        return null;
    }

    @Test
    @DisplayName("Creer une transition (auto-provisionnement formulaire + 2 etapes)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.initial("Etape initiale"));
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape finale"));
        run(ctx, TransitionDataSet.defaults());
    }
}
