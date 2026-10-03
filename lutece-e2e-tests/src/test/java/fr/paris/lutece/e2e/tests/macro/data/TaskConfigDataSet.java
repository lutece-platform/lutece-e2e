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
 *       .selection("unit_selection_id_to_add", "UnitSelectionSpecificUnit")
 *       .appliquer("addUnitSelection")
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
        APPLIQUER,
        /** Liste deroulante, par fragment du libelle d'une option. */
        SELECTION_LIBELLE,
        /** Enregistrement intermediaire, le formulaire se poursuivant ensuite. */
        ENREGISTRER,
        /** Ouverture du panneau de configuration avancee de la tache. */
        OUVRIR_AVANCE
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
     * Ajoute un clic sur un bouton intermediaire designe par sa valeur.
     *
     * <p>L'ecran d'une tache d'affectation en propose plusieurs — ajouter un mode d'assignation,
     * en supprimer un, choisir une configuration parametrable — tous portes par le meme attribut
     * {@code name}. Les distinguer par leur valeur evite de dependre de leur ordre d'apparition,
     * qui change avec l'etat de la configuration.</p>
     *
     * @param valeurBouton attribut {@code value} du bouton vise
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet appliquer(String valeurBouton) {
        return avec(new Reglage(Nature.APPLIQUER, "", valeurBouton));
    }

    /**
     * Ajoute la selection de l'option dont le libelle contient le fragment donne.
     *
     * <p>Certaines listes portent des valeurs construites a l'execution — un fournisseur de
     * donnees nomme {@code ...ProviderService.@.*26} melange un identifiant technique attribue par
     * la base — qu'aucun scenario ne peut connaitre a l'avance, et dont le libelle exact varie
     * aussi d'un site a l'autre. Seul un fragment stable du libelle permet de les designer.</p>
     *
     * @param champ    attribut {@code name} de la liste
     * @param fragment fragment devant figurer dans le libelle de l'option
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet selectionLibelle(String champ, String fragment) {
        return avec(new Reglage(Nature.SELECTION_LIBELLE, champ, fragment));
    }

    /**
     * Ajoute un enregistrement intermediaire du formulaire.
     *
     * <p>La tache de notification se configure en deux temps : le fournisseur de donnees doit etre
     * enregistre avant que les canaux de notification ne deviennent proposables. Contrairement au
     * bouton d'application, l'enregistrement peut quitter l'ecran ; la brique y revient d'elle-meme
     * pour poursuivre le parametrage.</p>
     *
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet enregistrer() {
        return avec(new Reglage(Nature.ENREGISTRER, "", ""));
    }

    /**
     * Ouvre le panneau de configuration avancee de la tache.
     *
     * <p>La notification y range ce qui ne concerne pas la redaction du message : le choix des
     * familles de signets utilisables. Ces cases ne sont pas simplement repliees mais hors de
     * portee tant que le panneau n'est pas ouvert.</p>
     *
     * @return un nouveau parametrage incluant ce reglage
     */
    public TaskConfigDataSet avance() {
        return avec(new Reglage(Nature.OUVRIR_AVANCE, "", ""));
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
