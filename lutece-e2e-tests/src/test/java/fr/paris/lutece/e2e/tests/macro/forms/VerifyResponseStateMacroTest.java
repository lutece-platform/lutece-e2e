package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ResponseStateDataSet;
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
 * Brique macro : verifier l'etat d'une reponse et la trace laissee par les actions de workflow.
 *
 * <p>Lit : la multivue et le detail de la premiere reponse. Ecrit : rien dans le contexte.</p>
 *
 * <p>C'est le controle qui donne son sens a l'execution d'une action : sans lui, un clic qui
 * n'aboutit pas — droits insuffisants, tache mal configuree, prerequis non satisfait — passe pour
 * un succes. On verifie donc que la ressource a bien change d'etat, et que l'action figure dans
 * l'historique de la reponse.</p>
 */
@Epic("Forms")
@Feature("Multivue et instruction")
@Story("Verifier l'etat et l'historique d'une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyResponseStateMacroTest extends MacroTest {

    /** Titre de la section listant les actions jouees sur la reponse. */
    private static final String SECTION_HISTORIQUE = "Historique";

    @Step("Verifier l'etat et l'historique de la reponse")
    public static void run(FormsContext ctx, ResponseStateDataSet data) {
        boolean ouvert = OpenResponseDetailMacroTest.openFirstResponseDetail(ctx);
        Assumptions.assumeTrue(ouvert,
            "aucune reponse a inspecter dans la multivue : etat et historique non verifiables");

        Page page = ctx.page;
        String contenu = page.locator("body").innerText().replaceAll("\\s+", " ");

        if (data.expectedState() != null) {
            Assertions.assertTrue(contenu.contains(data.expectedState()),
                "Le detail de la reponse devrait porter l'etat '" + data.expectedState()
                    + "'. Contenu lu : " + extrait(contenu));
        }

        if (data.expectHistory()) {
            Assertions.assertFalse(historiqueVide(page, contenu),
                "L'historique de la reponse devrait porter la trace de l'action jouee, "
                    + "or il est vide : l'action n'a produit aucun effet sur la ressource");
        }

        if (data.expectedAction() != null) {
            Assertions.assertTrue(contenu.contains(data.expectedAction()),
                "L'historique de la reponse devrait mentionner l'action '" + data.expectedAction()
                    + "'. Contenu lu : " + extrait(contenu));
        }

        for (String trace : data.expectedTraces()) {
            Assertions.assertTrue(contenu.contains(trace),
                "L'historique de la reponse devrait porter la trace '" + trace
                    + "' laissee par une tache de l'action : cette tache n'a rien produit. "
                    + "Contenu lu : " + extrait(contenu));
        }
    }

    /**
     * Indique si la section d'historique de la reponse est vide.
     *
     * <p>Lutece rend une section « Historique » meme sans entree, avec une mention explicite : sa
     * seule presence ne prouve donc rien, c'est son contenu qui compte.</p>
     *
     * @param page     detail de la reponse
     * @param contenu  texte de la page, espaces normalises
     * @return true si aucune entree n'est presente
     */
    private static boolean historiqueVide(Page page, String contenu) {
        int debut = contenu.indexOf(SECTION_HISTORIQUE);
        if (debut < 0) {
            Locator entrees = page.locator("a[href*='id_history='], a[href*='id_action=']");
            return entrees.count() == 0;
        }
        // La mention de vacuite n'est cherchee qu'en tete de la section : le detail d'une reponse
        // comporte d'autres listes susceptibles d'etre vides, et la chercher dans toute la page
        // faisait conclure a un historique vide alors qu'il portait bien les actions jouees.
        String section = contenu.substring(debut + SECTION_HISTORIQUE.length());
        String entete = section.substring(0, Math.min(60, section.length()));
        return entete.contains("Aucun élément à afficher") || entete.contains("Aucun element a afficher");
    }

    /**
     * Extrait lisible du contenu de la page, pour les messages d'echec.
     *
     * @param contenu texte complet de la page
     * @return un extrait centre sur l'etat affiche
     */
    private static String extrait(String contenu) {
        int i = contenu.indexOf("Etat");
        if (i < 0) {
            i = contenu.indexOf("État");
        }
        if (i < 0) {
            return contenu.substring(0, Math.min(180, contenu.length()));
        }
        return contenu.substring(i, Math.min(i + 180, contenu.length()));
    }

    @Test
    @DisplayName("Verifier l'etat d'une reponse existante (ignore si la multivue est vide)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        run(ctx, ResponseStateDataSet.defaults());
    }
}
