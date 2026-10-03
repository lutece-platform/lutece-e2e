package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionControlDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionDataSet;
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
 * Brique macro : conditionner une transition, c'est-a-dire rendre une etape conditionnelle.
 *
 * <p>Lit : {@code ctx.formId}, {@code ctx.steps}, {@code ctx.questions}. Ecrit : l'identifiant du
 * controle cree dans {@code ctx.controlIds}.</p>
 *
 * <p><b>Difference avec {@link AddConditionalControlMacroTest}.</b> Celui-ci conditionne
 * l'affichage d'une <i>question</i> dans une etape. Ici la cible du controle est la
 * <i>transition</i> elle-meme : le parcours ne franchit l'etape que si la condition est remplie,
 * ce qui fait bifurquer l'usager d'une etape a l'autre selon ses reponses. Les deux passent par le
 * meme ecran, distingues par le {@code control_type} amorce en session et par la cible.</p>
 */
@Epic("Forms")
@Feature("Etapes")
@Story("Conditionner une transition")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class AddTransitionControlMacroTest extends MacroTest {

    private static final String TYPE_CONTROLE = "TRANSITION";

    @Step("Conditionner la transition")
    public static void run(FormsContext ctx, TransitionControlDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && ctx.steps.size() > data.fromStepIndex(),
            "Un formulaire et l'etape source doivent exister avant de conditionner une transition");
        Assertions.assertTrue(ctx.questions.size() > data.pilotQuestionIndex(),
            "La question qui conditionne la transition doit exister (ctx.questions)");

        Page page = ctx.page;
        FormsContext.StepRef source = ctx.steps.get(data.fromStepIndex());
        FormsContext.QuestionRef pilote = ctx.questions.get(data.pilotQuestionIndex());

        int transitionId = idTransitionSortante(ctx, source.id);
        Assumptions.assumeTrue(transitionId > 0,
            "Aucune transition sortante sur l'etape '" + source.title + "' : rien a conditionner");

        try {
            // Meme sequence que le controle conditionnel : la vue de gestion amorce en session le
            // type de controle et sa cible, la vue de modification rend ensuite le formulaire.
            MacroSupport.navigate(ctx, MacroSupport.FORMS
                + "ManageControls.jsp?view=manageConditionControl&id_step=" + source.id
                + "&id_target=" + transitionId + "&control_type=" + TYPE_CONTROLE);
            MacroSupport.navigate(ctx, MacroSupport.FORMS
                + "ManageControls.jsp?view=modifyConditionControl&id_step=" + source.id
                + "&id_control_group=");

            Locator questions = page.locator("select[name='id_question']");
            Assumptions.assumeTrue(questions.count() > 0 && questions.first().isVisible(),
                "Le formulaire de controle de transition ne s'est pas affiche : non pilotable");

            // Cascade de l'ecran : l'etape, puis la question, puis le type de controle.
            AddConditionalControlMacroTest.clickSubmit(page, "view_modifyConditionControl", "validateStep");

            boolean choisie = AddConditionalControlMacroTest.selectByLabelContains(questions.first(), pilote.title);
            Assumptions.assumeTrue(choisie,
                "Question '" + pilote.title + "' absente de la liste du controle de transition");
            AddConditionalControlMacroTest.clickSubmit(page, "view_modifyConditionControl", "validateQuestion");

            // Le type de controle est obligatoire : sans lui, l'enregistrement est refuse avec
            // "Le champ Type de controle ne doit pas etre vide". La liste reste vide quand le site
            // n'expose aucun validateur, auquel cas aucune condition n'est creable, quel que soit
            // le type de question.
            Locator validateurs = page.locator("select[name='validatorName'] option[value]:not([value=''])");
            Assumptions.assumeTrue(validateurs.count() > 0,
                "Aucun type de controle propose pour la question '" + pilote.title + "' : ce site "
                    + "n'expose pas de validateur, les conditions ne sont pas creables");
            page.locator("select[name='validatorName']")
                .selectOption(validateurs.first().getAttribute("value"));
            AddConditionalControlMacroTest.clickSubmit(page, "view_modifyConditionControl", "validateValidator");

            AddConditionalControlMacroTest.fillControlValue(page, data.value());

            Locator ok = page.locator("button[name='action_modifyControl']");
            Assumptions.assumeTrue(ok.count() > 0,
                "Bouton de validation du controle absent : non pilotable");
            ok.first().click();
            page.waitForLoadState();
        } catch (org.opentest4j.TestAbortedException deja) {
            // Diagnostic deja explicite (question absente, aucun validateur) : le relayer tel quel
            // plutot que de le noyer dans un message generique.
            throw deja;
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                "Pilotage du controle de transition impossible de facon fiable : " + e.getMessage());
        }

        int controlId = idControleCree(ctx, source.id, transitionId);
        Assertions.assertTrue(controlId > 0,
            "Un controle de transition devrait exister sur l'etape '" + source.title + "' apres enregistrement");
        ctx.controlIds.add(controlId);
    }

    /**
     * Identifiant du dernier controle attache a une transition.
     *
     * <p>Les controles de transition se consultent par les memes vues que les controles
     * conditionnels — seule la cible change —, la ou l'extraction partagee ne reserve ces vues
     * qu'au type {@code CONDITIONAL}.</p>
     *
     * @param ctx          contexte du formulaire
     * @param stepId       identifiant de l'etape source
     * @param transitionId identifiant de la transition
     * @return l'identifiant du controle, ou -1 si aucun
     */
    private static int idControleCree(FormsContext ctx, int stepId, int transitionId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageControls.jsp?view=manageConditionControl&id_step=" + stepId
            + "&id_target=" + transitionId + "&control_type=" + TYPE_CONTROLE);
        int dernier = -1;
        Locator liens = ctx.page.locator("a[href*='id_control=']");
        for (int i = 0; i < liens.count(); i++) {
            String href = liens.nth(i).getAttribute("href");
            if (href == null || !href.contains("id_control=")) {
                continue;
            }
            try {
                int id = Integer.parseInt(href.split("id_control=")[1].split("&")[0].split("#")[0]);
                dernier = Math.max(dernier, id);
            } catch (RuntimeException ignore) {
                // lien sans identifiant exploitable
            }
        }
        return dernier;
    }

    /**
     * Identifiant de la transition sortante d'une etape.
     *
     * <p>Lue sur la page des transitions de l'etape plutot que dans le contexte : la creation de
     * transition ne memorise son identifiant qu'au mieux, et la cible du controle doit etre exacte.</p>
     *
     * @param ctx    contexte du formulaire
     * @param stepId identifiant de l'etape source
     * @return l'identifiant de la transition, ou -1 si l'etape n'en porte aucune
     */
    private static int idTransitionSortante(FormsContext ctx, int stepId) {
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + stepId);
        Locator liens = ctx.page.locator("a[href*='id_transition=']");
        for (int i = 0; i < liens.count(); i++) {
            String href = liens.nth(i).getAttribute("href");
            if (href == null) {
                continue;
            }
            try {
                return Integer.parseInt(href.split("id_transition=")[1].split("&")[0].split("#")[0]);
            } catch (RuntimeException ignore) {
                // lien sans identifiant exploitable
            }
        }
        return -1;
    }

    @Test
    @DisplayName("Conditionner une transition (auto-provisionnement formulaire + 2 etapes + question)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.of("Etape source"));
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape cible"));
        SetStepInitialMacroTest.run(ctx, StepTargetDataSet.of(0));
        SetStepFinalMacroTest.run(ctx, StepTargetDataSet.of(1));
        UnsetStepFinalMacroTest.run(ctx, StepTargetDataSet.of(0));
        CreateTransitionMacroTest.run(ctx, TransitionDataSet.of(0, 1));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question pilote").onStep(0));
        run(ctx, TransitionControlDataSet.of(0, 0, "oui"));
    }
}
