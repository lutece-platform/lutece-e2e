package fr.paris.lutece.e2e.tests.macro.workflow;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowSupport;
import fr.paris.lutece.e2e.tests.macro.data.ActionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StateDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TaskConfigDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TaskDataSet;
import fr.paris.lutece.e2e.tests.macro.data.WorkflowDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : parametrer la derniere tache ajoutee a une action.
 *
 * <p>Lit : {@code ctx.taskIds}. Ecrit : rien dans le contexte.</p>
 *
 * <p><b>Pourquoi une brique distincte de l'ajout.</b> Inserer une tache ne la configure pas : elle
 * est creee avec un parametrage vide, et son formulaire vit sur une page separee
 * ({@code ModifyTask.jsp?id_task=...}), inaccessible depuis la page de l'action. Une tache non
 * parametree est inoperante — une mise a jour de statut sans choix publie/depublie, une affectation
 * sans mode d'assignation ni strategie de selection d'entite — et plusieurs de ces formulaires
 * refusent l'enregistrement tant que leurs champs obligatoires ne sont pas renseignes.</p>
 */
@Epic("Workflow")
@Feature("Actions et taches")
@Story("Parametrer une tache")
@Tag("macro")
@Tag("workflow")
@Tag("brick")
public class ConfigureTaskMacroTest extends MacroTest {

    @Step("Parametrer la tache")
    public static void run(WorkflowContext ctx, TaskConfigDataSet data) {
        Assertions.assertFalse(ctx.taskIds.isEmpty(),
            "Une tache doit avoir ete ajoutee (ctx.taskIds) avant d'etre parametree");

        int taskId = ctx.taskIds.get(ctx.taskIds.size() - 1);
        Page page = ctx.page;
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyTask.jsp?id_task=" + taskId);

        for (TaskConfigDataSet.Reglage reglage : data.reglages()) {
            appliquer(page, reglage);
        }

        Locator enregistrer = page.locator("button[name='save'], input[name='save']");
        if (enregistrer.count() == 0) {
            enregistrer = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Enregistrer"));
        }
        enregistrer.first().click();
        page.waitForLoadState();

        // Un formulaire refuse renvoie sur lui-meme : rester sur ModifyTask signale un champ
        // obligatoire manquant ou une valeur rejetee, cas ou la tache resterait inoperante.
        Assertions.assertFalse(page.url().contains("ModifyTask.jsp"),
            "L'enregistrement de la tache " + taskId + " aurait du quitter son formulaire ; url: "
                + page.url());
    }

    /**
     * Applique un reglage sur l'ecran de configuration ouvert.
     *
     * @param page    page de configuration de la tache
     * @param reglage reglage a appliquer
     */
    private static void appliquer(Page page, TaskConfigDataSet.Reglage reglage) {
        switch (reglage.nature()) {
            case TEXTE -> {
                Locator champ = page.locator(
                    "input[name='" + reglage.champ() + "'], textarea[name='" + reglage.champ() + "']");
                exigerPresence(champ, reglage);
                champ.first().fill(reglage.valeur());
            }
            case SELECTION -> {
                Locator liste = page.locator("select[name='" + reglage.champ() + "']");
                exigerPresence(liste, reglage);
                selectionner(liste.first(), reglage);
            }
            case RADIO -> {
                Locator bouton = page.locator(
                    "input[type='radio'][name='" + reglage.champ() + "'][value='" + reglage.valeur() + "']");
                exigerPresence(bouton, reglage);
                bouton.first().check();
            }
            case CASE -> {
                Locator caseACocher = page.locator(
                    "input[type='checkbox'][name='" + reglage.champ() + "'][value='" + reglage.valeur() + "']");
                exigerPresence(caseACocher, reglage);
                caseACocher.first().check();
            }
            case APPLIQUER -> {
                Locator intermediaire = page.locator("button[name='apply'], input[name='apply']");
                Assertions.assertTrue(intermediaire.count() > 0,
                    "Le formulaire de la tache ne propose pas de bouton de validation intermediaire");
                intermediaire.first().click();
                page.waitForLoadState();
            }
            default -> throw new IllegalStateException("Nature de reglage inconnue : " + reglage.nature());
        }
    }

    /**
     * Selectionne une option par sa valeur, ou a defaut par son libelle.
     *
     * <p>Les listes des taches melangent les deux usages : certaines portent des valeurs parlantes
     * ({@code ParametrableUnitSelection}), d'autres des identifiants techniques attribues par la
     * base, qu'un scenario ne peut pas connaitre a l'avance — seul le libelle y est stable.</p>
     *
     * @param liste   liste deroulante ciblee
     * @param reglage reglage a appliquer
     */
    private static void selectionner(Locator liste, TaskConfigDataSet.Reglage reglage) {
        try {
            liste.selectOption(reglage.valeur());
        } catch (RuntimeException parValeur) {
            liste.selectOption(new com.microsoft.playwright.options.SelectOption()
                .setLabel(reglage.valeur()));
        }
    }

    /**
     * Verifie qu'un controle vise par un reglage existe bien sur l'ecran.
     *
     * @param controle localisateur du controle
     * @param reglage  reglage a l'origine de la recherche
     */
    private static void exigerPresence(Locator controle, TaskConfigDataSet.Reglage reglage) {
        Assertions.assertTrue(controle.count() > 0,
            "Le champ '" + reglage.champ() + "' (" + reglage.nature() + ") est absent de l'ecran de "
                + "configuration de la tache : le parametrage attendu ne correspond pas a ce type de tache");
    }

    @Test
    @DisplayName("Parametrer une tache de mise a jour du statut (auto-provisionnement)")
    void standalone() {
        login();
        WorkflowContext ctx = new WorkflowContext(page, BASE_URL, newSuffix());
        CreateWorkflowMacroTest.run(ctx, WorkflowDataSet.defaults());
        AddStateMacroTest.run(ctx, StateDataSet.initial("Etat initial"));
        AddStateMacroTest.run(ctx, StateDataSet.of("Etat final"));
        AddActionMacroTest.run(ctx, ActionDataSet.defaults());
        AddTaskToActionMacroTest.run(ctx, TaskDataSet.of("modifyUpdateStatusTask"));
        run(ctx, TaskConfigDataSet.vide().radio("published", "true"));
    }
}
