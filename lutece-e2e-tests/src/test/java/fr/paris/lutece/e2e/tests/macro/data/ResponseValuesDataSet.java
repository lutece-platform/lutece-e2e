package fr.paris.lutece.e2e.tests.macro.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valeurs attendues dans le detail BO des dernieres reponses enregistrees.
 *
 * <p>{@code expected} : une table « titre de question -> valeur » par reponse. Les tables sont comparees aux N
 * dernieres reponses de {@code ctx.responseIds} (N = taille de la liste), sans ordre : chaque table doit
 * correspondre a exactement une reponse.</p>
 *
 * <p>{@code unanswered} : titres de questions qui ne doivent porter aucune valeur dans ces reponses (ligne absente du
 * detail ou valeur vide), par exemple une question masquee par une condition d'affichage (2026-11-12).</p>
 */
public record ResponseValuesDataSet(List<Map<String, String>> expected, Set<String> unanswered) {

    public ResponseValuesDataSet {
        if (expected == null || expected.isEmpty()) {
            throw new IllegalArgumentException("Au moins une reponse attendue");
        }
        expected = List.copyOf(expected);
        unanswered = unanswered == null ? Set.of() : Set.copyOf(unanswered);
    }

    public ResponseValuesDataSet(List<Map<String, String>> expected) {
        this(expected, Set.of());
    }

    /** Memes valeurs attendues, et les questions {@code titles} sans valeur dans chaque reponse. */
    public ResponseValuesDataSet without(String... titles) {
        Set<String> all = new LinkedHashSet<>(unanswered);
        all.addAll(List.of(titles));
        return new ResponseValuesDataSet(expected, all);
    }

    /** Une reponse : la derniere enregistree. Paires titre, valeur. */
    public static ResponseValuesDataSet of(String... titleValuePairs) {
        if (titleValuePairs.length == 0 || titleValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Paires titre, valeur attendues");
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < titleValuePairs.length; i += 2) {
            values.put(titleValuePairs[i], titleValuePairs[i + 1]);
        }
        return new ResponseValuesDataSet(List.of(values));
    }

    /** Les {@code count} reponses d'un lot, valeurs {@code {i}} resolues de 1 a {@code count}. */
    public static ResponseValuesDataSet batch(BatchSubmissionDataSet batch) {
        List<Map<String, String>> all = new ArrayList<>();
        for (int i = 1; i <= batch.count(); i++) {
            Map<String, String> values = new LinkedHashMap<>();
            for (List<FieldValueDataSet> step : batch.stepsFields()) {
                for (FieldValueDataSet field : step) {
                    values.put(field.label(), BatchSubmissionDataSet.forSubmission(field, i).value());
                }
            }
            all.add(values);
        }
        return new ResponseValuesDataSet(all);
    }

    public int count() {
        return expected.size();
    }
}
