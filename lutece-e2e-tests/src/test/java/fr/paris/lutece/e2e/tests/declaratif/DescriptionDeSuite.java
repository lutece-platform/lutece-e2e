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
 * @param nom          nom de la suite, porte par le rapport
 * @param description  a quoi sert la suite, pour qui lira le rapport
 * @param organisation les entites organisationnelles a batir, ou {@code null}
 * @param workflow     le workflow a batir, ou {@code null}
 * @param formulaire   le formulaire a batir, ou {@code null}
 * @param parcours     ce qu'il faut ensuite jouer sur la demarche
 */
public record DescriptionDeSuite(
        String nom,
        String description,
        Organisation organisation,
        Workflow workflow,
        Formulaire formulaire,
        List<EtapeDeParcours> parcours) {

    /**
     * Entites organisationnelles sur lesquelles un workflow peut s'appuyer.
     *
     * <p>Une tache d'affectation a besoin d'entites, et une entite d'un agent pour que
     * l'affectation ait un destinataire. Les deux vont donc ensemble.</p>
     *
     * @param entites        libelles des entites a creer, a la racine
     * @param affecterUnAgent rattacher un agent a la derniere entite creee
     */
    public record Organisation(List<String> entites, boolean affecterUnAgent) {
    }

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
     * @param titre             titre du formulaire
     * @param etapes            ses etapes, dans l'ordre du parcours
     * @param enchainement      liaisons entre etapes, ou {@code null} pour un enchainement lineaire
     * @param options           options du formulaire, ou {@code null} pour garder les defauts
     * @param questionsRouvertes questions que rouvre une demande de correction ou de complement
     * @param mappingNotification declarer le mapping qui alimente les notifications de l'usager
     */
    public record Formulaire(
            String titre,
            List<Etape> etapes,
            List<Liaison> enchainement,
            Options options,
            List<String> questionsRouvertes,
            boolean mappingNotification) {
    }

    /**
     * Options du formulaire, telles que la page de modification les expose.
     *
     * <p>Un champ nul signifie « ne pas y toucher » : la brique ne modifie que ce qui est decrit,
     * ce qui evite qu'un fichier silencieux sur une option n'en impose le defaut.</p>
     *
     * @param disponibleDu        debut de disponibilite, au format flatpickr
     * @param disponibleAu        fin de disponibilite
     * @param messageIndisponible message affiche hors periode
     * @param reponsesMax         nombre maximum de reponses, 0 pour illimite
     * @param uneReponseParUsager limiter a une reponse par usager
     * @param recapitulatif       afficher le recapitulatif avant validation
     * @param brouillon           autoriser la sauvegarde en brouillon
     * @param filAriane           afficher le fil d'ariane
     * @param authentification    exiger que l'usager soit connecte
     */
    public record Options(
            String disponibleDu,
            String disponibleAu,
            String messageIndisponible,
            Integer reponsesMax,
            boolean uneReponseParUsager,
            boolean recapitulatif,
            boolean brouillon,
            boolean filAriane,
            boolean authentification) {
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
            Condition afficheeSi,
            Validation validation) {
    }

    /**
     * Une regle de validation posee sur une question.
     *
     * @param regle   libelle de l'expression reguliere a appliquer, tel que le site la nomme
     * @param message message affiche a l'usager quand la saisie est refusee
     */
    public record Validation(String regle, String message) {
    }

    /**
     * @param question libelle de la question qui commande l'affichage
     * @param vaut     valeur qui declenche l'affichage
     */
    public record Condition(String question, String vaut) {
    }

    /**
     * Une liaison entre deux etapes, eventuellement conditionnee.
     *
     * <p>Lutece examine les liaisons d'une etape dans l'ordre de leur priorite et emprunte la
     * premiere dont la condition est satisfaite. L'ordre de declaration fait donc partie de la
     * description : une liaison sans condition placee avant une autre les rend toutes inutiles.</p>
     *
     * @param de   titre de l'etape de depart
     * @param vers titre de l'etape d'arrivee
     * @param si   condition d'emprunt, ou {@code null} pour une sortie par defaut
     */
    public record Liaison(String de, String vers, Condition si) {
    }

    /** Une etape de parcours, jouee dans l'ordre du fichier. */
    public sealed interface EtapeDeParcours permits Soumission, Instruction, Daemon, Export {
    }

    /**
     * Execution d'un daemon, pour que la suite voie ce qu'il produit.
     *
     * @param cle identifiant du daemon, tel que la page d'administration le nomme
     */
    public record Daemon(String cle) implements EtapeDeParcours {
    }

    /**
     * Export des reponses recues.
     *
     * @param format format demande, « csv » ou « pdf »
     */
    public record Export(String format) implements EtapeDeParcours {
    }

    /**
     * Une soumission en front office.
     *
     * @param nom       nom de la soumission, porte par le rapport
     * @param reponses  valeurs a retenir pour les questions a choix, par libelle de question
     * @param controles visibilites a constater pendant le parcours
     */
    public record Soumission(
            String nom,
            Map<String, String> reponses,
            List<Saisie> valeurs,
            List<ControleDeVisibilite> controles,
            List<String> etapesAttendues,
            List<String> etapesEcartees,
            List<String> iterations,
            List<SaisieRefusee> refus,
            boolean brouillon) implements EtapeDeParcours {
    }

    /**
     * Une valeur posee dans un champ, plutot que laissee au remplissage automatique.
     *
     * @param question libelle de la question
     * @param valeur   valeur a saisir
     * @param nature   « texte », « nombre » ou « date », qui pilote la strategie de saisie
     */
    public record Saisie(String question, String valeur, String nature) {
    }

    /**
     * Une saisie que le formulaire doit refuser, et le message attendu.
     *
     * <p>Une valeur acceptee accompagne la valeur refusee : sans elle, un champ qui refuserait
     * tout passerait pour correctement valide.</p>
     *
     * @param question libelle de la question
     * @param valeur   valeur invalide, que le formulaire doit refuser
     * @param accepte  valeur valide, que le formulaire doit accepter
     * @param message  fragment attendu du message d'erreur
     */
    public record SaisieRefusee(String question, String valeur, String accepte, String message) {
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
    public record Instruction(
            String action,
            String etatAttendu,
            List<String> traces,
            String surEtat,
            Notification notification,
            boolean lienFrontOffice) implements EtapeDeParcours {
    }

    /**
     * Contenu attendu de la notification adressee a l'usager.
     *
     * @param canal   canal observe, par exemple « Agent »
     * @param message fragment attendu du message
     */
    public record Notification(String canal, String message) {
    }
}
