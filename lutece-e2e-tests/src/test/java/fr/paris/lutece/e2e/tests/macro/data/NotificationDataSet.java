package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour controler une notification deposee dans l'historique d'une reponse.
 *
 * @param canal   libelle du canal de notification, tel qu'affiche sur le detail (ex. {@code Agent})
 * @param message fragment attendu dans le corps de la notification
 */
public record NotificationDataSet(String canal, String message) {

    /**
     * Notification attendue sur un canal donne.
     *
     * @param canal   libelle du canal
     * @param message fragment attendu dans le corps du message
     * @return le jeu de donnees correspondant
     */
    public static NotificationDataSet of(String canal, String message) {
        return new NotificationDataSet(canal, message);
    }
}
