package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour l'ajout d'une tache a une action (cle du type de tache, ex.
 * "modifyUpdateStatusTask" / "taskComment" / "taskNotification"...).
 */
public record TaskDataSet(String taskTypeKey) {

    public static TaskDataSet of(String taskTypeKey) {
        return new TaskDataSet(taskTypeKey);
    }

    public static TaskDataSet defaults() {
        return new TaskDataSet("modifyUpdateStatusTask");
    }
}
