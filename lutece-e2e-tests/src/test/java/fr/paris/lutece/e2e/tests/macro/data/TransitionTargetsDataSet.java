package fr.paris.lutece.e2e.tests.macro.data;

import java.util.List;

/**
 * Ensemble exact des etapes d'arrivee attendues depuis une etape.
 *
 * @param fromStepIndex index de l'etape source dans {@code ctx.steps}
 * @param toStepIndices index des etapes d'arrivee attendues (ordre indifferent ; vide = aucune transition)
 */
public record TransitionTargetsDataSet(int fromStepIndex, List<Integer> toStepIndices) {

    public static TransitionTargetsDataSet of(int fromStepIndex, Integer... toStepIndices) {
        return new TransitionTargetsDataSet(fromStepIndex, List.of(toStepIndices));
    }
}
