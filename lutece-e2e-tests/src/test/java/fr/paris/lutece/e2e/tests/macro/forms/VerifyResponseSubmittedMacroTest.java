package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : PROUVER qu'une reponse a bien ete enregistree cote serveur.
 *
 * <p>Lit : {@code ctx.formId}. Ecrit : rien.</p>
 *
 * <p><b>Pourquoi cette brique existe.</b> {@code ValidateSummaryFO} ne peut constater qu'une chose :
 * qu'on a quitte l'ecran de recapitulatif sans message d'erreur. Or sur ce build, la validation renvoie
 * un {@code 302} vers un formulaire vierge <i>sans rien enregistrer</i> : le front-office est donc
 * incapable de distinguer un succes d'un echec silencieux. La seule preuve fiable est cote back-office,
 * dans la multivue des reponses — d'ou cette brique separee, a enchainer apres une soumission FO des
 * qu'un scenario a besoin de garantir que la reponse existe.</p>
 *
 * <p>Elle navigue en back-office : l'enchainer termine le parcours FO (les briques FO suivantes
 * devraient etre placees avant).</p>
 */
@Epic("Forms")
@Feature("Réponses")
@Story("Prouver l'enregistrement d'une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyResponseSubmittedMacroTest extends MacroTest {

    @Step("Prouver qu'une reponse est enregistree")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant de verifier l'enregistrement d'une reponse");

        Page page = ctx.page;
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "MultiviewForms.jsp");

        openAllResponsesPanel(page);

        int counter = readAllResponsesCounter(page);
        int rows = page.locator("table tbody tr").count();

        Assertions.assertTrue(counter > 0 || rows > 0,
            "Aucune reponse enregistree pour le formulaire '" + ctx.formTitle + "' : la multivue affiche "
            + counter + " reponse(s) et " + rows + " ligne(s). La validation du recapitulatif en "
            + "front-office renvoie un 302 sans erreur mais ne persiste rien (verifie : indexation "
            + "complete du portail relancee, formsIndexerDaemon execute, compteur toujours a 0).");
    }

    /**
     * Active le panneau « Toutes les reponses » de la multivue.
     *
     * <p>Le panneau selectionne par defaut depend des modules installes : avec {@code forms-unittree}
     * la multivue ouvre « Reponses de mon entite », qui est vide pour une reponse anonyme. Le comptage
     * des lignes porterait alors sur le mauvais panneau.</p>
     *
     * <p>Sans effet si l'onglet est absent ou deja actif.</p>
     */
    private static void openAllResponsesPanel(Page page) {
        Locator tab = allResponsesTab(page);
        if (tab.count() == 0 || "true".equals(tab.first().getAttribute("aria-selected"))) {
            return;
        }
        tab.first().click();
        page.waitForLoadState();
    }

    /**
     * Resout l'onglet « Toutes les reponses » de la multivue, par son identifiant puis par son intitule.
     *
     * @param page
     *            la page de la multivue
     * @return le locator de l'onglet, vide si la multivue ne l'expose pas
     */
    private static Locator allResponsesTab(Page page) {
        Locator byId = page.locator("a#multiviewforms[role='tab']");
        if (byId.count() > 0) {
            return byId;
        }
        return page.locator("a[role='tab']")
            .filter(new Locator.FilterOptions().setHasText("Toutes les réponses"));
    }

    /**
     * Lit le compteur du panneau « Toutes les reponses ».
     *
     * @param page
     *            la page de la multivue
     * @return le nombre de reponses annonce par l'onglet, ou -1 s'il est illisible
     */
    private static int readAllResponsesCounter(Page page) {
        Locator tab = allResponsesTab(page);
        if (tab.count() == 0) {
            return -1;
        }
        String text;
        try {
            text = tab.first().textContent();
        } catch (RuntimeException detached) {
            return -1;
        }
        if (text == null) {
            return -1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)")
            .matcher(text.replaceAll("\\s+", " ").trim());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    @Test
    @DisplayName("Prouver l'enregistrement d'une reponse (auto-provisionnement + soumission FO)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, fr.paris.lutece.e2e.tests.macro.data.FormDataSet.defaults());
        CreateStepMacroTest.run(ctx,
            fr.paris.lutece.e2e.tests.macro.data.StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet.of(
            fr.paris.lutece.e2e.tests.macro.data.QuestionType.TEXT, "Question texte"));
        PublishFormMacroTest.run(ctx, fr.paris.lutece.e2e.tests.macro.data.PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);
        FillFieldFOMacroTest.run(ctx, fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet.of(
            "Question texte", "Preuve de soumission"));
        ViewSummaryFOMacroTest.run(ctx);
        ValidateSummaryFOMacroTest.run(ctx);
        run(ctx);
    }
}
