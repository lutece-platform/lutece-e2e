package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : verifier quelle etape le front office affiche.
 *
 * <p>Lit : rien. Ecrit : rien.</p>
 *
 * <p>C'est le seul controle qui atteste qu'un embranchement a bien joue. Une transition
 * conditionnee qui ne l'est pas — condition mal enregistree, mauvaise transition visee, choix
 * retenu sur la mauvaise question — mene simplement ailleurs : les etapes s'enchainent, la reponse
 * se soumet, et le parcours alternatif n'est jamais emprunte sans que rien ne le signale.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Verifier l'etape atteinte")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyStepFOMacroTest extends MacroTest {

    @Step("Verifier l'etape affichee en front office")
    public static void run(FormsContext ctx, String titreAttendu) {
        ctx.page.waitForLoadState();
        String contenu = ctx.page.locator("body").innerText().replaceAll("\\s+", " ");
        Assertions.assertTrue(contenu.contains(titreAttendu),
            "Le front office devrait afficher l'etape '" + titreAttendu
                + "' : le parcours a bifurque ailleurs qu'attendu. Contenu lu : "
                + contenu.substring(0, Math.min(220, contenu.length())));
    }

    /**
     * Verifie qu'une etape donnee n'est PAS celle affichee.
     *
     * <p>Utile pour prouver qu'un embranchement a reellement ecarte l'autre branche, et pas
     * seulement atteint la bonne par hasard.</p>
     *
     * @param ctx         contexte formulaire courant
     * @param titreAbsent titre de l'etape qui ne doit pas etre affichee
     */
    @Step("Verifier qu'une etape n'est pas affichee en front office")
    public static void absente(FormsContext ctx, String titreAbsent) {
        ctx.page.waitForLoadState();
        String contenu = ctx.page.locator("body").innerText().replaceAll("\\s+", " ");
        Assertions.assertFalse(contenu.contains(titreAbsent),
            "Le front office ne devrait pas afficher l'etape '" + titreAbsent
                + "' sur ce parcours : l'embranchement n'a pas ecarte l'autre branche");
    }

    @Test
    @DisplayName("Verifier l'etape affichee en front office (auto-provisionnement + publication)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Question texte"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);
        run(ctx, "Etape unique");
    }
}
