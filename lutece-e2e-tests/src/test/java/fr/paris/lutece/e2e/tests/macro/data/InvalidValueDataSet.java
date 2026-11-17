package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Saisie refusee par un controle de validation en FO.
 *
 * @param questionIndex index de la question dans {@code ctx.questions}, affichee sur l'etape FO courante
 * @param value         valeur saisie, invalide pour le controle
 * @param errorMessage  message d'erreur du controle, attendu sous le champ
 */
public record InvalidValueDataSet(int questionIndex, String value, String errorMessage) {

    public static InvalidValueDataSet of(int questionIndex, String value, String errorMessage) {
        return new InvalidValueDataSet(questionIndex, value, errorMessage);
    }
}
