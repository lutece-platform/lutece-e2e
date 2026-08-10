package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la verification d'un controle de validation en front office.
 *
 * <p>{@code label} : libelle de la question portant le controle ; {@code invalidValue} : saisie
 * censee declencher l'erreur de validation ; {@code validValue} : saisie censee passer sans erreur ;
 * {@code errorHint} : fragment de texte attendu dans le message d'erreur (s'il est vide, la brique
 * retombe sur des marqueurs generiques d'erreur du DOM : {@code .alert-danger}, {@code .has-error},
 * {@code .is-invalid}).</p>
 *
 * <p>Le defaut est aligne sur {@code AddValidationControlMacroTest} : question "Question a valider",
 * message d'erreur "Saisie invalide".</p>
 */
public record ValidationCheckDataSet(
        String label,
        String invalidValue,
        String validValue,
        String errorHint) {

    public static ValidationCheckDataSet of(String label, String invalidValue,
            String validValue, String errorHint) {
        return new ValidationCheckDataSet(label, invalidValue, validValue, errorHint);
    }

    public static ValidationCheckDataSet defaults() {
        return new ValidationCheckDataSet("Question a valider", "@@@", "Texte valide", "Saisie invalide");
    }
}
