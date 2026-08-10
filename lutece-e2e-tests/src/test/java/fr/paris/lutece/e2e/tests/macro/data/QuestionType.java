package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Types de questions actifs du plugin Forms 4.0.2, avec le libelle du bouton de selection du type
 * dans l'assistant "Ajouter une question" (back-office).
 *
 * <p>Certains types exigent des champs supplementaires obligatoires (voir {@link QuestionDataSet}) :
 * {@code TEXTAREA} -> hauteur ; {@code FILE}/{@code IMAGE} -> taille max + nombre de fichiers.</p>
 */
public enum QuestionType {
    TEXT("Zone de texte court"),
    TEXTAREA("Zone de texte long"),
    NUMBER("Nombre"),
    DATE("Date"),
    RADIO("Bouton radio"),
    CHECKBOX("Case à cocher"),
    SELECT("Liste déroulante"),
    SELECT_ORDER("Liste triable"),
    FILE("Fichier"),
    IMAGE("Téléchargement d'image"),
    CAMERA("Camera"),
    COMMENT("Commentaire"),
    TERMS_OF_SERVICE("Conditions d'utilisation"),
    NUMBERING("Numérotation"),
    TELEPHONE("Numéro de téléphone"),
    GEOLOCATION("Géolocalisation"),
    SLOT("Creneau horaire"),
    SESSION("Session"),
    MYLUTECE_ATTRIBUTE("Attribut de l'utilisateur MyLutece");

    /** Libelle du bouton de choix du type dans l'assistant d'ajout de question. */
    public final String buttonLabel;

    QuestionType(String buttonLabel) {
        this.buttonLabel = buttonLabel;
    }
}
