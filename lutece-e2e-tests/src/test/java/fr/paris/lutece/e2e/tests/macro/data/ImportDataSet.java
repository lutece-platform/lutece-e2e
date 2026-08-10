package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour l'import JSON d'un formulaire.
 *
 * <p>{@code filePath} = chemin absolu du fichier JSON a importer. S'il est nul ou introuvable, la
 * brique {@code ImportFormJsonMacroTest} saute le flux via Assumptions (un fichier est requis pour
 * piloter l'import).</p>
 */
public record ImportDataSet(String filePath) {

    public static ImportDataSet defaults() {
        return new ImportDataSet(null);
    }

    public static ImportDataSet of(String filePath) {
        return new ImportDataSet(filePath);
    }
}
