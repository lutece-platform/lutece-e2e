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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : declarer le mapping de notification d'un formulaire.
 *
 * <p>Lit : {@code ctx.formTitle}. Ecrit : rien dans le contexte.</p>
 *
 * <p>La tache qui notifie l'usager ne lit pas la reponse directement : elle passe par un mapping
 * qui indique, pour un formulaire donne, quelles questions portent l'adresse, le telephone et les
 * identifiants de l'usager. Sans ce mapping, la tache s'enregistre et se configure normalement,
 * mais l'action echoue a l'execution sur un {@code No mapping found for the form}, et toute la
 * transaction est annulee : la ressource ne change pas d'etat et l'historique reste vide.</p>
 *
 * <p>Les correspondances sont laissees a « Aucun » : le scenario verifie que la notification
 * s'execute et laisse sa trace, non qu'un courriel part reellement, ce qui supposerait une
 * passerelle d'envoi.</p>
 */
@Epic("Forms")
@Feature("Notifications")
@Story("Declarer le mapping de notification d'un formulaire")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class CreateNotifygruMappingMacroTest extends MacroTest {

    /** Chemin du module de gestion des mappings de notification. */
    private static final String MAPPING = "/jsp/admin/plugins/modulenotifygrumappingmanager/";

    /** Delai de rechargement des listes apres le choix du fournisseur, en millisecondes. */
    private static final double RECHARGEMENT_MS = 5000;

    @Step("Declarer le mapping de notification du formulaire")
    public static void run(FormsContext ctx) {
        Assertions.assertNotNull(ctx.formTitle,
            "Un formulaire doit exister avant de declarer son mapping de notification");

        Page page = ctx.page;
        MacroSupport.navigate(ctx,
            MAPPING + "ManageNotifygruMappingManagers.jsp?plugin_name=modulenotifygrumappingmanager");

        Locator ajouter = page.locator("button[name='view_createNotifygruMappingManager'], "
            + "input[name='view_createNotifygruMappingManager']");
        Assertions.assertTrue(ajouter.count() > 0,
            "L'ecran des mappings de notification ne propose pas d'ajout : module absent ou droits "
                + "insuffisants");
        ajouter.first().click();
        page.waitForLoadState();

        choisirFournisseur(page, ctx.formTitle);
        page.locator("input[name='demandetype']").first().fill("1");

        page.locator("button[name='action_createNotifygruMappingManager'], "
            + "input[name='action_createNotifygruMappingManager']").first().click();
        page.waitForLoadState();

        String contenu = page.locator("body").innerText().replaceAll("\\s+", " ");
        Assertions.assertTrue(contenu.contains(ctx.formTitle),
            "Le mapping du formulaire '" + ctx.formTitle + "' devrait figurer dans la liste apres "
                + "enregistrement. Contenu lu : " + contenu.substring(0, Math.min(200, contenu.length())));
    }

    /**
     * Retient le fournisseur correspondant au formulaire et attend le rechargement des listes.
     *
     * <p>Les listes de correspondances sont alimentees par les questions du formulaire choisi :
     * elles ne sont renseignables qu'une fois le fournisseur retenu et la page rafraichie.</p>
     *
     * @param page       formulaire de creation du mapping
     * @param titreForm  titre du formulaire a mapper
     */
    private static void choisirFournisseur(Page page, String titreForm) {
        Locator liste = page.locator("select[name='beankey']");
        Assertions.assertTrue(liste.count() > 0,
            "Le formulaire de mapping ne propose pas de fournisseur de donnees");

        Locator options = liste.locator("option");
        for (int i = 0; i < options.count(); i++) {
            String libelle = options.nth(i).innerText();
            if (libelle != null && libelle.contains(titreForm)) {
                liste.first().selectOption(options.nth(i).getAttribute("value"));
                attendreRechargement(page, titreForm);
                return;
            }
        }
        Assertions.fail("Aucun fournisseur de donnees ne correspond au formulaire '" + titreForm
            + "' : le formulaire n'est pas expose aux notifications");
    }

    /**
     * Attend que les listes de correspondances refletent le formulaire retenu.
     *
     * @param page      formulaire de creation du mapping
     * @param titreForm titre du formulaire retenu, pour le diagnostic
     */
    private static void attendreRechargement(Page page, String titreForm) {
        try {
            page.waitForFunction(
                "() => { const s = document.querySelector(\"select[name='email']\");"
                    + " return s && s.options.length > 1; }",
                null, new Page.WaitForFunctionOptions().setTimeout(RECHARGEMENT_MS));
        } catch (RuntimeException sansQuestion) {
            // Formulaire sans question exploitable : les correspondances resteront a « Aucun »,
            // ce qui suffit au mapping.
        }
    }

    @Test
    @DisplayName("Declarer le mapping de notification d'un formulaire (auto-provisionnement)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Courriel"));
        run(ctx);
    }
}
