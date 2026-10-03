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
import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
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

    /**
     * Depose une valeur dans l'editeur riche attache a un champ, et la reporte dans le champ.
     *
     * <p>Ecrire dans le cadre d'edition ne suffit pas : l'editeur ne recopie son contenu dans le
     * champ d'origine — le seul que le serveur lise — qu'au moment ou il y est invite. Sans ce
     * report, le message part vide, et c'est seulement a l'execution de l'action que son absence
     * se remarque. On s'adresse donc a l'editeur qui pilote precisement ce champ, et non a
     * l'editeur actif, la page pouvant en compter plusieurs.</p>
     */
    private static final String EDITEUR_RICHE =
        "(el, valeur) => {"
        + " const tm = window.tinymce;"
        + " const ed = (tm && tm.get && el.id) ? tm.get(el.id) : null;"
        + " if (ed) { ed.setContent(valeur); ed.save(); return true; }"
        + " el.value = valeur;"
        + " el.dispatchEvent(new Event('change', { bubbles: true }));"
        + " return false;"
        + "}";

    /**
     * Attente de l'editeur riche attache a un champ.
     *
     * <p>On interroge son etat d'initialisation, et non sa seule existence : l'editeur est
     * enregistre avant d'avoir repris la main sur le champ, et ecrire dans cet intervalle depose
     * bien la valeur, que l'editeur remplace ensuite par la sienne — vide.</p>
     */
    private static final String EDITEUR_PRET =
        "id => { const tm = window.tinymce;"
        + " const ed = (tm && tm.get) ? tm.get(id) : null;"
        + " return !!(ed && ed.initialized); }";

    /** Delai d'initialisation de l'editeur riche, en millisecondes. */
    private static final double EDITEUR_TIMEOUT_MS = 5000;

    /** Panneau de configuration avancee d'une tache de notification. */
    private static final String PANNEAU_AVANCE = "#config_global";

    /** Delai d'ouverture du panneau de configuration avancee, en millisecondes. */
    private static final double PANNEAU_TIMEOUT_MS = 5000;

    /**
     * Parametre la tache du type indique, quelle que soit sa place dans l'ordre d'insertion.
     *
     * <p>C'est la forme a preferer des qu'une action porte plusieurs taches : designer la tache
     * par son type dit ce qu'on parametre, la ou s'en remettre a la derniere inseree impose
     * d'enchainer ajout et configuration sans rien intercaler — et parametre silencieusement la
     * mauvaise tache si cet ordre n'est pas tenu.</p>
     *
     * @param ctx         contexte workflow courant
     * @param taskTypeKey cle du type de tache a parametrer
     * @param data        reglages a appliquer
     */
    @Step("Parametrer la tache designee par son type")
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) WorkflowContext ctx, String taskTypeKey, TaskConfigDataSet data) {
        WorkflowContext.TaskRef tache = ctx.tasks.stream()
            .filter(t -> taskTypeKey.equals(t.typeKey))
            .reduce((premiere, derniere) -> derniere)
            .orElse(null);
        Assertions.assertNotNull(tache,
            "Aucune tache de type '" + taskTypeKey + "' n'a ete ajoutee au workflow : elle ne peut "
                + "pas etre parametree");
        parametrer(ctx, tache.id, data);
    }

    @Step("Parametrer la tache")
    public static void run(@Param(excluded = true, mode = Parameter.Mode.HIDDEN) WorkflowContext ctx, TaskConfigDataSet data) {
        Assertions.assertFalse(ctx.tasks.isEmpty(),
            "Une tache doit avoir ete ajoutee (ctx.tasks) avant d'etre parametree");

        parametrer(ctx, ctx.tasks.get(ctx.tasks.size() - 1).id, data);
    }

    /**
     * Applique les reglages a une tache designee par son identifiant, puis relit sa configuration.
     *
     * @param ctx    contexte workflow courant
     * @param taskId identifiant de la tache a parametrer
     * @param data   reglages a appliquer
     */
    private static void parametrer(WorkflowContext ctx, int taskId, TaskConfigDataSet data) {
        Page page = ctx.page;
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyTask.jsp?id_task=" + taskId);

        for (TaskConfigDataSet.Reglage reglage : data.reglages()) {
            appliquer(ctx, taskId, reglage);
        }

        Locator enregistrer = page.locator("button[name='save'], input[name='save']");
        if (enregistrer.count() == 0) {
            enregistrer = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Enregistrer"));
        }
        enregistrer.first().click();
        page.waitForLoadState();

        // Rester sur l'ecran n'est pas en soi un echec : les taches a configuration avancee, comme
        // la notification, y reviennent pour proposer la suite du parametrage. Seul un message
        // d'erreur temoigne d'un refus — le reste est etabli par la relecture ci-dessous.
        String erreur = erreurAffichee(page);
        Assertions.assertNull(erreur,
            "L'enregistrement de la tache " + taskId + " est refuse : " + erreur);

        verifierPersistance(ctx, taskId, data);
    }

    /**
     * Message d'erreur affiche par le formulaire de la tache, le cas echeant.
     *
     * @param page page de configuration de la tache
     * @return le message d'erreur, ou null s'il n'y en a pas
     */
    private static String erreurAffichee(Page page) {
        Locator erreurs = page.locator(".alert-danger, .alert-error");
        for (int i = 0; i < erreurs.count(); i++) {
            String texte = erreurs.nth(i).innerText().replaceAll("\\s+", " ").trim();
            if (!texte.isEmpty()) {
                return texte;
            }
        }
        return null;
    }

    /**
     * Relit l'ecran de la tache et controle que les reglages y ont bien ete enregistres.
     *
     * <p>Quitter le formulaire ne prouve rien : Lutece redirige vers l'action meme lorsque la
     * configuration n'a pas ete creee. Une tache laissee sans configuration n'echoue pas a
     * l'enregistrement, elle echoue bien plus tard, a l'execution de l'action, par une
     * {@code NullPointerException} cote serveur qui annule toute la transaction — la ressource ne
     * change pas d'etat et l'historique reste vide, sans qu'aucun message n'apparaisse a l'ecran.
     * Ce controle ramene ce defaut la ou il nait.</p>
     *
     * <p>Les listes deroulantes sont exclues : plusieurs d'entre elles servent a <em>ajouter</em> un
     * element a la configuration ({@code unit_selection_id_to_add}) et reviennent a leur valeur
     * initiale une fois l'ajout effectue — leur relecture ne dit rien de ce qui a ete enregistre.</p>
     *
     * @param ctx    contexte workflow courant
     * @param taskId identifiant de la tache parametree
     * @param data   reglages demandes
     */
    private static void verifierPersistance(WorkflowContext ctx, int taskId, TaskConfigDataSet data) {
        Page page = ctx.page;
        WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyTask.jsp?id_task=" + taskId);

        for (TaskConfigDataSet.Reglage reglage : data.reglages()) {
            switch (reglage.nature()) {
                case TEXTE -> {
                    Locator champ = page.locator(
                        "input[name='" + reglage.champ() + "'], textarea[name='" + reglage.champ() + "']");
                    String lu = champ.first().inputValue();
                    // Un editeur riche enregistre le texte enveloppe de balisage : exiger l'egalite
                    // stricte ferait echouer une configuration pourtant correcte.
                    boolean conforme = champ.first().isVisible()
                        ? reglage.valeur().equals(lu)
                        : lu.contains(reglage.valeur());
                    Assertions.assertTrue(conforme,
                        "Le champ '" + reglage.champ() + "' de la tache " + taskId
                            + " n'a pas ete enregistre : la tache resterait inoperante a "
                            + "l'execution. Valeur lue : '" + lu + "'");
                }
                case RADIO, CASE -> {
                    boolean coche = page.locator("input[name='" + reglage.champ() + "'][value='"
                        + reglage.valeur() + "']").first().isChecked();
                    Assertions.assertTrue(coche,
                        "Le choix '" + reglage.champ() + "=" + reglage.valeur() + "' de la tache "
                            + taskId + " n'a pas ete enregistre : la tache resterait inoperante a "
                            + "l'execution de l'action");
                }
                default -> {
                    // SELECTION et APPLIQUER : rien de relisible de facon fiable, cf. javadoc.
                }
            }
        }
    }

    /**
     * Applique un reglage sur l'ecran de configuration ouvert.
     *
     * @param ctx     contexte workflow courant
     * @param taskId  identifiant de la tache parametree
     * @param reglage reglage a appliquer
     */
    private static void appliquer(WorkflowContext ctx, int taskId, TaskConfigDataSet.Reglage reglage) {
        Page page = ctx.page;
        switch (reglage.nature()) {
            case TEXTE -> {
                Locator champ = page.locator(
                    "input[name='" + reglage.champ() + "'], textarea[name='" + reglage.champ() + "']");
                exigerPresence(champ, reglage);
                saisir(page, champ.first(), reglage);
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
                String selecteur = reglage.valeur().isBlank()
                    ? "button[name='apply'], input[name='apply']"
                    : "button[name='apply'][value='" + reglage.valeur() + "'], "
                        + "input[name='apply'][value='" + reglage.valeur() + "']";
                Locator intermediaire = page.locator(selecteur);
                Assertions.assertTrue(intermediaire.count() > 0,
                    "Le formulaire de la tache ne propose pas de bouton de validation intermediaire"
                        + (reglage.valeur().isBlank() ? "" : " '" + reglage.valeur() + "'"));
                intermediaire.first().click();
                page.waitForLoadState();
            }
            case SELECTION_LIBELLE -> {
                Locator liste = page.locator("select[name='" + reglage.champ() + "']");
                exigerPresence(liste, reglage);
                selectionnerParFragment(liste.first(), reglage);
            }
            case OUVRIR_AVANCE -> {
                Locator declencheur = page.locator("[data-bs-target='" + PANNEAU_AVANCE + "']");
                Assertions.assertTrue(declencheur.count() > 0,
                    "La tache ne propose pas de configuration avancee");
                declencheur.first().click();
                page.locator(PANNEAU_AVANCE).waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                    .setTimeout(PANNEAU_TIMEOUT_MS));
            }
            case ENREGISTRER -> {
                page.locator("button[name='save'], input[name='save']").first().click();
                page.waitForLoadState();
                WorkflowSupport.navigate(ctx, WorkflowSupport.WF + "ModifyTask.jsp?id_task=" + taskId);
            }
            default -> throw new IllegalStateException("Nature de reglage inconnue : " + reglage.nature());
        }
    }

    /**
     * Saisit une valeur dans un champ, qu'il soit ordinaire ou pourvu d'un editeur riche.
     *
     * <p>Les messages de notification sont rediges dans un editeur riche : le champ d'origine est
     * alors masque et remplace par un cadre editable. Y ecrire directement echoue, le champ n'etant
     * plus atteignable ; c'est a l'editeur qu'il faut s'adresser, une fois son initialisation
     * achevee, et c'est lui qui reporte son contenu dans le champ.</p>
     *
     * @param page    page de configuration de la tache
     * @param champ   champ vise
     * @param reglage reglage a appliquer
     */
    private static void saisir(Page page, Locator champ, TaskConfigDataSet.Reglage reglage) {
        if (champ.isVisible()) {
            champ.fill(reglage.valeur());
            return;
        }
        attendreEditeur(page, champ);
        champ.evaluate(EDITEUR_RICHE, reglage.valeur());
    }

    /**
     * Laisse a l'editeur riche le temps de prendre en charge le champ.
     *
     * <p>L'editeur s'initialise apres le chargement de la page. Ecrire avant qu'il ne soit pret
     * depose bien la valeur dans le champ, mais l'editeur la remplace ensuite par son propre
     * contenu — vide : la configuration semble faite et ne l'est pas.</p>
     *
     * @param page  page de configuration de la tache
     * @param champ champ masque, candidat a un editeur riche
     */
    private static void attendreEditeur(Page page, Locator champ) {
        String id = champ.getAttribute("id");
        if (id == null || id.isBlank()) {
            return;
        }
        try {
            page.waitForFunction(EDITEUR_PRET, id,
                new Page.WaitForFunctionOptions().setTimeout(EDITEUR_TIMEOUT_MS));
        } catch (RuntimeException sansEditeur) {
            // Champ masque pour une autre raison : la saisie directe ci-apres fera l'affaire.
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
     * Selectionne l'option dont le libelle contient le fragment demande.
     *
     * @param liste   liste deroulante ciblee
     * @param reglage reglage a appliquer
     */
    private static void selectionnerParFragment(Locator liste, TaskConfigDataSet.Reglage reglage) {
        Locator options = liste.locator("option");
        for (int i = 0; i < options.count(); i++) {
            String libelle = options.nth(i).innerText();
            if (libelle != null && libelle.contains(reglage.valeur())) {
                liste.selectOption(options.nth(i).getAttribute("value"));
                return;
            }
        }
        Assertions.fail("Aucune option de la liste '" + reglage.champ() + "' ne porte le libelle '"
            + reglage.valeur() + "' : le parametrage attendu ne correspond pas a cet ecran");
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
