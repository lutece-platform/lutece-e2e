package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexte partage explicitement entre briques macro du plugin Workflow.
 *
 * <p>Porte le {@link Page}, le {@code baseUrl}, un suffixe unique par run, et les elements produits :
 * id du workflow, etats et actions (references par leur nom cote UF).</p>
 */
public class WorkflowContext {

    public final Page page;
    public final String baseUrl;
    public final String runSuffix;

    public int workflowId = -1;
    public String workflowName;

    public final List<StateRef> states = new ArrayList<>();
    public final List<ActionRef> actions = new ArrayList<>();

    /**
     * Identifiants des taches inserees, dans l'ordre d'insertion.
     *
     * <p>Une tache n'est pas configurable depuis la page de l'action : son parametrage vit sur
     * {@code ModifyTask.jsp?id_task=...}. Memoriser l'identifiant est donc le seul moyen de
     * configurer une tache qu'on vient d'ajouter.</p>
     */
    public final List<Integer> taskIds = new ArrayList<>();

    public WorkflowContext(Page page, String baseUrl, String runSuffix) {
        this.page = page;
        this.baseUrl = baseUrl;
        this.runSuffix = runSuffix;
    }

    /** Etat d'un workflow (reference par son nom dans les formulaires d'action). */
    public static class StateRef {
        public String name;
        public boolean initial;

        public StateRef() {}

        public StateRef(String name, boolean initial) {
            this.name = name;
            this.initial = initial;
        }
    }

    /** Action d'un workflow. */
    public static class ActionRef {
        public String name;

        public ActionRef() {}

        public ActionRef(String name) {
            this.name = name;
        }
    }

    public StateRef state(int index) {
        return states.get(index);
    }
}
