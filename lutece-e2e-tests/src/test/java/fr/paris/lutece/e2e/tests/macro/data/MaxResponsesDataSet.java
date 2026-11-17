package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Nombre maximum de reponses d'un formulaire (parametre {@code maxNumberResponse}, 0 = pas de limite).
 *
 * @param max nombre total de reponses acceptees par le formulaire, 0 pour ne pas limiter
 */
public record MaxResponsesDataSet(int max) {

    public static MaxResponsesDataSet of(int max) {
        return new MaxResponsesDataSet(max);
    }

    public static MaxResponsesDataSet unlimited() {
        return new MaxResponsesDataSet(0);
    }
}
