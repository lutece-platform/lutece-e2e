package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Contenu d'un fichier JSON a refuser a l'import de formulaire.
 *
 * @param label   libelle du cas (nom du fichier et du pas Allure)
 * @param content contenu ecrit tel quel dans le fichier importe
 */
public record InvalidJsonDataSet(String label, String content) {

    /** Texte qui n'est pas du JSON : refus propre attendu (« Erreur lors de l'import du formulaire »). */
    public static InvalidJsonDataSet malformed() {
        return new InvalidJsonDataSet("json-mal-forme", "{ ceci n'est pas du json");
    }

    /** JSON valide sans cle {@code form} : constat du 2026-10-29, NullPointerException (FormJsonService.importForm). */
    public static InvalidJsonDataSet withoutForm() {
        return new InvalidJsonDataSet("json-sans-formulaire", "{}");
    }

    /**
     * JSON avec un formulaire mais sans {@code formMessage} : {@code FormJsonService.importForm} insere le formulaire puis
     * echoue sur le message. Prouve l'atomicite de l'import : aucun formulaire ne doit rester apres le refus.
     */
    public static InvalidJsonDataSet formWithoutMessage() {
        return new InvalidJsonDataSet("json-formulaire-sans-message", "{\"form\":{\"title\":\"E2E import interrompu\"}}");
    }
}
