package fr.paris.lutece.e2e.tests.declaratif;

import fr.paris.lutece.e2e.tests.declaratif.DescriptionDeSuite.*;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lit un fichier de suite et le valide avant toute execution.
 *
 * <p>Le format est le YAML, et donc aussi le JSON, qui en est un sous-ensemble : un site peut
 * decrire sa suite dans l'un ou l'autre sans que rien ne change ici. Le YAML est preferable pour
 * un fichier ecrit a la main, parce qu'il admet des commentaires — une suite de tests se justifie
 * autant qu'elle se decrit.</p>
 *
 * <p>Tout est verifie a la lecture : un type de question inconnu, une condition qui vise une
 * question absente, une action dont l'etat de depart n'existe pas. Decouvrir ces fautes au
 * vingtieme clic d'un navigateur couterait dix minutes a chaque correction, et le message qui en
 * sortirait parlerait d'un selecteur introuvable plutot que de la ligne a corriger.</p>
 *
 * <p>Le chargement passe par {@link SafeConstructor} : un fichier de suite vient de l'image d'un
 * site, et le YAML sait instancier des classes arbitraires si on le laisse faire.</p>
 */
public final class LecteurDeSuite {

    /** Taille maximale du fichier, en octets : une suite se decrit, elle ne s'engendre pas. */
    private static final int TAILLE_MAX = 1_048_576;

    private LecteurDeSuite() {
    }

    /**
     * Lit la suite decrite par un fichier.
     *
     * @param fichier chemin du fichier YAML ou JSON
     * @return la description validee
     */
    public static DescriptionDeSuite depuis(Path fichier) {
        if (!Files.isReadable(fichier)) {
            throw new DescriptionInvalide("fichier", "introuvable ou illisible : " + fichier);
        }
        try (InputStream flux = Files.newInputStream(fichier)) {
            return lire(flux, fichier.toString());
        } catch (IOException echecLecture) {
            throw new DescriptionInvalide("fichier", "lecture impossible : " + echecLecture.getMessage());
        }
    }

    /**
     * Lit la suite decrite par un flux.
     *
     * @param flux    contenu YAML ou JSON
     * @param origine nom a citer dans les messages d'erreur
     * @return la description validee
     */
    public static DescriptionDeSuite lire(InputStream flux, String origine) {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(TAILLE_MAX);
        Object racine = new Yaml(new SafeConstructor(options)).load(flux);
        if (!(racine instanceof Map)) {
            throw new DescriptionInvalide(origine, "le fichier devrait decrire un objet, "
                + (racine == null ? "il est vide" : "il contient " + racine.getClass().getSimpleName()));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> contenu = (Map<String, Object>) racine;

        Map<String, Object> entete = objet(contenu, "suite", "racine");
        String nom = texte(entete, "nom", "suite", origine);
        String description = texteFacultatif(entete, "description", "");

        Organisation organisation = lireOrganisation(objet(contenu, "organisation", "racine"));
        Workflow workflow = lireWorkflow(objet(contenu, "workflow", "racine"));
        Formulaire formulaire = lireFormulaire(objet(contenu, "formulaire", "racine"));
        List<EtapeDeParcours> parcours = lireParcours(liste(contenu, "parcours", "racine"), workflow, formulaire);

        if (organisation == null && workflow == null && formulaire == null && parcours.isEmpty()) {
            throw new DescriptionInvalide(origine,
                "la suite ne decrit ni organisation, ni workflow, ni formulaire, ni parcours : "
                + "elle n'a rien a executer");
        }
        return new DescriptionDeSuite(nom, description, organisation, workflow, formulaire, parcours);
    }

    private static Organisation lireOrganisation(Map<String, Object> bloc) {
        if (bloc == null) {
            return null;
        }
        List<String> entites = new ArrayList<>();
        for (Object entite : liste(bloc, "entites", "organisation")) {
            entites.add(String.valueOf(entite));
        }
        if (entites.isEmpty()) {
            throw new DescriptionInvalide("organisation", "au moins une entite est necessaire");
        }
        return new Organisation(entites, booleen(bloc, "affecter-un-agent"));
    }

    // ---------------------------------------------------------------- workflow

    private static Workflow lireWorkflow(Map<String, Object> bloc) {
        if (bloc == null) {
            return null;
        }
        String nom = texte(bloc, "nom", "workflow", null);
        List<String> etats = new ArrayList<>();
        for (Object etat : liste(bloc, "etats", "workflow")) {
            etats.add(String.valueOf(etat));
        }
        if (etats.isEmpty()) {
            throw new DescriptionInvalide("workflow", "au moins un etat est necessaire ; le premier "
                + "de la liste devient l'etat initial, celui que prend une reponse a sa soumission");
        }

        List<Action> actions = new ArrayList<>();
        int rang = 0;
        for (Object brut : liste(bloc, "actions", "workflow")) {
            rang++;
            String ou = "workflow > action " + rang;
            Map<String, Object> champs = commeObjet(brut, ou);
            String nomAction = texte(champs, "nom", ou, null);
            String de = texte(champs, "de", ou + " « " + nomAction + " »", null);
            String vers = texte(champs, "vers", ou + " « " + nomAction + " »", null);
            exigerEtatConnu(etats, de, ou + " « " + nomAction + " », champ « de »");
            exigerEtatConnu(etats, vers, ou + " « " + nomAction + " », champ « vers »");
            actions.add(new Action(nomAction, de, vers, lireTaches(champs, ou + " « " + nomAction + " »")));
        }
        return new Workflow(nom, etats, actions);
    }

    private static List<Tache> lireTaches(Map<String, Object> champs, String ou) {
        List<Tache> taches = new ArrayList<>();
        int rang = 0;
        for (Object brut : liste(champs, "taches", ou)) {
            rang++;
            String ouTache = ou + " > tache " + rang;
            Map<String, Object> bloc = commeObjet(brut, ouTache);
            String type = texte(bloc, "type", ouTache, null);
            Map<String, String> reglages = new LinkedHashMap<>();
            Map<String, Object> config = objet(bloc, "config", ouTache);
            if (config != null) {
                config.forEach((cle, valeur) -> reglages.put(cle, String.valueOf(valeur)));
            }
            taches.add(new Tache(type, Vocabulaire.cleDeTache(type, ouTache), reglages));
        }
        return taches;
    }

    private static void exigerEtatConnu(List<String> etats, String etat, String ou) {
        if (!etats.contains(etat)) {
            throw new DescriptionInvalide(ou, "etat inconnu : « " + etat + " »", etats);
        }
    }

    // -------------------------------------------------------------- formulaire

    private static Formulaire lireFormulaire(Map<String, Object> bloc) {
        if (bloc == null) {
            return null;
        }
        String titre = texte(bloc, "titre", "formulaire", null);
        List<Etape> etapes = new ArrayList<>();
        int rang = 0;
        for (Object brut : liste(bloc, "etapes", "formulaire")) {
            rang++;
            etapes.add(lireEtape(commeObjet(brut, "formulaire > etape " + rang), rang));
        }
        if (etapes.isEmpty()) {
            throw new DescriptionInvalide("formulaire", "au moins une etape est necessaire");
        }
        controlerConditions(etapes);

        List<String> rouvertes = new ArrayList<>();
        for (Object titreQuestion : liste(bloc, "questions-rouvertes", "formulaire")) {
            String libelle = String.valueOf(titreQuestion);
            if (etapes.stream().flatMap(e -> e.questions().stream())
                    .noneMatch(q -> q.titre().equals(libelle))) {
                throw new DescriptionInvalide("formulaire > questions-rouvertes",
                    "aucune question ne s'intitule « " + libelle + " »");
            }
            rouvertes.add(libelle);
        }

        return new Formulaire(titre, etapes, lireEnchainement(bloc, etapes),
            lireOptions(objet(bloc, "options", "formulaire")), rouvertes,
            booleen(bloc, "mapping-notification"));
    }

    private static Options lireOptions(Map<String, Object> bloc) {
        if (bloc == null) {
            return null;
        }
        Integer max = bloc.containsKey("reponses-max")
            ? entier(bloc, "reponses-max", 0, "formulaire > options")
            : null;
        return new Options(
            texteFacultatif(bloc, "disponible-du", null),
            texteFacultatif(bloc, "disponible-au", null),
            texteFacultatif(bloc, "message-indisponible", null),
            max,
            booleen(bloc, "une-reponse-par-usager"),
            booleen(bloc, "recapitulatif"),
            booleen(bloc, "brouillon"),
            booleen(bloc, "fil-ariane"),
            booleen(bloc, "authentification"));
    }

    private static Etape lireEtape(Map<String, Object> bloc, int rang) {
        String titre = texte(bloc, "titre", "formulaire > etape " + rang, null);
        String ou = "formulaire > etape « " + titre + " »";

        List<Groupe> groupes = new ArrayList<>();
        for (Object brut : liste(bloc, "groupes", ou)) {
            Map<String, Object> champs = commeObjet(brut, ou + " > groupe");
            String titreGroupe = texte(champs, "titre", ou + " > groupe", null);
            int iterations = entier(champs, "iterations", 1, ou + " > groupe « " + titreGroupe + " »");
            groupes.add(new Groupe(titreGroupe, iterations));
        }

        List<Question> questions = new ArrayList<>();
        int rangQuestion = 0;
        for (Object brut : liste(bloc, "questions", ou)) {
            rangQuestion++;
            questions.add(lireQuestion(commeObjet(brut, ou + " > question " + rangQuestion),
                ou + " > question " + rangQuestion, groupes));
        }

        return new Etape(titre,
            booleen(bloc, "initiale"),
            booleen(bloc, "finale"),
            booleen(bloc, "hors-parcours"),
            groupes, questions);
    }

    private static Question lireQuestion(Map<String, Object> bloc, String ou, List<Groupe> groupes) {
        String titre = texte(bloc, "titre", ou, null);
        String ouQuestion = ou + " « " + titre + " »";
        var type = Vocabulaire.typeDeQuestion(texte(bloc, "type", ouQuestion, null), ouQuestion);

        List<String> choix = new ArrayList<>();
        for (Object valeur : liste(bloc, "choix", ouQuestion)) {
            choix.add(String.valueOf(valeur));
        }

        String groupe = texteFacultatif(bloc, "groupe", null);
        if (groupe != null && groupes.stream().noneMatch(g -> g.titre().equals(groupe))) {
            throw new DescriptionInvalide(ouQuestion, "groupe inconnu sur cette etape : « " + groupe + " »",
                groupes.stream().map(Groupe::titre).toList());
        }

        Condition condition = null;
        Map<String, Object> si = objet(bloc, "affichee-si", ouQuestion);
        if (si != null) {
            condition = new Condition(
                texte(si, "question", ouQuestion + " > affichee-si", null),
                texte(si, "vaut", ouQuestion + " > affichee-si", null));
        }
        Validation validation = null;
        Map<String, Object> regle = objet(bloc, "validation", ouQuestion);
        if (regle != null) {
            validation = new Validation(
                texte(regle, "regle", ouQuestion + " > validation", null),
                texteFacultatif(regle, "message", "Saisie invalide"));
        }
        return new Question(type, titre, choix, groupe, condition, validation);
    }

    /**
     * Verifie que chaque condition d'affichage vise une question qui existe et porte la valeur citee.
     *
     * <p>Une condition qui vise une question absente, ou une valeur que la question ne propose pas,
     * produit un controle que le back-office accepte et qui ne se declenchera jamais : la question
     * cible restera masquee quoi que reponde l'usager, et la soumission echouera bien plus loin sur
     * un champ obligatoire invisible.</p>
     *
     * @param etapes les etapes lues
     */
    private static void controlerConditions(List<Etape> etapes) {
        Map<String, Question> parTitre = new LinkedHashMap<>();
        for (Etape etape : etapes) {
            for (Question question : etape.questions()) {
                parTitre.put(question.titre(), question);
            }
        }
        for (Etape etape : etapes) {
            for (Question question : etape.questions()) {
                Condition condition = question.afficheeSi();
                if (condition == null) {
                    continue;
                }
                String ou = "formulaire > etape « " + etape.titre() + " » > question « "
                    + question.titre() + " » > affichee-si";
                Question pilote = parTitre.get(condition.question());
                if (pilote == null) {
                    throw new DescriptionInvalide(ou,
                        "aucune question ne s'intitule « " + condition.question() + " »",
                        parTitre.keySet());
                }
                if (!pilote.choix().isEmpty() && !pilote.choix().contains(condition.vaut())) {
                    throw new DescriptionInvalide(ou,
                        "la question « " + condition.question() + " » ne propose pas « "
                        + condition.vaut() + " »", pilote.choix());
                }
            }
        }
    }

    private static List<Liaison> lireEnchainement(Map<String, Object> bloc, List<Etape> etapes) {
        Object brut = bloc.get("enchainement");
        if (brut == null || "lineaire".equals(String.valueOf(brut))) {
            return null;
        }
        List<Liaison> liaisons = new ArrayList<>();
        for (Object element : liste(bloc, "enchainement", "formulaire")) {
            Map<String, Object> champs = commeObjet(element, "formulaire > enchainement");
            String de = texte(champs, "de", "formulaire > enchainement", null);
            String vers = texte(champs, "vers", "formulaire > enchainement", null);
            exigerEtapeConnue(etapes, de, "formulaire > enchainement, champ « de »");
            exigerEtapeConnue(etapes, vers, "formulaire > enchainement, champ « vers »");
            Condition si = null;
            Map<String, Object> condition = objet(champs, "si", "formulaire > enchainement");
            if (condition != null) {
                si = new Condition(
                    texte(condition, "question", "formulaire > enchainement > si", null),
                    texte(condition, "vaut", "formulaire > enchainement > si", null));
            }
            liaisons.add(new Liaison(de, vers, si));
        }
        return liaisons;
    }

    private static void exigerEtapeConnue(List<Etape> etapes, String titre, String ou) {
        if (etapes.stream().noneMatch(e -> e.titre().equals(titre))) {
            throw new DescriptionInvalide(ou, "etape inconnue : « " + titre + " »",
                etapes.stream().map(Etape::titre).toList());
        }
    }

    // ---------------------------------------------------------------- parcours

    private static List<EtapeDeParcours> lireParcours(List<Object> blocs, Workflow workflow,
        Formulaire formulaire) {

        List<EtapeDeParcours> parcours = new ArrayList<>();
        int rang = 0;
        for (Object brut : blocs) {
            rang++;
            String ou = "parcours > etape " + rang;
            Map<String, Object> bloc = commeObjet(brut, ou);
            if (bloc.containsKey("soumission")) {
                parcours.add(lireSoumission(objet(bloc, "soumission", ou), ou, formulaire));
            } else if (bloc.containsKey("instruction")) {
                parcours.add(lireInstruction(objet(bloc, "instruction", ou), ou, workflow));
            } else if (bloc.containsKey("daemon")) {
                parcours.add(new Daemon(texte(bloc, "daemon", ou, null)));
            } else if (bloc.containsKey("export")) {
                String format = texte(bloc, "export", ou, null).toLowerCase(java.util.Locale.ROOT);
                if (!List.of("csv", "pdf").contains(format)) {
                    throw new DescriptionInvalide(ou, "format d'export inconnu : « " + format + " »",
                        List.of("csv", "pdf"));
                }
                parcours.add(new Export(format));
            } else {
                throw new DescriptionInvalide(ou, "etape de parcours inconnue",
                    List.of("soumission", "instruction", "daemon", "export"));
            }
        }
        return parcours;
    }

    private static Soumission lireSoumission(Map<String, Object> bloc, String ou, Formulaire formulaire) {
        if (formulaire == null) {
            throw new DescriptionInvalide(ou,
                "une soumission suppose un formulaire : la section « formulaire » est absente");
        }
        String nom = texteFacultatif(bloc, "nom", "Soumission");
        Map<String, String> reponses = new LinkedHashMap<>();
        Map<String, Object> brutes = objet(bloc, "reponses", ou);
        if (brutes != null) {
            brutes.forEach((question, valeur) -> reponses.put(question, String.valueOf(valeur)));
        }
        List<Saisie> valeurs = new ArrayList<>();
        for (Object element : liste(bloc, "valeurs", ou)) {
            Map<String, Object> champs = commeObjet(element, ou + " > valeurs");
            valeurs.add(new Saisie(
                texte(champs, "question", ou + " > valeurs", null),
                texte(champs, "valeur", ou + " > valeurs", null),
                natureDeSaisie(texteFacultatif(champs, "nature", "texte"), ou + " > valeurs")));
        }

        List<ControleDeVisibilite> controles = new ArrayList<>();
        List<String> etapesAttendues = new ArrayList<>();
        List<String> etapesEcartees = new ArrayList<>();
        List<SaisieRefusee> refus = new ArrayList<>();
        for (Object element : liste(bloc, "verifier", ou)) {
            Map<String, Object> champs = commeObjet(element, ou + " > verifier");
            if (champs.containsKey("etape")) {
                String etape = texte(champs, "etape", ou + " > verifier", null);
                exigerEtapeConnue(formulaire.etapes(), etape, ou + " > verifier, champ « etape »");
                (booleen(champs, "ecartee") ? etapesEcartees : etapesAttendues).add(etape);
            } else if (champs.containsKey("refuse")) {
                refus.add(new SaisieRefusee(
                    texte(champs, "question", ou + " > verifier", null),
                    texte(champs, "refuse", ou + " > verifier", null),
                    texte(champs, "accepte", ou + " > verifier", null),
                    texteFacultatif(champs, "message", "")));
            } else {
                String question = texte(champs, "question", ou + " > verifier", null);
                exigerQuestionConnue(formulaire, question, ou + " > verifier");
                controles.add(new ControleDeVisibilite(question, !booleen(champs, "masquee")));
            }
        }

        List<String> iterations = new ArrayList<>();
        for (Object element : liste(bloc, "iterations", ou)) {
            Map<String, Object> champs = commeObjet(element, ou + " > iterations");
            String etape = texte(champs, "etape", ou + " > iterations", null);
            exigerEtapeConnue(formulaire.etapes(), etape, ou + " > iterations, champ « etape »");
            iterations.add(etape);
        }

        for (String question : reponses.keySet()) {
            exigerQuestionConnue(formulaire, question, ou + " > reponses");
        }
        for (Saisie saisie : valeurs) {
            exigerQuestionConnue(formulaire, saisie.question(), ou + " > valeurs");
        }

        return new Soumission(nom, reponses, valeurs, controles, etapesAttendues, etapesEcartees,
            iterations, refus, booleen(bloc, "brouillon"));
    }

    /**
     * Nature d'une saisie, qui pilote la strategie employee en front office.
     *
     * @param mot mot employe dans le fichier
     * @param ou  emplacement, cite si le mot est inconnu
     * @return la nature attendue par la brique de saisie
     */
    private static String natureDeSaisie(String mot, String ou) {
        return switch (mot.toLowerCase(java.util.Locale.ROOT)) {
            case "texte" -> "text";
            case "nombre" -> "number";
            case "date" -> "date";
            default -> throw new DescriptionInvalide(ou,
                "nature de saisie inconnue : « " + mot + " »", List.of("texte", "nombre", "date"));
        };
    }

    /**
     * Verifie qu'une question citee par le parcours existe bien dans le formulaire decrit.
     *
     * <p>Une faute de frappe sur un libelle ne se verrait sinon qu'a l'execution, et sous la forme
     * d'une reponse qui ne trouve jamais sa question.</p>
     *
     * @param formulaire le formulaire decrit
     * @param titre      libelle cite
     * @param ou         emplacement dans le fichier
     */
    private static void exigerQuestionConnue(Formulaire formulaire, String titre, String ou) {
        boolean connue = formulaire.etapes().stream()
            .flatMap(etape -> etape.questions().stream())
            .anyMatch(question -> question.titre().equals(titre));
        if (!connue) {
            throw new DescriptionInvalide(ou, "aucune question ne s'intitule « " + titre + " »",
                formulaire.etapes().stream().flatMap(e -> e.questions().stream())
                    .map(Question::titre).toList());
        }
    }

    private static Instruction lireInstruction(Map<String, Object> bloc, String ou, Workflow workflow) {
        if (workflow == null) {
            throw new DescriptionInvalide(ou,
                "une instruction suppose un workflow : la section « workflow » est absente");
        }
        String action = texte(bloc, "action", ou, null);
        if (workflow.actions().stream().noneMatch(a -> a.nom().equals(action))) {
            throw new DescriptionInvalide(ou, "action inconnue : « " + action + " »",
                workflow.actions().stream().map(Action::nom).toList());
        }
        List<String> traces = new ArrayList<>();
        for (Object trace : liste(bloc, "traces", ou)) {
            traces.add(String.valueOf(trace));
        }
        Notification notification = null;
        Map<String, Object> notif = objet(bloc, "notification", ou);
        if (notif != null) {
            notification = new Notification(
                texte(notif, "canal", ou + " > notification", null),
                texte(notif, "message", ou + " > notification", null));
        }
        return new Instruction(action, texteFacultatif(bloc, "etat-attendu", null), traces,
            texteFacultatif(bloc, "sur-etat", null), notification, booleen(bloc, "lien-fo"));
    }

    // ------------------------------------------------------------- extraction

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objet(Map<String, Object> parent, String cle, String ou) {
        Object valeur = parent.get(cle);
        if (valeur == null) {
            return null;
        }
        if (!(valeur instanceof Map)) {
            throw new DescriptionInvalide(ou, "« " + cle + " » devrait etre un objet");
        }
        return (Map<String, Object>) valeur;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> liste(Map<String, Object> parent, String cle, String ou) {
        Object valeur = parent.get(cle);
        if (valeur == null) {
            return Collections.emptyList();
        }
        if (!(valeur instanceof List)) {
            throw new DescriptionInvalide(ou, "« " + cle + " » devrait etre une liste");
        }
        return (List<Object>) valeur;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> commeObjet(Object brut, String ou) {
        if (!(brut instanceof Map)) {
            throw new DescriptionInvalide(ou, "chaque element devrait etre un objet");
        }
        return (Map<String, Object>) brut;
    }

    private static String texte(Map<String, Object> bloc, String cle, String ou, String origine) {
        Object valeur = bloc == null ? null : bloc.get(cle);
        if (valeur == null || String.valueOf(valeur).isBlank()) {
            throw new DescriptionInvalide(origine == null ? ou : origine,
                "« " + cle + " » est obligatoire");
        }
        return String.valueOf(valeur).trim();
    }

    private static String texteFacultatif(Map<String, Object> bloc, String cle, String defaut) {
        Object valeur = bloc == null ? null : bloc.get(cle);
        return valeur == null || String.valueOf(valeur).isBlank() ? defaut : String.valueOf(valeur).trim();
    }

    private static boolean booleen(Map<String, Object> bloc, String cle) {
        Object valeur = bloc.get(cle);
        return valeur != null && Boolean.parseBoolean(String.valueOf(valeur));
    }

    private static int entier(Map<String, Object> bloc, String cle, int defaut, String ou) {
        Object valeur = bloc.get(cle);
        if (valeur == null) {
            return defaut;
        }
        try {
            return Integer.parseInt(String.valueOf(valeur).trim());
        } catch (NumberFormatException pasUnNombre) {
            throw new DescriptionInvalide(ou, "« " + cle + " » devrait etre un nombre, lu « " + valeur + " »");
        }
    }
}
