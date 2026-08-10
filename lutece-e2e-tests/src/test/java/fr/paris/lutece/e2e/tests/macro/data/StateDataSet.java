package fr.paris.lutece.e2e.tests.macro.data;

/** Jeu de donnees pour la creation / configuration d'un etat de workflow. */
public record StateDataSet(String name, String description, boolean initial) {

    // La description est obligatoire dans le formulaire CreateState : ne jamais la laisser vide.

    public static StateDataSet of(String name) {
        return new StateDataSet(name, "Etat " + name, false);
    }

    public static StateDataSet initial(String name) {
        return new StateDataSet(name, "Etat initial " + name, true);
    }

    public static StateDataSet defaults() {
        return new StateDataSet("Etat initial", "Etat initial du workflow", true);
    }
}
