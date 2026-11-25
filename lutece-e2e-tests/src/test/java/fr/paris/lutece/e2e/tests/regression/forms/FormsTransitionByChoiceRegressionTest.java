package fr.paris.lutece.e2e.tests.regression.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.ChoiceDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ResponseValuesDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionControlDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionDataSet;
import fr.paris.lutece.e2e.tests.macro.forms.AddListValueMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.AddQuestionMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.AddTransitionControlMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.ChooseValueFOMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.ClearStepTransitionsMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.CreateFormMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.CreateStepMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.CreateTransitionMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.GoToNextStepFOMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.GoToSummaryFOMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.ModifyListValueMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.OpenFormFOMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.PublishFormMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.ValidateSummaryFOMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.VerifyQuestionsListedMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.VerifyResponseRecordedMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.VerifyResponseValuesMacroTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cas remonte 3446 : liaison entre etapes selon le choix d'un bouton radio. Formulaire « Vegetaliser Paris » : la
 * question « Que voulez-vous vegetaliser ? » de l'etape 1 propose Arbre, Mur et Autre ; chaque choix mene, par une
 * transition conditionnee sur sa valeur, a l'etape qui le traite (2, 3 ou 4), et les trois branches se rejoignent sur
 * l'etape finale 5. Remonte : le 1er choix (« Arbre », d'abord nomme « Pied d'arbre ») donne « erreur technique -
 * veuillez contacter votre webmaster », Mur et Autre passent. Meme symptome que LUT-34468 (annule, sans correctif).
 *
 * <p>Les trois choix sont joues dans l'ordre Mur, Autre, puis le 1er choix, pour que le rapport montre les branches
 * qui passent avant celle qui casse. Chaque parcours va jusqu'a la reponse relue en BO. Trois variantes du 1er choix :
 * « Arbre », « Pied d'arbre » (apostrophe), et « Pied d'arbre » renomme « Arbre » apres la pose des liaisons.</p>
 *
 * <p>Constat du 2026-11-24 (forms {@code develop} ffaa5ccc) : non reproduit, les trois variantes sont vertes, de meme
 * qu'un montage qui garde les liaisons creees automatiquement entre etapes. Une cause connue donne ce symptome :
 * une question source d'une condition de transition supprimee, la condition restant en base (LUT-34960, 4.1.0).</p>
 *
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.regression.forms.FormsTransitionByChoiceRegressionTest \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Forms")
@Feature("Cas remontes")
@Story("Etape suivante selon le choix d'un bouton radio")
@Tag("macro")
@Tag("forms")
@Tag("regression")
public class FormsTransitionByChoiceRegressionTest extends MacroTest {

    private static final String QUESTION = "Que voulez-vous végétaliser ?";

    private static final int ETAPE_CHOIX = 0;

    private static final int ETAPE_FINALE = 4;

    @Test
    @DisplayName("Végétaliser Paris : chaque choix (Arbre, Mur, Autre) mène à son étape")
    void eachChoiceLeadsToItsStep() {
        parcours("Arbre", "arbre", null);
    }

    @Test
    @DisplayName("Végétaliser Paris : le choix « Pied d'arbre » mène à son étape")
    void choiceWithApostropheLeadsToItsStep() {
        parcours("Pied d'arbre", "pied_arbre", null);
    }

    @Test
    @DisplayName("Végétaliser Paris : « Pied d'arbre » renommé « Arbre » après la pose des liaisons mène à son étape")
    void choiceRenamedAfterTheConditionLeadsToItsStep() {
        parcours("Pied d'arbre", "pied_arbre", "Arbre");
    }

    /**
     * Construit le formulaire a embranchement puis joue les trois choix en FO.
     *
     * @param premierChoix libelle du 1er choix, celui qui casse dans le cas remonte
     * @param codePremier  code de ce choix
     * @param renommeEn    nouveau libelle du 1er choix, donne apres la pose des liaisons, ou null pour le garder
     */
    private void parcours(String premierChoix, String codePremier, String renommeEn) {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults().withTitle("Végétaliser Paris"));
            CreateStepMacroTest.run(ctx, StepDataSet.of("Que végétaliser"));
            CreateStepMacroTest.run(ctx, StepDataSet.of("Arbre"));
            CreateStepMacroTest.run(ctx, StepDataSet.of("Mur"));
            CreateStepMacroTest.run(ctx, StepDataSet.of("Autre"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Fin"));

            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, QUESTION).onStep(ETAPE_CHOIX));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Essence souhaitée").onStep(1));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Surface du mur").onStep(2));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Précisez").onStep(3));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Commentaire").onStep(ETAPE_FINALE));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, premierChoix, codePremier));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Mur", "mur"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(0, "Autre", "autre"));

            // La creation d'une etape la relie a la precedente : ces liaisons lineaires sont retirees avant de poser
            // l'embranchement, sinon la premiere, prioritaire, l'emporte.
            for (int etape = 0; etape < ctx.steps.size(); etape++) {
                ClearStepTransitionsMacroTest.run(ctx, StepTargetDataSet.of(etape));
            }
            CreateTransitionMacroTest.run(ctx, TransitionDataSet.of(ETAPE_CHOIX, 1));
            CreateTransitionMacroTest.run(ctx, TransitionDataSet.of(ETAPE_CHOIX, 2));
            CreateTransitionMacroTest.run(ctx, TransitionDataSet.of(ETAPE_CHOIX, 3));
            for (int branche = 1; branche <= 3; branche++) {
                CreateTransitionMacroTest.run(ctx, TransitionDataSet.of(branche, ETAPE_FINALE));
            }
            AddTransitionControlMacroTest.run(ctx, TransitionControlDataSet.valeurChoisie(ETAPE_CHOIX, 1, 0, premierChoix));
            AddTransitionControlMacroTest.run(ctx, TransitionControlDataSet.valeurChoisie(ETAPE_CHOIX, 2, 0, "Mur"));
            AddTransitionControlMacroTest.run(ctx, TransitionControlDataSet.valeurChoisie(ETAPE_CHOIX, 3, 0, "Autre"));
            String choixJoue = premierChoix;
            if (renommeEn != null) {
                ModifyListValueMacroTest.run(ctx, ListValueDataSet.modify(0, 0, renommeEn, codePremier));
                choixJoue = renommeEn;
            }
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());

            choisir(ctx, "Mur", 2);
            choisir(ctx, "Autre", 3);
            choisir(ctx, choixJoue, 1);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }

    /**
     * Parcours FO complet pour un choix : etape de la branche attendue, etape finale, recapitulatif, reponse relue.
     *
     * @param ctx     contexte du formulaire
     * @param choix   libelle du choix coche a l'etape 1
     * @param branche position de l'etape qui traite ce choix
     */
    private static void choisir(FormsContext ctx, String choix, int branche) {
        OpenFormFOMacroTest.run(ctx);
        ChooseValueFOMacroTest.run(ctx, ChoiceDataSet.of(0, choix));
        GoToNextStepFOMacroTest.run(ctx, StepTargetDataSet.of(branche));
        GoToNextStepFOMacroTest.run(ctx, StepTargetDataSet.of(ETAPE_FINALE));
        GoToSummaryFOMacroTest.run(ctx);
        ValidateSummaryFOMacroTest.run(ctx);
        VerifyResponseRecordedMacroTest.run(ctx, ResponseCheckDataSet.defaults());
        VerifyResponseValuesMacroTest.run(ctx, ResponseValuesDataSet.of(QUESTION, choix));
    }
}
