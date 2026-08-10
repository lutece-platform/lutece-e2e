package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la creation d'une action de workflow : nom + etats source/cible
 * (references par leur index dans ctx.states).
 */
public record ActionDataSet(String name, String description, int fromStateIndex, int toStateIndex) {

    // La description est obligatoire dans le formulaire CreateAction : ne jamais la laisser vide.

    public static ActionDataSet of(String name, int fromStateIndex, int toStateIndex) {
        return new ActionDataSet(name, "Action " + name, fromStateIndex, toStateIndex);
    }

    public static ActionDataSet defaults() {
        return new ActionDataSet("Valider", "Action de validation", 0, 1);
    }
}
