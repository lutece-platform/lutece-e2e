package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
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
import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
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
 * <p><b>Enchainement, tel que l'IHM l'impose.</b> L'ecran des transitions ouvre la gestion des
 * controles dans un panneau lateral, dont le contenu est une iframe sur
 * {@code ManageControls.jsp?view=manageControl} — et non {@code manageConditionControl}, qui sert
 * aux conditions d'affichage d'une question. De la, « Ajouter un controle » mene au formulaire, ou
 * l'enchainement est en cascade : choisir la question cible, <b>valider</b> ce choix, et seulement
 * alors la liste des types de controle se remplit — elle reste vide tant que la question n'est pas
 * validee, ce qui fait echouer l'enregistrement sur « Le champ Type de controle ne doit pas etre
 * vide ». Le type retenu determine enfin la nature du champ de valeur, liste fermee ou saisie
 * libre.</p>
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
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) FormsContext ctx, TransitionControlDataSet data) {
        Assertions.assertTrue(ctx.formId > 0 && ctx.steps.size() > data.fromStepIndex(),
            "Un formulaire et l'etape source doivent exister avant de conditionner une transition");
        Assertions.assertTrue(ctx.questions.size() > data.pilotQuestionIndex(),
            "La question qui conditionne la transition doit exister (ctx.questions)");

        FormsContext.StepRef source = ctx.steps.get(data.fromStepIndex());
        FormsContext.QuestionRef pilote = ctx.questions.get(data.pilotQuestionIndex());

        if (data.toStepIndex() == null) {
            // Aucune transition visee en particulier : l'etape peut legitimement n'en porter
            // aucune, le scenario se poursuit sans condition.
            int premiere = idTransitionSortante(ctx, source.id);
            Assumptions.assumeTrue(premiere > 0,
                "Aucune transition sortante sur l'etape '" + source.title + "' : rien a conditionner");
            conditionner(ctx, data, source, pilote, premiere);
            return;
        }

        // Une transition precise est visee : ne pas la trouver signifie que l'embranchement
        // n'existe pas tel que le scenario le decrit. L'ignorer laisserait les deux parcours
        // emprunter la meme branche sans que rien ne le signale.
        String arrivee = ctx.steps.get(data.toStepIndex()).title;
        int transitionId = idTransitionVers(ctx, source.id, arrivee);
        Assertions.assertTrue(transitionId > 0,
            "Aucune transition de l'etape '" + source.title + "' ne mene a '" + arrivee
                + "' : l'embranchement attendu n'existe pas");
        conditionner(ctx, data, source, pilote, transitionId);
    }

    /**
     * Attache le controle a la transition designee.
     *
     * @param ctx          contexte du formulaire
     * @param data         condition a poser
     * @param source       etape portant la transition
     * @param pilote       question qui conditionne
     * @param transitionId identifiant de la transition visee
     */
    private static void conditionner(FormsContext ctx, TransitionControlDataSet data,
        FormsContext.StepRef source, FormsContext.QuestionRef pilote, int transitionId) {
        Page page = ctx.page;

        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageControls.jsp?view=manageControl&id_step=" + source.id
            + "&id_target=" + transitionId + "&control_type=" + TYPE_CONTROLE);

        Locator ajouter = page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Ajouter un contrôle"));
        Assumptions.assumeTrue(ajouter.count() > 0,
            "Le panneau des controles de la transition ne propose pas d'ajout : non pilotable");
        String lien = ajouter.first().getAttribute("href");
        Assumptions.assumeTrue(lien != null && lien.contains("modifyControl"),
            "Le lien d'ajout de controle ne mene pas au formulaire attendu : " + lien);
        MacroSupport.navigate(ctx, "/" + lien.replaceFirst("^/", ""));

        Locator questions = page.locator("select[name='id_question']");
        Assumptions.assumeTrue(questions.count() > 0,
            "Le formulaire de controle ne propose pas de question cible : non pilotable");
        boolean choisie = AddConditionalControlMacroTest.selectByLabelContains(questions.first(), pilote.title);
        Assumptions.assumeTrue(choisie,
            "Question '" + pilote.title + "' absente de la liste des questions de l'etape source");
        AddConditionalControlMacroTest.clickSubmit(page, "view_modifyControl", "validateQuestion");

        Locator validateurs = page.locator("select[name='validatorName']");
        Assumptions.assumeTrue(
            validateurs.count() > 0
                && validateurs.locator("option[value='" + data.validatorName() + "']").count() > 0,
            "Type de controle '" + data.validatorName() + "' non propose pour la question '"
                + pilote.title + "' : ce type de question ne le supporte pas sur ce site");
        validateurs.first().selectOption(data.validatorName());
        AddConditionalControlMacroTest.clickSubmit(page, "view_modifyControl", "validateValidator");

        renseignerValeur(page, data.value());

        Locator ok = page.locator("button[name='action_modifyControl']");
        Assumptions.assumeTrue(ok.count() > 0, "Bouton d'enregistrement du controle absent");
        ok.first().click();
        page.waitForLoadState();

        int controlId = idControleCree(ctx, source.id, transitionId);
        Assertions.assertTrue(controlId > 0,
            "Un controle devrait etre attache a la transition de l'etape '" + source.title
                + "' apres enregistrement");
        ctx.controlIds.add(controlId);
    }

    /**
     * Renseigne la valeur attendue par le controle.
     *
     * <p>Sa nature depend du type retenu : une liste fermee pour un controle d'unicite, une saisie
     * libre pour une expression reguliere.</p>
     *
     * @param page   formulaire de controle
     * @param valeur valeur a appliquer
     */
    private static void renseignerValeur(Page page, String valeur) {
        Locator liste = page.locator("select[name='value']");
        if (liste.count() > 0) {
            try {
                liste.first().selectOption(valeur);
            } catch (RuntimeException parValeur) {
                try {
                    liste.first().selectOption(new com.microsoft.playwright.options.SelectOption()
                        .setLabel(valeur));
                } catch (RuntimeException parLibelle) {
                    Assumptions.assumeTrue(false,
                        "Valeur '" + valeur + "' absente de la liste du controle ; proposees : "
                            + liste.first().locator("option").allTextContents());
                }
            }
            return;
        }
        Locator saisie = page.locator("input[name='value'], textarea[name='value']");
        Assumptions.assumeTrue(saisie.count() > 0,
            "Aucun champ de valeur propose apres le choix du type de controle");
        saisie.first().fill(valeur);
    }

    /**
     * Identifiant du dernier controle attache a une transition.
     *
     * @param ctx          contexte du formulaire
     * @param stepId       identifiant de l'etape source
     * @param transitionId identifiant de la transition
     * @return l'identifiant du controle, ou -1 si aucun
     */
    private static int idControleCree(FormsContext ctx, int stepId, int transitionId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageControls.jsp?view=manageControl&id_step=" + stepId
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
     * Identifiant de la transition d'une etape menant vers une etape d'arrivee donnee.
     *
     * <p>Une etape qui bifurque porte plusieurs transitions, qui ne se distinguent que par leur
     * etape d'arrivee : conditionner la mauvaise inverserait silencieusement les deux parcours.
     * L'ecran les presente en blocs separes, chacun portant le titre de son etape d'arrivee et les
     * liens de gestion correspondants, d'ou la recherche par bloc.</p>
     *
     * @param ctx          contexte du formulaire
     * @param stepId       identifiant de l'etape source
     * @param titreArrivee titre de l'etape d'arrivee visee
     * @return l'identifiant de la transition, ou -1 si aucune ne mene a cette etape
     */
    private static int idTransitionVers(FormsContext ctx, int stepId, String titreArrivee) {
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + stepId);
        Locator liens = ctx.page.locator("a[href*='id_transition=']");
        for (int i = 0; i < liens.count(); i++) {
            String href = liens.nth(i).getAttribute("href");
            if (href == null) {
                continue;
            }
            // On remonte a la carte de la transition, et non au premier ancetre dont la classe
            // contient « card » : celui-ci est le bloc des boutons d'action (card-body), qui ne
            // porte pas le titre de l'etape d'arrivee.
            Locator bloc = liens.nth(i).locator(
                "xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' card ')][1]");
            if (bloc.count() == 0 || !bloc.first().innerText().contains(titreArrivee)) {
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

    /**
     * Identifiant de la transition sortante d'une etape.
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
        run(ctx, TransitionControlDataSet.unicite(0, 0, true));
    }
}
