package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation d'un formulaire.
 *
 * <p>Le {@code title} est un titre de base : les briques y ajoutent le suffixe unique du contexte
 * pour garantir l'unicite sur un run. Les dates acceptent le format flatpickr ("today", "2033-12-31").</p>
 */
public record FormDataSet(
        String title,
        String description,
        String startDate,
        String endDate,
        String workflowName) {

    public static FormDataSet defaults() {
        return new FormDataSet("Macro Form", "Formulaire macro E2E", "today", "2033-12-31", null);
    }

    public FormDataSet withTitle(String newTitle) {
        return new FormDataSet(newTitle, description, startDate, endDate, workflowName);
    }

    public FormDataSet withWorkflow(String workflow) {
        return new FormDataSet(title, description, startDate, endDate, workflow);
    }
}
