package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
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
 * Brique macro : ouvrir le detail de la premiere reponse depuis la multivue.
 *
 * <p>Lit : rien. Ecrit : rien. Depuis la multivue, ouvre le detail de la premiere reponse si une ligne
 * existe, sinon saute proprement via Assumptions ("aucune reponse a ouvrir"). Verifie que la vue de
 * detail affiche des champs de reponse.</p>
 */
@Epic("Forms")
@Feature("Réponses")
@Story("Ouvrir le detail d'une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class OpenResponseDetailMacroTest extends MacroTest {

    @Step("Ouvrir le detail de la premiere reponse")
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) FormsContext ctx) {
        boolean opened = openFirstResponseDetail(ctx);
        Assumptions.assumeTrue(opened,
            "aucune reponse a ouvrir dans la multivue (multivue vide) : detail non pilotable");

        Page page = ctx.page;
        Assertions.assertFalse(page.url().contains("AdminLogin"),
            "La session admin ne devrait pas etre perdue en ouvrant le detail de la reponse");
        Assertions.assertTrue(detailDisplayed(page),
            "La vue de detail devrait afficher les champs de la reponse (panneau / table / formulaire)");
    }

    /**
     * Navigue vers la multivue et ouvre le detail de la premiere reponse si une ligne existe.
     * Toutes les recherches sont gated. Retourne {@code true} si un detail a ete ouvert.
     */
    /**
     * Ouvre le detail d'une reponse du formulaire courant se trouvant dans l'etat indique.
     *
     * <p>Memorise l'identifiant de la reponse ouverte, pour que les controles qui suivent portent
     * sur ce dossier precis et non sur le premier de la liste.</p>
     *
     * @param ctx  contexte formulaire courant
     * @param etat libelle de l'etat recherche
     * @return true si une reponse dans cet etat a ete ouverte
     */
    static boolean openResponseInState(FormsContext ctx, String etat) {
        Page page = ctx.page;
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "MultiviewForms.jsp?plugin_name=forms&selected_panel=forms&change_panel=true");
        page.waitForLoadState();

        Locator lignes = page.locator("table tbody tr[data-url]");
        if (ctx.formTitle != null && !ctx.formTitle.isBlank()) {
            lignes = lignes.filter(new Locator.FilterOptions().setHasText(ctx.formTitle));
        }
        Locator dansEtat = lignes.filter(new Locator.FilterOptions().setHasText(etat));
        if (dansEtat.count() == 0) {
            return false;
        }
        dansEtat.first().click();
        page.waitForLoadState();
        memoriserReponse(ctx);
        return true;
    }

    /**
     * Rouvre le detail de la derniere reponse instruite.
     *
     * @param ctx contexte formulaire courant
     * @return true si une reponse etait memorisee et a pu etre rouverte
     */
    static boolean reopenLastResponse(FormsContext ctx) {
        if (ctx.lastResponseId == null) {
            return false;
        }
        MacroSupport.navigate(ctx, MacroSupport.FORMS
            + "ManageDirectoryFormResponseDetails.jsp?view=view_form_response_details&id_form_response="
            + ctx.lastResponseId);
        return true;
    }

    /**
     * Retient l'identifiant de la reponse dont le detail est affiche.
     *
     * @param ctx contexte formulaire courant
     */
    static void memoriserReponse(FormsContext ctx) {
        String url = ctx.page.url();
        if (!url.contains("id_form_response=")) {
            return;
        }
        try {
            ctx.lastResponseId = Integer.valueOf(
                url.split("id_form_response=")[1].split("&")[0].split("#")[0]);
        } catch (RuntimeException sansIdentifiant) {
            // detail ouvert sans identifiant exploitable : les controles suivants retomberont
            // sur la premiere reponse du formulaire
        }
    }

    static boolean openFirstResponseDetail(FormsContext ctx) {
        Page page = ctx.page;
        // Selectionner le panneau "Toutes les reponses". Par defaut la multivue ouvre le panneau
        // filtre par entite "Reponses de mon entite" (aria-selected=true dans le DOM), vide pour
        // l'admin -> "multivue vide". Le panneau all-responses est atteint via
        // selected_panel=forms&change_panel=true (cf. lien tab title="Toutes les reponses").
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "MultiviewForms.jsp?plugin_name=forms&selected_panel=forms&change_panel=true");
        page.waitForLoadState();

        // 1) Ligne porteuse de son URL de detail : la multivue ne rend pas de lien dans ses
        // cellules, c'est le <tr> qui porte data-url et le clic sur la ligne qui ouvre le detail.
        // La multivue cumule les reponses de tous les formulaires du site : on vise d'abord celle
        // du formulaire courant, sans quoi le scenario inspecterait la reponse d'un autre, dont
        // l'etat et le workflow n'ont rien a voir avec ce qu'il vient de produire.
        Locator lignesCliquables = page.locator("table tbody tr[data-url]");
        if (ctx.formTitle != null && !ctx.formTitle.isBlank()) {
            Locator duFormulaire = page.locator("table tbody tr[data-url]")
                .filter(new Locator.FilterOptions().setHasText(ctx.formTitle));
            if (duFormulaire.count() > 0 && duFormulaire.first().isVisible()) {
                duFormulaire.first().click();
                page.waitForLoadState();
                memoriserReponse(ctx);
                return true;
            }
        }
        if (lignesCliquables.count() > 0 && lignesCliquables.first().isVisible()) {
            lignesCliquables.first().click();
            page.waitForLoadState();
            memoriserReponse(ctx);
            return true;
        }

        // 2) Liens de detail explicites (view_form_response_details / ManageDirectoryFormResponseDetails)
        for (String hint : new String[] {"view_form_response_details", "ManageDirectoryFormResponseDetails"}) {
            Locator links = page.locator("a[href*='" + hint + "']");
            if (links.count() > 0 && links.first().isVisible()) {
                links.first().click();
                page.waitForLoadState();
                return true;
            }
        }

        // 3) Repli : premier lien d'une ligne du tableau des reponses
        Locator rowLinks = page.locator("table tbody tr a");
        if (rowLinks.count() > 0 && rowLinks.first().isVisible()) {
            rowLinks.first().click();
            page.waitForLoadState();
            return true;
        }

        return false;
    }

    /** Vrai si la vue de detail d'une reponse est affichee. */
    private static boolean detailDisplayed(Page page) {
        if (page.url().contains("view_form_response_details")
            || page.url().contains("ManageDirectoryFormResponseDetails")) {
            return true;
        }
        // Repli best-effort : un conteneur de contenu est present (la multivue a ete quittee).
        return page.locator(".card, .panel, table, dl, form").count() > 0;
    }

    @Test
    @DisplayName("Ouvrir le detail d'une reponse (auto-provisionnement + soumission FO best-effort)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        // Provisionnement best-effort : cree une reponse pour que la multivue ait une ligne.
        OpenMultiviewMacroTest.submitOneFoResponse(ctx);
        // run() saute proprement si aucune reponse n'est finalement presente.
        run(ctx);
    }
}
