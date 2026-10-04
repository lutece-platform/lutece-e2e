package fr.paris.lutece.e2e.tests.declaratif;

import fr.paris.lutece.e2e.tests.macro.data.QuestionType;

import java.util.List;
import java.util.Map;

/**
 * Ce qu'un fichier de suite decrit, une fois lu et valide.
 *
 * <p>Modele volontairement plat et sans comportement : il ne sait ni se lire ni s'executer, ces
 * deux roles revenant a {@link LecteurDeSuite} et {@link ExecuteurDeSuite}. La separation permet de
 * valider un fichier sans navigateur, ce dont se sert {@code ValiderSuiteDeclarativeTest}.</p>
 *
 * <p>Les trois sections sont independantes : un fichier peut ne decrire qu'un formulaire, ou n'en
 * decrire aucun et ne jouer qu'un parcours sur une demarche deja en place.</p>
 *
 * @param nom         nom de la suite, porte par le rapport
 * @param description a quoi sert la suite, pour qui lira le rapport
 * @param workflow    le workflow a batir, ou {@code null}
 * @param formulaire  le formulaire a batir, ou {@code null}
 * @param parcours    ce qu'il faut ensuite jouer sur la demarche
 */
public record DescriptionDeSuite(
        String nom,
        String description,
        Workflow workflow,
        Formulaire formulaire,
        List<EtapeDeParcours> parcours) {

    /**
     * @param nom     nom du workflow
     * @param etats   ses etats, le premier etant l'etat initial
     * @param actions ses actions
     */
    public record Workflow(String nom, List<String> etats, List<Action> actions) {
    }

    /**
     * @param nom    libelle de l'action
     * @param de     etat de depart
     * @param vers   etat d'arrivee
     * @param taches taches executees par l'action, dans l'ordre
     */
    public record Action(String nom, String de, String vers, List<Tache> taches) {
    }

    /**
     * Une tache et son parametrage.
     *
     * <p>{@code reglages} porte les champs du formulaire de configuration tels que le plugin les
     * nomme. Les traduire aurait suppose de connaitre le formulaire de chaque type de tache, qui
     * varie d'un module a l'autre et d'une version a l'autre ; les laisser bruts rend le fichier un
     * peu plus technique mais permet de configurer une tache que le depot ne connaissait pas.</p>
     *
     * @param type     type de tache, dans le vocabulaire du fichier
     * @param cle      cle attendue par le plugin, resolue a la lecture
     * @param reglages champs de configuration, par leur nom cote plugin
     */
    public record Tache(String type, String cle, Map<String, String> reglages) {
    }

    /**
     * @param titre        titre du formulaire
     * @param etapes       ses etapes, dans l'ordre du parcours
     * @param enchainement liaisons entre etapes, ou {@code null} pour un enchainement lineaire
     */
    public record Formulaire(String titre, List<Etape> etapes, List<Liaison> enchainement) {
    }

    /**
     * @param titre     titre de l'etape
     * @param initiale  l'etape par laquelle l'usager commence
     * @param finale    l'etape qui conclut le parcours
     * @param horsParcours l'etape n'est reliee a aucune autre : reservee au back-office
     * @param groupes   regroupements de l'etape
     * @param questions questions de l'etape
     */
    public record Etape(
            String titre,
            boolean initiale,
            boolean finale,
            boolean horsParcours,
            List<Groupe> groupes,
            List<Question> questions) {
    }

    /**
     * @param titre      titre du groupe
     * @param iterations nombre maximum de repetitions, 1 pour un groupe simple
     */
    public record Groupe(String titre, int iterations) {
    }

    /**
     * @param type        type de question
     * @param titre       libelle de la question
     * @param choix       valeurs proposees, pour les types a choix
     * @param groupe      titre du groupe qui l'accueille, ou {@code null} pour la racine de l'etape
     * @param afficheeSi  condition d'affichage, ou {@code null}
     */
    public record Question(
            QuestionType type,
            String titre,
            List<String> choix,
            String groupe,
            Condition afficheeSi) {
    }

    /**
     * @param question libelle de la question qui commande l'affichage
     * @param vaut     valeur qui declenche l'affichage
     */
    public record Condition(String question, String vaut) {
    }

    /**
     * @param de   titre de l'etape de depart
     * @param vers titre de l'etape d'arrivee
     */
    public record Liaison(String de, String vers) {
    }

    /** Une etape de parcours : soit une soumission en front office, soit une instruction. */
    public sealed interface EtapeDeParcours permits Soumission, Instruction {
    }

    /**
     * Une soumission en front office.
     *
     * @param nom       nom de la soumission, porte par le rapport
     * @param reponses  valeurs a retenir pour les questions a choix, par libelle de question
     * @param controles visibilites a constater pendant le parcours
     */
    public record Soumission(String nom, Map<String, String> reponses, List<ControleDeVisibilite> controles)
        implements EtapeDeParcours {
    }

    /**
     * @param question libelle de la question observee
     * @param visible  visibilite attendue
     */
    public record ControleDeVisibilite(String question, boolean visible) {
    }

    /**
     * Une action de workflow jouee sur la reponse recue, et ce qu'elle doit produire.
     *
     * @param action      libelle de l'action
     * @param etatAttendu etat que doit prendre la reponse, ou {@code null} pour ne pas le controler
     * @param traces      fragments attendus dans l'historique de la reponse
     * @param surEtat     n'agir que sur une reponse dans cet etat, ou {@code null} pour la derniere ouverte
     */
    public record Instruction(String action, String etatAttendu, List<String> traces, String surEtat)
        implements EtapeDeParcours {
    }
}
