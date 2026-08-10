package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour remplir un champ en front-office.
 *
 * <p>{@code kind} vaut "text", "number" ou "date" et pilote la strategie de saisie : getByRole
 * TEXTBOX (text) / SPINBUTTON (number) par libelle, puis getByLabel, puis premier champ visible ;
 * approche flatpickr en JavaScript pour une date. Le {@code label} est le libelle de la question tel
 * qu'affiche en front-office (= titre de la question).</p>
 */
public record FieldValueDataSet(String label, String value, String kind) {

    /** Champ texte par defaut. */
    public static FieldValueDataSet of(String label, String value) {
        return new FieldValueDataSet(label, value, "text");
    }

    public static FieldValueDataSet text(String label, String value) {
        return new FieldValueDataSet(label, value, "text");
    }

    public static FieldValueDataSet number(String label, String value) {
        return new FieldValueDataSet(label, value, "number");
    }

    public static FieldValueDataSet date(String label, String value) {
        return new FieldValueDataSet(label, value, "date");
    }
}
