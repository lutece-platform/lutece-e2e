package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour l'ajout d'une tache a une action de workflow.
 *
 * <p>Un workflow realiste ne concentre pas tout son traitement sur une seule action : chaque
 * transition porte les taches qui lui sont propres. {@code actionLabel} designe l'action qui
 * recoit la tache ; laisse a null, la tache va a la premiere action du workflow.</p>
 *
 * @param taskTypeKey cle du type de tache (ex. {@code modifyUpdateStatusTask})
 * @param actionLabel libelle de l'action destinataire, ou null pour la premiere
 */
public record TaskDataSet(String taskTypeKey, String actionLabel) {

    /**
     * Tache destinee a la premiere action du workflow.
     *
     * @param taskTypeKey cle du type de tache
     * @return le jeu de donnees correspondant
     */
    public static TaskDataSet of(String taskTypeKey) {
        return new TaskDataSet(taskTypeKey, null);
    }

    /**
     * Tache destinee a une action designee par son libelle.
     *
     * @param taskTypeKey cle du type de tache
     * @param actionLabel libelle de l'action destinataire
     * @return le jeu de donnees correspondant
     */
    public static TaskDataSet sur(String taskTypeKey, String actionLabel) {
        return new TaskDataSet(taskTypeKey, actionLabel);
    }

    /**
     * Jeu de donnees par defaut : mise a jour du statut sur la premiere action.
     *
     * @return le jeu de donnees par defaut
     */
    public static TaskDataSet defaults() {
        return of("modifyUpdateStatusTask");
    }
}
