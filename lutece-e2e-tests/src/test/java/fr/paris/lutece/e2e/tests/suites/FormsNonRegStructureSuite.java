package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Non-regression Forms 1/6 — structure : creation, modification et suppression de tout l'objet formulaire,
 * dans l'ordre CRUD.
 *
 * <p>Enchainement : categorie -> formulaire (rattache, modifie) -> 3 etapes liees automatiquement (ensemble
 * exact des transitions verifie) -> renommage d'etape -> 7 types de questions de base -> valeurs de liste
 * (ajout, modification, suppression) -> duplication et modification de question -> groupe (creation, deplacement
 * d'une question d'une autre etape dedans, renommage) -> modification de transition -> suppressions (question,
 * groupe avec son contenu, etape, formulaire, categorie).</p>
 *
 * <p>Execution (banc lpe2e forms, port selon {@code lpe2e port}) :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.FormsNonRegStructureSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 */
@Epic("Non-regression Forms")
@Feature("Structure")
@Tag("macro")
@Tag("suite")
@Tag("nonreg")
@ExtendWith(SkipIsFailureExtension.class)
@DisplayName("Non-regression Forms : structure du formulaire (CRUD)")
public class FormsNonRegStructureSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Categorie, formulaire, etapes, questions, valeurs, groupe, transitions : creer, modifier, supprimer")
    void structureDuFormulaire() {
        String suffix = newSuffix();
        login();
        FormsContext forms = new FormsContext(page, BASE_URL, suffix);
        try {
            // Categorie et formulaire
            CreateFormCategoryMacroTest.run(forms, FormCategoryDataSet.of("Demarches sociales", "social"));
            CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle("Demande d'aide"));
            AssignFormCategoryMacroTest.run(forms);
            ModifyFormMacroTest.run(forms, FormModifyDataSet.of("Demande d'aide sociale", "Formulaire de non-regression"));

            // Etapes : chaque creation lie l'etape precedente a la nouvelle
            CreateStepMacroTest.run(forms, StepDataSet.of("Demandeur"));
            CreateStepMacroTest.run(forms, StepDataSet.of("Situation"));
            CreateStepMacroTest.run(forms, StepDataSet.finalStep("Pieces"));
            VerifyTransitionTargetsMacroTest.run(forms, TransitionTargetsDataSet.of(0, 1));
            VerifyTransitionTargetsMacroTest.run(forms, TransitionTargetsDataSet.of(1, 2));
            VerifyTransitionTargetsMacroTest.run(forms, TransitionTargetsDataSet.of(2));
            RenameStepMacroTest.run(forms, StepTargetDataSet.of(1, "Situation familiale"));

            // Questions de base (index 0..6)
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXTAREA, "Motif de la demande"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.DATE, "Date de naissance"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.SELECT, "Arrondissement"));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.NUMBER, "Revenus mensuels").onStep(1));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, "Logement").onStep(1));
            AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.CHECKBOX, "Aides deja percues").onStep(1));
            VerifyQuestionsListedMacroTest.run(forms);

            // Valeurs de la question radio « Logement » (index 5)
            AddListValueMacroTest.run(forms, ListValueDataSet.create(5, "Locataire", "locataire"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(5, "Proprietaire", "proprietaire"));
            AddListValueMacroTest.run(forms, ListValueDataSet.create(5, "Heberge", "heberge"));
            ModifyListValueMacroTest.run(forms, ListValueDataSet.modify(5, 2, "Heberge gratuitement", "heberge_gratuit"));
            RemoveListValueMacroTest.run(forms, ListValueDataSet.target(5, 1));

            // Duplication puis modification de question (copie = index 7)
            DuplicateQuestionMacroTest.run(forms, QuestionTargetDataSet.of(0));
            ModifyQuestionMacroTest.run(forms, QuestionTargetDataSet.of(7, "Nom d'usage"));
            // ModifyQuestion cible la question par son texte : l'id renomme et les autres titres sont prouves par id.
            VerifyQuestionsListedMacroTest.run(forms);

            // Groupe dans la derniere etape, une question d'une autre etape y est deplacee
            CreateGroupMacroTest.run(forms, GroupDataSet.of("Pieces justificatives"));
            MoveQuestionMacroTest.run(forms, MoveQuestionDataSet.toGroup(7, 2, 0));
            ModifyGroupMacroTest.run(forms, GroupModifyDataSet.of(0, "Justificatifs"));

            // Transition : Demandeur -> Situation redirigee vers Pieces
            ModifyTransitionMacroTest.run(forms, TransitionModifyDataSet.of(0, 1, 2));
            VerifyTransitionTargetsMacroTest.run(forms, TransitionTargetsDataSet.of(0, 2));

            // Suppressions, en ordre inverse
            RemoveQuestionMacroTest.run(forms, QuestionTargetDataSet.of(1));
            RemoveGroupMacroTest.run(forms, GroupModifyDataSet.target(0));
            RemoveStepMacroTest.run(forms, StepTargetDataSet.of(1));
            RemoveFormMacroTest.run(forms);
            RemoveFormCategoryMacroTest.run(forms);
        } finally {
            FormsNonRegSupport.cleanup(forms);
        }
    }
}
