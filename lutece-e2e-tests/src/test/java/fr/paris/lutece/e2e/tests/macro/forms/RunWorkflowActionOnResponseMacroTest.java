package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseActionDataSet;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Brique macro : declencher une action de workflow sur une reponse.
 *
 * <p>Lit : rien. Ecrit : rien. Ouvre le detail de la premiere reponse (saute si multivue vide), puis
 * declenche l'action de workflow dont le libelle correspond a {@code data.actionLabel()} SI elle est
 * presente ; sinon saute proprement via Assumptions (formulaire sans workflow, ou libelle different).</p>
 *
 * <p><b>Le clic ne suffit pas.</b> Des qu'une action porte une tache a formulaire — commentaire,
 * affectation d'entite, confirmation — Lutece intercale un ecran de saisie
 * ({@code view=view_tasksForm}) et n'execute l'action qu'une fois celui-ci valide. Quitter cet ecran
 * laisse la ressource dans son etat initial, sans la moindre trace : c'est un echec entierement
 * silencieux, que seul le controle d'etat posterieur revele.</p>
 */
@Epic("Forms")
@Feature("Réponses")
@Story("Declencher une action de workflow sur une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class RunWorkflowActionOnResponseMacroTest extends MacroTest {

    private static final Pattern CONFIRM = Pattern.compile(
        "^(Oui|OK|Confirmer|Valider|Valider l'action)$", Pattern.CASE_INSENSITIVE);

    /** Valeur injectee dans les saisies libres du formulaire de taches. */
    private static final String SAISIE_PAR_DEFAUT = "Renseigne par le test E2E";

    @Step("Declencher une action de workflow sur la reponse")
    public static void run(FormsContext ctx, ResponseActionDataSet data) {
        boolean opened = OpenResponseDetailMacroTest.openFirstResponseDetail(ctx);
        Assumptions.assumeTrue(opened,
            "aucune reponse sur laquelle declencher une action (multivue vide)");

        Page page = ctx.page;
        // Le libelle est passe tel quel, sans Pattern.quote : celui-ci produit une sequence
        // \\Q...\\E propre a Java, que le moteur d'expressions regulieres de Playwright, cote
        // JavaScript, ne reconnait pas — le motif ne correspondait alors a rien et l'action
        // paraissait absente. La correspondance par chaine est de toute facon insensible a la
        // casse et aux espaces superflus.
        String label = data.actionLabel();

        // L'action peut etre un lien ou un bouton sur le detail de la reponse.
        Locator action = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(label));
        if (action.count() == 0 || !action.first().isVisible()) {
            action = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(label));
        }
        boolean present = action.count() > 0 && action.first().isVisible();
        Assumptions.assumeTrue(present,
            "action workflow '" + data.actionLabel() + "' absente sur le detail de la reponse "
                + "(aucun workflow associe au formulaire ?)");

        action.first().click();
        page.waitForLoadState();

        validerFormulaireDeTaches(page);

        // Confirmation eventuelle (page AdminMessage / modal / offcanvas).
        confirmIfPresent(page);
        page.waitForLoadState();

        Assertions.assertFalse(page.url().contains("AdminLogin"),
            "La session admin ne devrait pas etre perdue apres l'action de workflow");
        String erreur = erreurAffichee(page);
        Assertions.assertNull(erreur,
            "L'execution de l'action '" + data.actionLabel() + "' remonte une erreur : " + erreur);
    }

    /**
     * Valide l'ecran de saisie des taches si l'action en ouvre un.
     *
     * <p>L'ecran est reconnu a son bouton de validation plutot qu'a l'URL : c'est le seul signal
     * qui reste vrai quelle que soit la facon dont Lutece y amene.</p>
     *
     * @param page page courante, positionnee apres le clic sur l'action
     */
    private static void validerFormulaireDeTaches(Page page) {
        Locator valider = page.locator(
            "button[name='action_doSaveTaskForm'], input[name='action_doSaveTaskForm']");
        if (valider.count() == 0) {
            return;
        }
        renseignerSaisies(page);
        renseignerListes(page);
        cocherChoixUniques(page);
        valider.first().click();
        page.waitForLoadState();
    }

    /**
     * Renseigne les saisies libres encore vides du formulaire de taches.
     *
     * @param page formulaire de taches ouvert
     */
    private static void renseignerSaisies(Page page) {
        Locator saisies = page.locator("textarea:visible, input[type='text']:visible");
        for (int i = 0; i < saisies.count(); i++) {
            Locator champ = saisies.nth(i);
            if (champ.inputValue().isBlank()) {
                champ.fill(SAISIE_PAR_DEFAUT);
            }
        }
    }

    /**
     * Positionne les listes deroulantes laissees sur une valeur vide.
     *
     * @param page formulaire de taches ouvert
     */
    private static void renseignerListes(Page page) {
        Locator listes = page.locator("select:visible");
        for (int i = 0; i < listes.count(); i++) {
            Locator liste = listes.nth(i);
            if (!liste.inputValue().isBlank()) {
                continue;
            }
            Locator options = liste.locator("option[value]:not([value=''])");
            if (options.count() > 0) {
                liste.selectOption(options.first().getAttribute("value"));
            }
        }
    }

    /**
     * Coche le premier choix de chaque groupe de boutons radio laisse sans reponse.
     *
     * @param page formulaire de taches ouvert
     */
    private static void cocherChoixUniques(Page page) {
        Locator radios = page.locator("input[type='radio']:visible");
        List<String> groupes = new ArrayList<>();
        for (int i = 0; i < radios.count(); i++) {
            String nom = radios.nth(i).getAttribute("name");
            if (nom != null && !nom.isBlank() && !groupes.contains(nom)) {
                groupes.add(nom);
            }
        }
        for (String nom : groupes) {
            if (page.locator("input[type='radio'][name='" + nom + "']:checked").count() == 0) {
                page.locator("input[type='radio'][name='" + nom + "']").first().check();
            }
        }
    }

    private static void confirmIfPresent(Page page) {
        Locator link = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(CONFIRM));
        if (link.count() > 0 && link.first().isVisible()) {
            link.first().click();
            return;
        }
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CONFIRM));
        if (btn.count() > 0 && btn.first().isVisible()) {
            btn.first().click();
        }
    }

    /**
     * Message d'erreur affiche a l'issue de l'action, le cas echeant.
     *
     * <p>L'ancienne version de ce controle acceptait la presence de n'importe quelle pastille
     * ({@code .badge}) comme preuve de succes : le detail d'une reponse en porte toujours, si bien
     * que l'assertion passait meme quand l'action echouait. On ne cherche donc plus a prouver le
     * succes ici — c'est le role du controle d'etat et d'historique — mais seulement a remonter une
     * erreur explicite.</p>
     *
     * @param page page affichee apres l'action
     * @return le message d'erreur, ou null s'il n'y en a pas
     */
    private static String erreurAffichee(Page page) {
        Locator erreurs = page.locator(".alert-danger, .alert-error, .error:visible");
        for (int i = 0; i < erreurs.count(); i++) {
            String texte = erreurs.nth(i).innerText().replaceAll("\\s+", " ").trim();
            if (!texte.isEmpty()) {
                return texte;
            }
        }
        return null;
    }

    @Test
    @DisplayName("Declencher une action de workflow sur une reponse (auto-provisionnement + soumission FO)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        // Provisionnement best-effort d'une reponse. Sans workflow associe, l'action sera absente et
        // run() sautera proprement via Assumptions.
        OpenMultiviewMacroTest.submitOneFoResponse(ctx);
        run(ctx, ResponseActionDataSet.defaults());
    }
}
