package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexte partage explicitement entre briques macro.
 *
 * <p>Il porte le {@link Page} Playwright, le {@code baseUrl}, un suffixe unique par run (pour eviter
 * les collisions de noms) et les identifiants produits par les briques (formId, etapes, questions,
 * groupes, transitions, controles, id de reponse FO).</p>
 *
 * <p>Aucun etat cache (pas de fichiers {@code target/*.txt}) : l'etat circule d'une brique a l'autre
 * via cet objet. En suite, le contexte est threade ; en solo, la brique s'auto-provisionne en
 * appelant les briques amont sur un contexte frais.</p>
 */
public class FormsContext {

    public final Page page;
    public final String baseUrl;
    public final String runSuffix;

    /** Identifiant du formulaire courant (-1 si aucun). */
    public int formId = -1;
    public String formTitle;

    /** Workflow associe (optionnel). */
    public Integer workflowId;
    public String workflowName;

    public final List<StepRef> steps = new ArrayList<>();
    public final List<QuestionRef> questions = new ArrayList<>();
    public final List<GroupRef> groups = new ArrayList<>();
    public final List<Integer> transitionIds = new ArrayList<>();
    public final List<Integer> controlIds = new ArrayList<>();

    /** Id de la derniere reponse soumise en front office (null si aucune). */
    public Integer lastResponseId;

    public FormsContext(Page page, String baseUrl, String runSuffix) {
        this.page = page;
        this.baseUrl = baseUrl;
        this.runSuffix = runSuffix;
    }

    // === References mutables (mises a jour par les briques) ===

    /** Reference d'une etape creee. */
    public static class StepRef {
        public int id;
        public String title;
        public boolean initial;
        public boolean isFinal;

        public StepRef() {}

        public StepRef(int id, String title, boolean initial, boolean isFinal) {
            this.id = id;
            this.title = title;
            this.initial = initial;
            this.isFinal = isFinal;
        }
    }

    /** Reference d'une question creee. */
    public static class QuestionRef {
        public int id;
        public String title;
        public String type;
        public int stepId;

        public QuestionRef() {}

        public QuestionRef(int id, String title, String type, int stepId) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.stepId = stepId;
        }
    }

    /** Reference d'un groupe (regroupement) cree. */
    public static class GroupRef {
        public int id;
        public String title;
        public int stepId;

        public GroupRef() {}

        public GroupRef(int id, String title, int stepId) {
            this.id = id;
            this.title = title;
            this.stepId = stepId;
        }
    }

    // === Accesseurs pratiques ===

    public StepRef step(int index) {
        return steps.get(index);
    }

    public StepRef lastStep() {
        return steps.get(steps.size() - 1);
    }

    public QuestionRef lastQuestion() {
        return questions.get(questions.size() - 1);
    }

    public GroupRef lastGroup() {
        return groups.get(groups.size() - 1);
    }
}
