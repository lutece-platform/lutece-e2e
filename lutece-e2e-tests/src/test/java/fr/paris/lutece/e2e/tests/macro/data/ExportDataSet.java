package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour l'export des reponses depuis la multivue.
 *
 * <p>{@code format} = format d'export cible ("CSV", "PDF"). La brique
 * {@code ExportResponsesMacroTest} cherche un controle d'export correspondant ; si aucun controle
 * n'est present, elle saute via Assumptions plutot que d'echouer.</p>
 */
public record ExportDataSet(String format) {

    public static ExportDataSet csv() {
        return new ExportDataSet("CSV");
    }

    public static ExportDataSet pdf() {
        return new ExportDataSet("PDF");
    }

    public static ExportDataSet defaults() {
        return csv();
    }
}
