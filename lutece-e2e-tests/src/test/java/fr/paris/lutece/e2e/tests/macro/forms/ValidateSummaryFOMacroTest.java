package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
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
 * Brique macro : valider le recapitulatif et soumettre la reponse en front-office
 * ("Valider le recapitulatif").
 *
 * <p>Lit : {@code ctx.formId} et la page FO courante (recapitulatif affiche). Ecrit :
 * {@code ctx.lastResponseId} si l'id de reponse est derivable de l'URL. Le bouton "Valider le
 * recapitulatif" n'existe que sur l'ecran de recapitulatif : s'il est absent, la brique est ignoree
 * (Assumptions). Sinon on clique, on attend NETWORKIDLE et on verifie au mieux l'accuse de reception
 * de la soumission.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Valider le recapitulatif en front-office")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class ValidateSummaryFOMacroTest extends MacroTest {

    @Step("Valider le recapitulatif en front-office")
    public static void run(FormsContext ctx) {
        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire doit exister (ctx.formId) avant de valider le recapitulatif en front-office");

        Page page = ctx.page;
        page.waitForLoadState();

        Locator validate = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Valider le récapitulatif"));
        boolean present = validate.count() > 0 && validate.first().isVisible();
        Assumptions.assumeTrue(present,
            "Bouton 'Valider le récapitulatif' absent (recapitulatif non affiche ou flux different) : "
                + "brique ignoree");

        validate.first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        String url = page.url();
        // Succes de soumission : on a quitte l'ecran de recapitulatif (le bouton "Valider le
        // recapitulatif" n'est plus la) SANS message d'erreur/validation. Sur ce site, la soumission
        // reussie redirige vers le stepView du formulaire (et non vers page=formsResponse), tout en
        // creant bien la reponse cote serveur. On peut aussi tomber sur un accuse explicite.
        Locator validateAfter = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Valider le récapitulatif"));
        boolean leftRecap = validateAfter.count() == 0 || !validateAfter.first().isVisible();
        boolean explicitAck = url.contains("page=formsResponse")
            || isTextVisible(page, "succès") || isTextVisible(page, "enregistrée");
        // Detection d'une VRAIE erreur de validation. Ne PAS matcher "obligatoire" tout court : la
        // legende generique "Les champs suivis d'un asterisque * sont obligatoires." est toujours
        // affichee et provoquerait un faux echec. On cible l'erreur champ ("est obligatoire", singulier)
        // et une alerte rouge reellement VISIBLE et renseignee (les .alert-danger vides du theme = de
        // simples placeholders "Titre de l'alerte", caches).
        boolean hasError = isTextVisible(page, "est obligatoire")
            || isTextVisible(page, "Veuillez")
            || hasVisibleErrorAlert(page);
        Assertions.assertTrue((leftRecap && !hasError) || explicitAck,
            "La soumission du recapitulatif aurait du etre acceptee (sortie du recap sans erreur, ou "
                + "accuse explicite) ; url courante: " + url);

        Integer responseId = parseResponseId(url);
        if (responseId != null) {
            ctx.lastResponseId = responseId;
        }
    }

    /** Extrait l'id de reponse de l'URL (plusieurs conventions possibles), ou {@code null}. */
    private static Integer parseResponseId(String url) {
        if (url == null) {
            return null;
        }
        for (String key : new String[] {"id_response=", "id_form_response=", "id_formResponse=", "id_history="}) {
            if (url.contains(key)) {
                try {
                    return Integer.parseInt(url.split(key)[1].split("&")[0].split("#")[0]);
                } catch (NumberFormatException ignored) {
                    // convention presente mais valeur non numerique : on tente la suivante
                }
            }
        }
        return null;
    }

    private static boolean isTextVisible(Page page, String text) {
        try {
            Locator loc = page.getByText(text);
            return loc.count() > 0 && loc.first().isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Vrai s'il existe une alerte {@code .alert-danger} VISIBLE et renseignee (contenu reel).
     * Exclut les blocs placeholders du theme (texte "Titre de l'alerte", generalement caches).
     */
    private static boolean hasVisibleErrorAlert(Page page) {
        try {
            Locator alerts = page.locator(".alert-danger");
            int n = alerts.count();
            for (int i = 0; i < n; i++) {
                Locator a = alerts.nth(i);
                if (!a.isVisible()) {
                    continue;
                }
                String txt = a.textContent();
                if (txt != null && !txt.isBlank() && !txt.contains("Titre de l'alerte")) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            // pas d'alerte exploitable
        }
        return false;
    }

    @Test
    @DisplayName("Valider le recapitulatif en FO (auto-provisionnement + ouverture FO + saisie ; ignoree si absent)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Champ FO"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);
        FillFieldFOMacroTest.run(ctx, FieldValueDataSet.of("Champ FO", "Valeur E2E macro"));
        // Passer a l'ecran de recapitulatif avant de valider (mono-etape : pas d'"Etape suivante").
        ViewSummaryFOMacroTest.run(ctx);
        run(ctx);
    }
}
