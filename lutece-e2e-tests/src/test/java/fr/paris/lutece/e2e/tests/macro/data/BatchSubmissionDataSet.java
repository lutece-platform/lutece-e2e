package fr.paris.lutece.e2e.tests.macro.data;

import java.util.List;

/**
 * Lot de soumissions de reponses par l'interface : nombre de reponses et valeurs a saisir, etape par etape.
 *
 * <p>Les valeurs peuvent contenir {@code {i}}, remplace par le numero de la soumission (1..count), pour que
 * chaque reponse soit distincte.</p>
 *
 * @param count       nombre de soumissions
 * @param stepsFields champs a remplir pour chaque etape du parcours, dans l'ordre (une liste par etape)
 */
public record BatchSubmissionDataSet(int count, List<List<FieldValueDataSet>> stepsFields) {

    /**
     * Nombre lu dans la propriete systeme {@code property} ({@code -Dforms.volume.fo=50}), sinon
     * {@code defaultCount}.
     */
    public static BatchSubmissionDataSet of(String property, int defaultCount, List<List<FieldValueDataSet>> stepsFields) {
        return new BatchSubmissionDataSet(Integer.getInteger(property, defaultCount), stepsFields);
    }

    /** Valeur de la soumission {@code i} (remplacement de {@code {i}}). */
    public static FieldValueDataSet forSubmission(FieldValueDataSet template, int i) {
        return new FieldValueDataSet(template.label(), template.value().replace("{i}", String.valueOf(i)), template.kind());
    }
}
