package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees de verification de l'enregistrement de reponses dans la multivue.
 *
 * <p>{@code expectedNewResponses} : nombre exact de reponses nouvelles attendues par rapport aux ids deja
 * connus de {@code ctx.responseIds}. {@code timeoutSeconds} : delai max d'apparition (la multivue lit un
 * index Lucene alimente par un demon). {@code triggerIndexing} : lance d'abord l'indexation incrementale
 * depuis l'ecran « Indexation des contenus Forms » (chemin reel de l'interface).</p>
 */
public record ResponseCheckDataSet(int expectedNewResponses, int timeoutSeconds, boolean triggerIndexing) {

    public ResponseCheckDataSet {
        if (expectedNewResponses < 0 || timeoutSeconds < 0) {
            throw new IllegalArgumentException("expectedNewResponses et timeoutSeconds doivent etre >= 0");
        }
    }

    /** Une reponse nouvelle, indexation declenchee, 120 s max. */
    public static ResponseCheckDataSet defaults() {
        return new ResponseCheckDataSet(1, 120, true);
    }

    public static ResponseCheckDataSet of(int expectedNewResponses) {
        return new ResponseCheckDataSet(expectedNewResponses, 120, true);
    }

    public ResponseCheckDataSet withTimeoutSeconds(int seconds) {
        return new ResponseCheckDataSet(expectedNewResponses, seconds, triggerIndexing);
    }
}
