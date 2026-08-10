package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la publication d'un formulaire sur le portail (dates de disponibilite).
 *
 * <p>Les dates acceptent le format flatpickr ("today", "2033-12-31").</p>
 */
public record PublishDataSet(String startDate, String endDate) {

    public static PublishDataSet defaults() {
        return new PublishDataSet("today", "2033-12-31");
    }

    public static PublishDataSet of(String startDate, String endDate) {
        return new PublishDataSet(startDate, endDate);
    }
}
