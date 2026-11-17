package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.GroupDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ListValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.MoveQuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionTargetDataSet;
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

import java.util.List;

/**
 * Brique de verification : un autre formulaire (copie, import) a la meme structure que le formulaire du
 * contexte, lue cote BO et independante des ids ({@link FormsNonRegSupport#structureOf}) : etapes (ordre, badges),
 * composants de chaque etape (questions et groupes dans l'ordre, groupe parent, type, obligation, valeurs de liste,
 * condition d'affichage, controle de validation, iterations des groupes), transitions (priorite, etape d'arrivee
 * par son rang dans le formulaire lu, controles). Une transition de la copie qui menerait encore a une etape de
 * l'original est marquee « hors formulaire » et fait echouer la comparaison (revue adversariale du 2026-10-30,
 * constat 3 : l'ancienne signature comparait les titres d'etapes et ignorait types, valeurs et groupes).
 *
 * <p>Lit : {@code ctx.formId} ; l'id compare est passe en parametre (souvent {@code ctx.duplicatedFormId}).</p>
 */
@Epic("Forms")
@Feature("Copie")
@Story("Verifier la structure d'une copie")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyFormStructureEqualMacroTest extends MacroTest {

    @Step("Verifier que le formulaire {otherFormId} a la structure du formulaire du contexte")
    public static void run(FormsContext ctx, int otherFormId) {
        Assertions.assertTrue(ctx.formId > 0 && otherFormId > 0 && otherFormId != ctx.formId, "Deux formulaires distincts attendus");
        List<String> original = FormsNonRegSupport.structureOf(ctx, ctx.formId);
        Assertions.assertFalse(original.isEmpty(), "Le formulaire " + ctx.formId + " doit avoir au moins une etape");
        Assertions.assertTrue(original.stream().noneMatch(line -> line.contains("hors formulaire")),
            "Le formulaire " + ctx.formId + " a une transition vers une etape d'un autre formulaire : " + original);
        Assertions.assertEquals(original, FormsNonRegSupport.structureOf(ctx, otherFormId),
            "Structure du formulaire " + otherFormId + " comparee a celle du formulaire " + ctx.formId);
    }

    @Test
    @DisplayName("La copie d'un formulaire a deux etapes, groupe, liste et question obligatoire a la meme structure")
    void standaloneVerifyFormStructureEqual() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Identite"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Coordonnees"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.RADIO, "Civilite"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom d'usage"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            VerifyQuestionsListedMacroTest.run(ctx);
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(1, "Madame", "mme"));
            AddListValueMacroTest.run(ctx, ListValueDataSet.create(1, "Monsieur", "m"));
            SetQuestionMandatoryMacroTest.run(ctx, QuestionTargetDataSet.of(0));
            CreateGroupMacroTest.run(ctx, GroupDataSet.repeatable("Adresse"));
            MoveQuestionMacroTest.run(ctx, MoveQuestionDataSet.toGroup(3, 1, 0));
            DuplicateFormFromListMacroTest.run(ctx);
            run(ctx, ctx.duplicatedFormId);
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
