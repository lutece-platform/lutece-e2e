package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Valeur d'une question texte sauvegardee en brouillon, et resultat attendu a la reouverture du formulaire.
 *
 * @param questionIndex index de la question dans {@code ctx.questions}
 * @param value         valeur saisie puis sauvegardee (la brique y ajoute le suffixe d'execution)
 * @param restored      {@code true} : la valeur et le message de reprise doivent revenir ; {@code false} : champ vide
 */
public record BackupValueDataSet(int questionIndex, String value, boolean restored) {

    public static BackupValueDataSet of(int questionIndex, String value) {
        return new BackupValueDataSet(questionIndex, value, true);
    }

    /** Meme valeur, attendue absente (autre usager, usager deconnecte). */
    public BackupValueDataSet absent() {
        return new BackupValueDataSet(questionIndex, value, false);
    }

    /** Valeur effectivement saisie : {@code value} suivie du suffixe d'execution. */
    public String valueFor(String runSuffix) {
        return value + " " + runSuffix;
    }
}
