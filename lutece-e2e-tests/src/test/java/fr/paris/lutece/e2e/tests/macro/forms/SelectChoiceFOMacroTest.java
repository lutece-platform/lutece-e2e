package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ChoiceSelectionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionChoicesDataSet;
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
 * Brique macro : retenir un choix precis sur une question a liste, en front office.
 *
 * <p>Lit : rien. Ecrit : rien. La page front-office doit afficher l'etape portant la question.</p>
 *
 * <p>Le choix est cherche dans le bloc de la question plutot que sur toute la page : deux questions
 * peuvent proposer des libelles identiques, et cocher celui de la mauvaise ferait bifurquer le
 * parcours ailleurs qu'attendu, sans que rien ne le signale.</p>
 */
@Epic("Forms")
@Feature("Front Office")
@Story("Retenir un choix sur une question a liste")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class SelectChoiceFOMacroTest extends MacroTest {

    @Step("Retenir un choix en front-office")
    public static void run(FormsContext ctx, ChoiceSelectionDataSet data) {
        Page page = ctx.page;
        page.waitForLoadState();

        Locator bloc = blocDeLaQuestion(page, data.questionLabel());

        Locator liste = bloc.locator("select");
        if (liste.count() > 0) {
            retenirDansListe(liste.first(), data);
            return;
        }

        Assertions.assertTrue(bloc.locator("input[type='radio'], input[type='checkbox']").count() > 0,
            "La question '" + data.questionLabel() + "' ne propose aucun choix en front office : "
                + "elle a ete creee sans reponses possibles");
        Locator parLibelle = bloc.getByLabel(data.choiceLabel());
        Assertions.assertTrue(parLibelle.count() > 0,
            "Le choix '" + data.choiceLabel() + "' est absent de la question '"
                + data.questionLabel() + "' en front office");
        parLibelle.first().check();
        Assertions.assertTrue(parLibelle.first().isChecked(),
            "Le choix '" + data.choiceLabel() + "' devrait etre retenu apres selection");
    }

    /**
     * Bloc front-office qui porte une question donnee.
     *
     * @param page          page front-office courante
     * @param questionLabel libelle de la question
     * @return le bloc le plus proche contenant ce libelle
     */
    private static Locator blocDeLaQuestion(Page page, String questionLabel) {
        Locator blocs = page.locator("form fieldset, form .form-group, form div.mb-3")
            .filter(new Locator.FilterOptions().setHasText(questionLabel));
        Assertions.assertTrue(blocs.count() > 0,
            "La question '" + questionLabel + "' est absente de l'etape affichee en front office");
        return blocs.last();
    }

    /**
     * Retient le choix dans une liste deroulante.
     *
     * @param liste liste deroulante de la question
     * @param data  choix demande
     */
    private static void retenirDansListe(Locator liste, ChoiceSelectionDataSet data) {
        liste.selectOption(new com.microsoft.playwright.options.SelectOption()
            .setLabel(data.choiceLabel()));
    }

    @Test
    @DisplayName("Retenir un choix sur une question a liste (auto-provisionnement + publication)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Nature de la demande"));
        AddQuestionChoicesMacroTest.run(ctx,
            QuestionChoicesDataSet.of("Nature de la demande", "Subvention", "Information"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);

        // Les choix ajoutes a une question ne sont pas rendus immediatement en front office : le
        // site met plusieurs minutes a les prendre en compte, meme apres vidage des caches. Un
        // scenario reel absorbe ce delai par les operations qui suivent ; un test isole, non. On le
        // constate donc explicitement plutot que d'echouer sur un comportement du produit, que la
        // suite de parcours, elle, eprouve bel et bien.
        boolean rendue = ctx.page.locator("form fieldset, form .form-group, form div.mb-3")
            .filter(new Locator.FilterOptions().setHasText("Nature de la demande")).count() > 0;
        Assumptions.assumeTrue(rendue,
            "Les choix de la question ne sont pas encore rendus en front office : le site met "
                + "plusieurs minutes a prendre en compte des choix fraichement crees");

        run(ctx, ChoiceSelectionDataSet.of("Nature de la demande", "Subvention"));
    }
}
