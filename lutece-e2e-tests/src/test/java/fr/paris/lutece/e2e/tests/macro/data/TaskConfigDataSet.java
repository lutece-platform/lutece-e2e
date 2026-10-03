package fr.paris.lutece.e2e.tests.macro.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Parametrage d'une tache de workflow, exprime comme une suite de reglages a appliquer sur son
 * ecran de configuration.
 *
 * <p>Chaque type de tache a son propre formulaire : la publication se choisit par un bouton radio,
 * le commentaire demande un titre et deux options, l'affectation a une entite combine un mode
 * d'assignation et une strategie de selection. Plutot qu'une fabrique par type — qui figerait la
 * brique sur une liste fermee —, le parametrage est decrit champ par champ, ce qui couvre
 * n'importe quelle tache, y compris celles apportees par un module tiers.</p>
 *
 * <p>Exemple :</p>
 * <pre>
 *   TaskConfigDataSet.vide()
 *       .radio("assignment_type", "create")
 *       .selection("unit_selection_id_to_add", "ParametrableUnitSelection")
 *       .appliquer()
 * </pre>
 *
 * @param reglages reglages a appliquer dans l'ordre
 */
public record TaskConfigDataSet(List<Reglage> reglages) {

    /** Nature d'un reglage sur l'ecran de configuration d'une tache. */
    public enum Nature {
        /** Champ texte ou zone de texte. */
        TEXTE,
        /** Liste deroulante, par valeur d'option. */
        SELECTION,
        /** Bouton radio, par valeur. */
        RADIO,
        /** Case a cocher, par valeur. */
        CASE,
        /** Clic sur le bouton intermediaire du formulaire (ex. "Ajouter le mode d'assignation"). */
        APPLIQUER
    }

    /**
     * Un reglage elementaire.
     *
     * @param nature  nature du controle vise
     * @param champ   attribut {@code name} du controle
     * @param valeur  valeur a saisir ou a selectionner
     */
    public record Reglage(Nature nature, String champ, String valeur) {
    }

    /**
     * Parametrage vide, a completer par les methodes fluides.
     *
     * @return un parametrage sans aucun reglage
     */
    public static TaskConfigDataSet vide() {
        return new TaskConfigDataSet(List.of());
    }

    /**
     * Parametrage par defaut : aucun reglage.
     *
     * @return un parametrage sans aucun reglage
     */
    public static TaskConfigDataSet defaults() {
        return vide();
    }

    /**
     * Ajoute la saisie d'un champ texte.
     *
     * @param champ  attribut {@code name} du champ
     * @param valeur texte a saisir
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet texte(String champ, String valeur) {
        return avec(new Reglage(Nature.TEXTE, champ, valeur));
    }

    /**
     * Ajoute la selection d'une option dans une liste deroulante.
     *
     * @param champ  attribut {@code name} de la liste
     * @param valeur valeur de l'option
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet selection(String champ, String valeur) {
        return avec(new Reglage(Nature.SELECTION, champ, valeur));
    }

    /**
     * Ajoute le choix d'un bouton radio.
     *
     * @param champ  attribut {@code name} du groupe de boutons
     * @param valeur valeur du bouton a cocher
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet radio(String champ, String valeur) {
        return avec(new Reglage(Nature.RADIO, champ, valeur));
    }

    /**
     * Ajoute le cochage d'une case.
     *
     * @param champ  attribut {@code name} de la case
     * @param valeur valeur de la case
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet case_(String champ, String valeur) {
        return avec(new Reglage(Nature.CASE, champ, valeur));
    }

    /**
     * Ajoute un clic sur le bouton intermediaire du formulaire.
     *
     * <p>Certains ecrans demandent une validation intermediaire avant l'enregistrement : c'est le
     * cas des taches d'affectation, ou le mode d'assignation doit etre ajoute a la liste avant que
     * la tache puisse etre enregistree.</p>
     *
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet appliquer() {
        return avec(new Reglage(Nature.APPLIQUER, "", ""));
    }

    /**
     * Copie enrichie d'un reglage supplementaire.
     *
     * @param reglage reglage a ajouter
     * @return un nouveau parametrage
     */
    private TaskConfigDataSet avec(Reglage reglage) {
        List<Reglage> copie = new ArrayList<>(reglages);
        copie.add(reglage);
        return new TaskConfigDataSet(List.copyOf(copie));
    }
}
