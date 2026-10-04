package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.QuestionVisibilityDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.model.Parameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

/**
 * Visibilite d'une question sur l'etape de formulaire actuellement ouverte en front office.
 *
 * <p>Complement de {@link VerifyConditionalDisplayFOMacroTest}, qui rouvre le formulaire a sa
 * premiere etape pour y piloter lui-meme la question declenchante : utile pour un controle isole,
 * inutilisable au milieu d'un parcours, et sans effet sur une question pilote situee au-dela de la
 * premiere etape — la pilote restant introuvable, la verification est alors ignoree sans rien
 * prouver.</p>
 *
 * <p>Cette brique-ci ne navigue pas et ne saisit rien : elle constate, la ou le parcours en est.
 * C'est ce qui permet de verifier un affichage conditionnel sur une etape profonde, une fois la
 * question pilote renseignee par la brique de selection.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Verifier la visibilite d'une question sur l'etape courante")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class VerifyQuestionVisibilityFOMacroTest extends MacroTest {

    @Step("Verifier la visibilite d'une question en front office")
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) FormsContext ctx,
        QuestionVisibilityDataSet data) {

        Assertions.assertTrue(ctx.formId > 0,
            "Un formulaire publie doit exister (ctx.formId) avant la verification FO");

        Page page = ctx.page;
        boolean visible = libelleVisible(page, data.questionLabel());

        Assertions.assertEquals(data.expectVisible(), visible,
            "La question '" + data.questionLabel() + "' devrait etre "
            + (data.expectVisible() ? "affichee" : "masquee")
            + " sur l'etape ouverte en front office");
    }

    /**
     * Visibilite du libelle d'une question sur la page courante.
     *
     * <p>Le libelle est cherche, et non le champ de saisie : une question masquee par un controle
     * conditionnel garde son balisage dans le DOM, seul son conteneur est cache. Chercher le champ
     * le trouverait donc dans les deux cas.</p>
     *
     * @param page   page Playwright pilotant le navigateur
     * @param libelle libelle de la question
     * @return vrai si le libelle est rendu et visible
     */
    private static boolean libelleVisible(Page page, String libelle) {
        try {
            Locator loc = page.getByText(libelle);
            return loc.count() > 0 && loc.first().isVisible();
        } catch (RuntimeException absent) {
            return false;
        }
    }
}
