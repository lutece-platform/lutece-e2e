package fr.paris.lutece.e2e.tests.declaratif;

import fr.paris.lutece.e2e.tests.macro.data.QuestionType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Les mots du fichier de suite, et ce qu'ils designent dans le depot.
 *
 * <p>Un fichier de suite est ecrit par quelqu'un qui connait sa demarche, pas le code des briques.
 * Il nomme donc « liste deroulante » et non {@code QuestionType.SELECT}, « notification usager » et
 * non {@code taskNotifyGru}. Cette classe est le seul endroit ou les deux vocabulaires se
 * rencontrent : ajouter un mot ne demande de toucher a rien d'autre.</p>
 *
 * <p>La correspondance est volontairement permissive sur la forme — casse, accents et separateurs
 * sont ignores — et stricte sur le fond : un mot inconnu arrete la lecture en listant ceux qui
 * conviennent, plutot que de laisser la suite s'executer a cote de ce qui etait decrit.</p>
 */
public final class Vocabulaire {

    /** Types de question, par leur nom dans le fichier. */
    private static final Map<String, QuestionType> QUESTIONS = new LinkedHashMap<>();

    /** Types de tache de workflow, par leur nom dans le fichier, vers la cle du plugin. */
    private static final Map<String, String> TACHES = new LinkedHashMap<>();

    static {
        question("texte", QuestionType.TEXT);
        question("texte-long", QuestionType.TEXTAREA);
        question("nombre", QuestionType.NUMBER);
        question("date", QuestionType.DATE);
        question("radio", QuestionType.RADIO);
        question("bouton-radio", QuestionType.RADIO);
        question("case-a-cocher", QuestionType.CHECKBOX);
        question("liste-deroulante", QuestionType.SELECT);
        question("liste-ordonnee", QuestionType.SELECT_ORDER);
        question("fichier", QuestionType.FILE);
        question("image", QuestionType.IMAGE);
        question("camera", QuestionType.CAMERA);
        question("commentaire", QuestionType.COMMENT);
        question("conditions-utilisation", QuestionType.TERMS_OF_SERVICE);
        question("numerotation", QuestionType.NUMBERING);
        question("telephone", QuestionType.TELEPHONE);
        question("geolocalisation", QuestionType.GEOLOCATION);
        question("creneau", QuestionType.SLOT);
        question("session", QuestionType.SESSION);
        question("attribut-mylutece", QuestionType.MYLUTECE_ATTRIBUTE);

        tache("statut-publication", "modifyUpdateStatusTask");
        tache("date-modification", "modifyUpdateDateTypeTask");
        tache("commentaire", "taskTypeComment");
        tache("confirmation", "taskTypeConfirmAction");
        tache("notification-usager", "taskNotifyGru");
        tache("alerte-usager", "taskAlertGru");
        tache("piece-jointe", "taskTypeUpload");
        tache("demande-complement", "completeFormResponseTypeTask");
        tache("correction-saisie", "resubmitFormResponseTypeTask");
        tache("edition-reponse", "editFormResponseTypeTask");
        tache("edition-reponse-fo", "editFormResponseFoTypeTask");
        tache("edition-reponse-auto", "editFormResponseTypeAutoUpdateTask");
        tache("duplication-reponse", "taskDuplicateFormResponse");
        tache("valorisation-auto", "linkedValuesFormResponseTypeTask");
        tache("affectation-entite", "taskUnitAssignmentManual");
        tache("affectation-entite-auto", "taskUnitAssignmentAutomatic");
        tache("desaffectation-entite", "taskUnitUnassignment");
        tache("notification-entite", "taskUnitAssignmentNotification");
        tache("assignation-utilisateur", "assignUserResourceTask");
        tache("archivage", "taskTypeArchive");
        tache("choix", "taskTypeChoice");
        tache("changement-etat-auto", "changeStateTask");
        tache("choix-etat-suivant", "chooseStateTask");
        tache("export-pdf", "formsPDFTask");
    }

    private Vocabulaire() {
    }

    private static void question(String mot, QuestionType type) {
        QUESTIONS.put(mot, type);
    }

    private static void tache(String mot, String cle) {
        TACHES.put(mot, cle);
    }

    /**
     * Type de question designe par un mot du fichier.
     *
     * @param mot     mot employe dans le fichier
     * @param contexte emplacement dans le fichier, cite si le mot est inconnu
     * @return le type correspondant
     */
    public static QuestionType typeDeQuestion(String mot, String contexte) {
        QuestionType type = QUESTIONS.get(normaliser(mot));
        if (type == null) {
            throw new DescriptionInvalide(contexte, "type de question inconnu : « " + mot + " »",
                QUESTIONS.keySet());
        }
        return type;
    }

    /**
     * Cle de tache de workflow designee par un mot du fichier.
     *
     * @param mot      mot employe dans le fichier
     * @param contexte emplacement dans le fichier, cite si le mot est inconnu
     * @return la cle attendue par le plugin
     */
    public static String cleDeTache(String mot, String contexte) {
        String cle = TACHES.get(normaliser(mot));
        if (cle == null) {
            throw new DescriptionInvalide(contexte, "type de tache inconnu : « " + mot + " »",
                TACHES.keySet());
        }
        return cle;
    }

    /**
     * Forme comparable d'un mot : sans casse, sans accent, les separateurs ramenes au tiret.
     *
     * <p>« Liste deroulante », « liste_deroulante » et « LISTE-DÉROULANTE » designent la meme chose.
     * Exiger une orthographe exacte ferait echouer des fichiers justes pour une raison qui n'a rien
     * a voir avec la demarche decrite.</p>
     *
     * @param mot mot tel qu'ecrit dans le fichier
     * @return sa forme comparable
     */
    private static String normaliser(String mot) {
        if (mot == null) {
            return "";
        }
        String sansAccent = java.text.Normalizer.normalize(mot.trim().toLowerCase(java.util.Locale.ROOT),
                java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sansAccent.replaceAll("[\\s_]+", "-");
    }
}
