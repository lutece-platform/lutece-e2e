package fr.paris.lutece.e2e.tests.macro.scenarios;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.WorkflowContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.forms.*;

/**
 * Declaration d'interets simplifiee : transcription du formulaire de la commission de deontologie.
 *
 * <p>Fragment de scenario reutilisable, calque sur un formulaire reellement en service. Il
 * reproduit onze etapes, leurs regroupements — dont sept repetables —, les types de question
 * employes et les trente et un controles d'affichage conditionnel qui commandent chaque rubrique
 * declarative.</p>
 *
 * <p>La forme est partout la meme, et c'est elle que le scenario eprouve : chaque rubrique s'ouvre
 * sur une question fermee « avez-vous… », et les questions de detail ne s'affichent que si l'usager
 * repond « Oui ». Un groupe repetable permet ensuite d'en declarer plusieurs. Une regression sur
 * l'affichage conditionnel ou sur l'iteration de groupe se voit donc ici sur sept rubriques a la
 * fois, pas sur un cas isole.</p>
 *
 * <p>Trois ecarts assumes par rapport au formulaire d'origine, tous dus au back-office et non au
 * scenario :</p>
 * <ul>
 *   <li>Les questions « attribut de l'utilisateur MyLutece » de l'etape d'identite sont posees sans
 *       choisir l'attribut vise : la liste depend des attributs declares sur le site, qu'une
 *       instance de test ne porte pas.</li>
 *   <li>L'etape d'instruction est creee comme les autres puis detachee de l'enchainement, le
 *       back-office ne sachant pas creer une etape orpheline.</li>
 *   <li>Les libelles les plus longs sont abreges : ils servent de selecteur aux briques, et le
 *       back-office les tronque dans ses listes.</li>
 * </ul>
 */
public final class DeclarationInteretsScenario {

    public static final String TITRE_FORMULAIRE = "Declaration d'interets simplifiee";


    public static final String ETAPE_AVANT_DEMARRER = "Avant de demarrer";

    public static final String ETAPE_RENSEIGNEMENTS = "Renseignements personnels";

    public static final String ETAPE_ACTIVITES_PRO = "Activites professionnelles";

    public static final String ETAPE_ORGANES_DIRIGEANTS = "Participations aux organes dirigeants";

    public static final String ETAPE_CAPITAL_SOCIETE = "Participations dans le capital d'une societe";

    public static final String ETAPE_BENEVOLAT = "Fonctions benevoles";

    public static final String ETAPE_MANDATS_ELECTIFS = "Fonctions et mandats electifs";

    public static final String ETAPE_CONSULTANT = "Activites de consultant";

    public static final String ETAPE_CONJOINT = "Activites professionnelles du conjoint";

    public static final String ETAPE_VALIDATION = "Validation declaration";

    public static final String ETAPE_INSTRUCTION = "Instruction de la declaration";


    /** Les deux reponses de toutes les questions pivots : « Oui » ouvre la rubrique. */
    public static final String CHOIX_OUI = "Oui";

    public static final String CHOIX_NON = "Non";


    public static final String Q_NUMERO_DECLARATION = "Numero de la declaration";

    public static final String Q_CHARTE = "Je m'engage a respecter les regles de la charte";

    public static final String Q_CIVILITE = "Civilite";

    public static final String Q_NOM_USAGE = "Nom d'usage";

    public static final String Q_TELEPHONE = "Numero de telephone";

    public static final String Q_ADRESSE = "Adresse postale";

    public static final String Q_PROFESSION = "Profession";

    public static final String Q_QUALITE = "Je suis...";

    public static final String Q_DATE_ENTREE = "Date de debut de mandat";

    public static final String Q_OBSERVATIONS = "Observations";

    public static final String Q_CERTIFICATION = "Je certifie sur l'honneur l'exactitude";

    public static final String Q_COMMENTAIRES_INSTRUCTION = "Commentaires";

    public static final String Q_ANALYSE_CDVP = "Analyse du membre de la CDVP";

    public static final String Q_AVIS_SIGNE = "Avis definitif signe par le president";


    /** Les questions pivots, dans l'ordre des etapes declaratives. */
    public static final String Q_PIVOT_ACTIVITES_PRO = "Avez-vous des activites professionnelles";

    public static final String Q_PIVOT_ORGANES = "Participez-vous a un organe dirigeant";

    public static final String Q_PIVOT_CAPITAL = "Avez-vous des participations financieres";

    public static final String Q_PIVOT_BENEVOLAT = "Exercez-vous des fonctions benevoles";

    public static final String Q_PIVOT_MANDATS = "Exercez-vous des fonctions et mandats electifs";

    public static final String Q_PIVOT_CONSULTANT = "Avez-vous des activites de consultant";

    public static final String Q_PIVOT_CONJOINT = "Souhaitez-vous declarer les activites du conjoint";


    /**
     * Pour chaque question pivot, une question de detail qu'elle commande.
     *
     * <p>Les cibles sont nombreuses — cinq par rubrique le plus souvent. Une seule suffit a
     * constater qu'un controle fait son office : si elle apparait quand elle ne devrait pas, ou
     * l'inverse, le controle est en cause, et les quatre autres suivraient. Exposer la liste
     * entiere obligerait chaque appelant a choisir, sans rien lui apprendre de plus.</p>
     */
    private static final String[][] CIBLES_PAR_PIVOT = {
        { Q_PIVOT_ACTIVITES_PRO, "Nom de l'employeur" },
        { Q_PIVOT_ORGANES, "Denomination de l'organisme" },
        { Q_PIVOT_CAPITAL, "Denomination de la societe" },
        { Q_PIVOT_BENEVOLAT, "Denomination de la structure" },
        { Q_PIVOT_MANDATS, "Nature des fonctions et mandats" },
        { Q_PIVOT_CONSULTANT, "Nom de l'employeur consultant" },
        { Q_PIVOT_CONJOINT, "Nom de naissance du conjoint" },
    };

    /** Les sept questions pivots, dans l'ordre des etapes declaratives. */
    public static String[] pivots() {
        String[] titres = new String[CIBLES_PAR_PIVOT.length];
        for (int i = 0; i < CIBLES_PAR_PIVOT.length; i++) {
            titres[i] = CIBLES_PAR_PIVOT[i][0];
        }
        return titres;
    }

    /**
     * Une question de detail commandee par la question pivot donnee.
     *
     * @param pivot titre d'une question pivot
     * @return le titre d'une question qu'elle masque ou affiche
     */
    public static String cibleDe(String pivot) {
        for (String[] paire : CIBLES_PAR_PIVOT) {
            if (paire[0].equals(pivot)) {
                return paire[1];
            }
        }
        throw new IllegalArgumentException("'" + pivot + "' n'est pas une question pivot du scenario");
    }


    /** Rang des etapes dans {@code forms.steps}, dans l'ordre de creation. */
    private static final int RANG_AVANT_DEMARRER = 0;

    private static final int RANG_RENSEIGNEMENTS = 1;

    private static final int RANG_ACTIVITES_PRO = 2;

    private static final int RANG_ORGANES_DIRIGEANTS = 3;

    private static final int RANG_CAPITAL_SOCIETE = 4;

    private static final int RANG_BENEVOLAT = 5;

    private static final int RANG_MANDATS_ELECTIFS = 6;

    private static final int RANG_CONSULTANT = 7;

    private static final int RANG_CONJOINT = 8;

    private static final int RANG_VALIDATION = 9;

    private static final int RANG_INSTRUCTION = 10;

    /** Titre du groupe repetable de la premiere rubrique declarative. */
    public static final String GROUPE_ACTIVITES_PRO = "Activites professionnelles remunerees";

    /** Plafond d'iterations des groupes repetables, repris du formulaire d'origine. */
    private static final int ITERATIONS_MAX = 10;


    private DeclarationInteretsScenario() {
    }

    /**
     * Construit le formulaire complet : etapes, groupes, questions, controles et enchainement.
     *
     * <p>L'ordre importe. Les questions sont posees etape par etape pour que les briques les
     * rattachent a la bonne, puis les liaisons automatiques sont effacees avant de poser
     * l'enchainement voulu : creer une etape la relie d'office a la precedente, et cette liaison
     * implicite primerait sur celles du scenario.</p>
     *
     * @param page    page Playwright pilotant le navigateur
     * @param baseUrl adresse du site cible
     * @param suffix  suffixe unique du run, pour des libelles non collisionnants
     * @return le contexte formulaire alimente
     */
    @Step("Construire le formulaire de declaration d'interets")
    public static FormsContext construire(Page page, String baseUrl, String suffix) {
        FormsContext forms = new FormsContext(page, baseUrl, suffix);
        CreateFormMacroTest.run(forms, FormDataSet.defaults().withTitle(TITRE_FORMULAIRE));

        creerEtapes(forms);
        etapeAvantDemarrer(forms);
        etapeRenseignements(forms);
        rubriqueDeclarative(forms, RANG_ACTIVITES_PRO, Q_PIVOT_ACTIVITES_PRO,
            GROUPE_ACTIVITES_PRO, "Ajouter une activite professionnelle",
            "Nom de l'employeur", "Debut de la periode d'exercice", "Fin de la periode d'exercice",
            "Remuneration ou gratification percue", "Description de l'activite professionnelle");
        rubriqueDeclarative(forms, RANG_ORGANES_DIRIGEANTS, Q_PIVOT_ORGANES,
            "Participations aux organes dirigeants", "Ajouter une participation",
            "Denomination de l'organisme", "Debut de la periode de participation",
            "Fin de la periode de participation", "Remuneration percue pour l'organe dirigeant",
            "Description de la fonction exercee");
        etapeCapitalSociete(forms);
        etapeBenevolat(forms);
        rubriqueDeclarative(forms, RANG_MANDATS_ELECTIFS, Q_PIVOT_MANDATS,
            "Fonctions et mandats electifs", "Ajouter une fonction et mandat",
            "Nature des fonctions et mandats", "Date de debut de fonction ou de mandat",
            "Date de fin de fonction ou de mandat", "Indemnites percues", "Description du mandat");
        rubriqueDeclarative(forms, RANG_CONSULTANT, Q_PIVOT_CONSULTANT,
            "Activites de consultant", "Ajouter une activite de consultant",
            "Nom de l'employeur consultant", "Debut de l'activite de consultant",
            "Fin de l'activite de consultant", "Remuneration de l'activite de consultant",
            "Description de l'activite de consultant");
        etapeConjoint(forms);
        etapeValidation(forms);
        etapeInstruction(forms);

        enchainerLesEtapes(forms);
        return forms;
    }

    /**
     * Les onze etapes, dans l'ordre du parcours declaratif.
     *
     * <p>Le back-office marque la premiere etape creee comme initiale et la derniere comme finale.
     * Les deux marques sont reposees explicitement : l'etape d'instruction, creee en dernier, ne
     * doit pas heriter du caractere final qui revient a l'etape de validation.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void creerEtapes(FormsContext forms) {
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_AVANT_DEMARRER));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_RENSEIGNEMENTS));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_ACTIVITES_PRO));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_ORGANES_DIRIGEANTS));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_CAPITAL_SOCIETE));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_BENEVOLAT));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_MANDATS_ELECTIFS));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_CONSULTANT));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_CONJOINT));
        CreateStepMacroTest.run(forms, StepDataSet.finalStep(ETAPE_VALIDATION));
        CreateStepMacroTest.run(forms, StepDataSet.of(ETAPE_INSTRUCTION));

        SetStepInitialMacroTest.run(forms, StepTargetDataSet.of(RANG_AVANT_DEMARRER));
        SetStepFinalMacroTest.run(forms, StepTargetDataSet.of(RANG_VALIDATION));
        UnsetStepFinalMacroTest.run(forms, StepTargetDataSet.of(RANG_AVANT_DEMARRER));
    }

    /**
     * Etape liminaire : information de l'usager et acceptation de la charte.
     *
     * <p>Elle ne collecte presque rien — un numero de declaration auto-attribue et une case
     * d'engagement — mais porte trois regroupements de texte informatif. C'est le seul endroit du
     * formulaire ou un groupe sert a presenter plutot qu'a collecter.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void etapeAvantDemarrer(FormsContext forms) {
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.NUMBERING, Q_NUMERO_DECLARATION).onStep(RANG_AVANT_DEMARRER));

        CreateGroupMacroTest.run(forms, GroupDataSet.of("Dispositions generales").onStep(RANG_AVANT_DEMARRER));
        CreateGroupMacroTest.run(forms, GroupDataSet.of("Protection des donnees").onStep(RANG_AVANT_DEMARRER));
        CreateGroupMacroTest.run(forms, GroupDataSet.of("Engagement de transparence").onStep(RANG_AVANT_DEMARRER));

        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TERMS_OF_SERVICE, Q_CHARTE).onStep(RANG_AVANT_DEMARRER));
    }

    /**
     * Identite, contact, profession et date d'entree en fonction du declarant.
     *
     * <p>Quatre regroupements non repetables. Le formulaire d'origine y puise l'essentiel de
     * l'etat civil dans les attributs MyLutece du compte connecte et ne demande a la main que ce
     * que l'annuaire ne porte pas.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void etapeRenseignements(FormsContext forms) {
        CreateGroupMacroTest.run(forms, GroupDataSet.of("Identite du declarant").onStep(RANG_RENSEIGNEMENTS));
        CreateGroupMacroTest.run(forms, GroupDataSet.of("Information de contact").onStep(RANG_RENSEIGNEMENTS));
        CreateGroupMacroTest.run(forms, GroupDataSet.of("Profession exercee").onStep(RANG_RENSEIGNEMENTS));
        CreateGroupMacroTest.run(forms, GroupDataSet.of("Date d'entree en fonction").onStep(RANG_RENSEIGNEMENTS));

        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.RADIO, Q_CIVILITE).onStep(RANG_RENSEIGNEMENTS));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of(Q_CIVILITE, "Madame", "Monsieur"));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, Q_NOM_USAGE).onStep(RANG_RENSEIGNEMENTS));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Nom de naissance").onStep(RANG_RENSEIGNEMENTS));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Prenom").onStep(RANG_RENSEIGNEMENTS));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Date de naissance").onStep(RANG_RENSEIGNEMENTS));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Commune de naissance").onStep(RANG_RENSEIGNEMENTS));

        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TELEPHONE, Q_TELEPHONE).onStep(RANG_RENSEIGNEMENTS));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, Q_ADRESSE).onStep(RANG_RENSEIGNEMENTS));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.MYLUTECE_ATTRIBUTE, "Adresse courriel de contact").onStep(RANG_RENSEIGNEMENTS));

        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, Q_PROFESSION).onStep(RANG_RENSEIGNEMENTS));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.RADIO, Q_QUALITE).onStep(RANG_RENSEIGNEMENTS));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of(Q_QUALITE, "Elu", "Collaborateur", "Agent"));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.DATE, Q_DATE_ENTREE).onStep(RANG_RENSEIGNEMENTS));
    }

    /**
     * Rubrique declarative type : une question pivot, un groupe repetable, cinq questions masquees.
     *
     * <p>Quatre des sept rubriques du formulaire partagent exactement cette forme — seuls changent
     * les libelles. La factoriser evite de recopier quatre fois la meme sequence, mais surtout elle
     * nomme le motif : c'est lui que le scenario surveille.</p>
     *
     * <p>Les cinq controles sont poses apres les questions, une fois que toutes existent : un
     * controle conditionnel reference une question pilote et une question cible, qui doivent donc
     * etre creees toutes les deux.</p>
     *
     * @param forms      contexte formulaire courant
     * @param rangEtape  rang de l'etape dans {@code forms.steps}
     * @param pivot      titre de la question fermee qui commande la rubrique
     * @param groupe     titre du groupe repetable portant le detail
     * @param libelleAjout libelle du bouton d'ajout d'iteration
     * @param texte      question de texte court du detail
     * @param dateDebut  question de date de debut
     * @param dateFin    question de date de fin
     * @param remuneration question de remuneration
     * @param description question de description libre
     */
    private static void rubriqueDeclarative(FormsContext forms, int rangEtape, String pivot,
        String groupe, String libelleAjout, String texte, String dateDebut, String dateFin,
        String remuneration, String description) {

        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.RADIO, pivot).onStep(rangEtape));
        AddQuestionChoicesMacroTest.run(forms, QuestionChoicesDataSet.of(pivot, CHOIX_OUI, CHOIX_NON));

        CreateGroupMacroTest.run(forms, GroupDataSet.repeatable(groupe, ITERATIONS_MAX).onStep(rangEtape));
        int rangGroupe = forms.groups.size() - 1;

        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXT, texte).onStep(rangEtape));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.DATE, dateDebut).onStep(rangEtape));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.DATE, dateFin).onStep(rangEtape));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXTAREA, remuneration).onStep(rangEtape));
        AddQuestionMacroTest.run(forms, QuestionDataSet.of(QuestionType.TEXTAREA, description).onStep(rangEtape));

        rangerDansLeGroupe(forms, rangGroupe, texte, dateDebut, dateFin, remuneration, description);
        masquerDerriereLePivot(forms, pivot, texte, dateDebut, dateFin, remuneration, description);
    }

    /**
     * Participations financieres : meme motif, mais le detail est entierement textuel.
     *
     * <p>Aucune date : une participation au capital se decrit par des montants et des parts, pas
     * par une periode. C'est la seule rubrique dans ce cas.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void etapeCapitalSociete(FormsContext forms) {
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.RADIO, Q_PIVOT_CAPITAL).onStep(RANG_CAPITAL_SOCIETE));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of(Q_PIVOT_CAPITAL, CHOIX_OUI, CHOIX_NON));

        CreateGroupMacroTest.run(forms, GroupDataSet
            .repeatable("Participations financieres directes", ITERATIONS_MAX)
            .onStep(RANG_CAPITAL_SOCIETE));
        int rangGroupe = forms.groups.size() - 1;

        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Denomination de la societe").onStep(RANG_CAPITAL_SOCIETE));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Nombre de parts ou pourcentage du capital").onStep(RANG_CAPITAL_SOCIETE));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Evaluation de la participation financiere").onStep(RANG_CAPITAL_SOCIETE));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Remuneration percue l'annee precedente").onStep(RANG_CAPITAL_SOCIETE));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXTAREA, "Description de la participation").onStep(RANG_CAPITAL_SOCIETE));

        rangerDansLeGroupe(forms, rangGroupe,
            "Denomination de la societe", "Nombre de parts ou pourcentage du capital",
            "Evaluation de la participation financiere", "Remuneration percue l'annee precedente",
            "Description de la participation");
        masquerDerriereLePivot(forms, Q_PIVOT_CAPITAL,
            "Denomination de la societe", "Nombre de parts ou pourcentage du capital",
            "Evaluation de la participation financiere", "Remuneration percue l'annee precedente",
            "Description de la participation");
    }

    /**
     * Fonctions benevoles : la rubrique la plus courte, deux questions masquees seulement.
     *
     * <p>Elle verifie qu'une question pivot commande aussi bien deux cibles que cinq : le nombre
     * de controles attaches a une meme question n'est pas suppose fixe.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void etapeBenevolat(FormsContext forms) {
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.RADIO, Q_PIVOT_BENEVOLAT).onStep(RANG_BENEVOLAT));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of(Q_PIVOT_BENEVOLAT, CHOIX_OUI, CHOIX_NON));

        CreateGroupMacroTest.run(forms, GroupDataSet
            .repeatable("Fonctions benevoles declarees", ITERATIONS_MAX)
            .onStep(RANG_BENEVOLAT));
        int rangGroupe = forms.groups.size() - 1;

        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Denomination de la structure").onStep(RANG_BENEVOLAT));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXTAREA, "Description des responsabilites exercees").onStep(RANG_BENEVOLAT));

        rangerDansLeGroupe(forms, rangGroupe,
            "Denomination de la structure", "Description des responsabilites exercees");
        masquerDerriereLePivot(forms, Q_PIVOT_BENEVOLAT,
            "Denomination de la structure", "Description des responsabilites exercees");
    }

    /**
     * Activites du conjoint : deux regroupements, l'un pour l'identite, l'autre pour les activites.
     *
     * <p>Seule rubrique ou le detail masque se repartit sur deux groupes : l'identite du conjoint
     * n'est declaree qu'une fois, ses activites autant de fois que necessaire.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void etapeConjoint(FormsContext forms) {
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.RADIO, Q_PIVOT_CONJOINT).onStep(RANG_CONJOINT));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of(Q_PIVOT_CONJOINT, CHOIX_OUI, CHOIX_NON));

        CreateGroupMacroTest.run(forms, GroupDataSet.of("Informations du conjoint").onStep(RANG_CONJOINT));
        int rangIdentite = forms.groups.size() - 1;
        CreateGroupMacroTest.run(forms, GroupDataSet
            .repeatable("Activites professionnelles du conjoint", ITERATIONS_MAX)
            .onStep(RANG_CONJOINT));
        int rangActivites = forms.groups.size() - 1;

        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Nom de naissance du conjoint").onStep(RANG_CONJOINT));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Prenom du conjoint").onStep(RANG_CONJOINT));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXT, "Nom de l'employeur du conjoint").onStep(RANG_CONJOINT));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXTAREA, "Description de l'activite du conjoint").onStep(RANG_CONJOINT));

        rangerDansLeGroupe(forms, rangIdentite,
            "Nom de naissance du conjoint", "Prenom du conjoint");
        rangerDansLeGroupe(forms, rangActivites,
            "Nom de l'employeur du conjoint", "Description de l'activite du conjoint");
        masquerDerriereLePivot(forms, Q_PIVOT_CONJOINT,
            "Nom de naissance du conjoint", "Prenom du conjoint",
            "Nom de l'employeur du conjoint", "Description de l'activite du conjoint");
    }

    /**
     * Etape finale : observations libres et certification sur l'honneur.
     *
     * @param forms contexte formulaire courant
     */
    private static void etapeValidation(FormsContext forms) {
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXTAREA, Q_OBSERVATIONS).onStep(RANG_VALIDATION));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.CHECKBOX, Q_CERTIFICATION).onStep(RANG_VALIDATION));
        AddQuestionChoicesMacroTest.run(forms,
            QuestionChoicesDataSet.of(Q_CERTIFICATION, "Je certifie l'exactitude des renseignements"));
    }

    /**
     * Etape d'instruction, reservee a l'agent et tenue hors du parcours de l'usager.
     *
     * <p>Elle n'est atteinte par aucune liaison : le workflow la presente a l'instructeur au
     * travers d'une tache d'edition de reponse. Ses trois questions — un commentaire et deux
     * televersements — sont les champs que remplit la commission.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void etapeInstruction(FormsContext forms) {
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.TEXTAREA, Q_COMMENTAIRES_INSTRUCTION).onStep(RANG_INSTRUCTION));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.FILE, Q_ANALYSE_CDVP).onStep(RANG_INSTRUCTION));
        AddQuestionMacroTest.run(forms,
            QuestionDataSet.of(QuestionType.FILE, Q_AVIS_SIGNE).onStep(RANG_INSTRUCTION));
    }

    /**
     * Enchainement lineaire des dix etapes du parcours usager.
     *
     * <p>Les liaisons automatiques sont d'abord effacees sur toutes les etapes, y compris la
     * onzieme : creee en dernier, l'etape d'instruction a herite d'une liaison depuis l'etape de
     * validation, qui la ferait entrer dans le parcours de l'usager alors qu'elle est reservee a
     * l'agent.</p>
     *
     * @param forms contexte formulaire courant
     */
    private static void enchainerLesEtapes(FormsContext forms) {
        for (int etape = 0; etape < forms.steps.size(); etape++) {
            ClearStepTransitionsMacroTest.run(forms, StepTargetDataSet.of(etape));
        }
        for (int depuis = RANG_AVANT_DEMARRER; depuis < RANG_VALIDATION; depuis++) {
            CreateTransitionMacroTest.run(forms, TransitionDataSet.of(depuis, depuis + 1));
        }
    }

    /**
     * Range des questions dans un groupe, dans l'ordre donne.
     *
     * <p>Une question creee sur une etape se pose a la racine de celle-ci : c'est le deplacement
     * qui la fait entrer dans un groupe. L'etape compte pour les groupes repetables, ou seule
     * l'appartenance au groupe rend une question iterable — hors du groupe, elle resterait unique
     * quel que soit le nombre d'iterations declarees.</p>
     *
     * @param forms      contexte formulaire courant
     * @param rangGroupe rang du groupe dans {@code forms.groups}
     * @param titres     titres des questions a y ranger
     */
    private static void rangerDansLeGroupe(FormsContext forms, int rangGroupe, String... titres) {
        for (String titre : titres) {
            MoveQuestionIntoGroupMacroTest.run(forms,
                GroupTargetDataSet.of(rangGroupe, indexQuestion(forms, titre)));
        }
    }

    /**
     * Masque des questions derriere la reponse « Oui » d'une question pivot.
     *
     * @param forms  contexte formulaire courant
     * @param pivot  titre de la question fermee qui commande l'affichage
     * @param cibles titres des questions a n'afficher que si le pivot vaut « Oui »
     */
    private static void masquerDerriereLePivot(FormsContext forms, String pivot, String... cibles) {
        int rangPivot = indexQuestion(forms, pivot);
        for (String cible : cibles) {
            AddConditionalControlMacroTest.run(forms,
                ControlDataSet.conditionalSurListe(rangPivot, indexQuestion(forms, cible), CHOIX_OUI));
        }
    }

    /**
     * Une imbrication question/groupe que le formulaire garantit, a verifier apres coup.
     *
     * <p>La premiere question du formulaire — le numero de declaration — est posee a la racine de
     * son etape : la prendre pour exemple d'imbrication, comme le ferait un jeu de donnees par
     * defaut, verifierait le contraire de ce qui est voulu. On designe donc une paire dont le
     * scenario a reellement demande l'imbrication.</p>
     *
     * @param forms contexte formulaire courant
     * @return le couple (groupe, question) a controler
     */
    public static GroupTargetDataSet imbricationConnue(FormsContext forms) {
        return GroupTargetDataSet.of(
            indexGroupe(forms, GROUPE_ACTIVITES_PRO),
            indexQuestion(forms, cibleDe(Q_PIVOT_ACTIVITES_PRO)));
    }

    /**
     * Position d'un groupe dans le contexte, par le debut de son titre.
     *
     * <p>La recherche porte sur le debut du titre : la brique de creation ajoute a chaque groupe
     * le suffixe unique du run, que l'appelant ne connait pas.</p>
     *
     * @param forms   contexte formulaire courant
     * @param prefixe debut du titre du groupe
     * @return sa position dans {@code forms.groups}
     */
    private static int indexGroupe(FormsContext forms, String prefixe) {
        for (int i = 0; i < forms.groups.size(); i++) {
            if (forms.groups.get(i).title.startsWith(prefixe)) {
                return i;
            }
        }
        throw new IllegalStateException("Groupe '" + prefixe + "' absent du contexte");
    }

    /**
     * Position d'une question dans le contexte, par son titre.
     *
     * @param forms contexte formulaire courant
     * @param titre titre de la question
     * @return sa position dans {@code forms.questions}
     */
    private static int indexQuestion(FormsContext forms, String titre) {
        for (int i = 0; i < forms.questions.size(); i++) {
            if (titre.equals(forms.questions.get(i).title)) {
                return i;
            }
        }
        throw new IllegalStateException("Question '" + titre + "' absente du contexte");
    }

    /**
     * Associe le formulaire au workflow puis le publie.
     *
     * @param forms contexte formulaire courant
     * @param wf    workflow a associer
     */
    @Step("Associer le workflow et publier la declaration")
    public static void mettreEnService(FormsContext forms, WorkflowContext wf) {
        forms.workflowId = wf.workflowId;
        forms.workflowName = wf.workflowName;
        AssociateWorkflowMacroTest.run(forms, WorkflowRefDataSet.of(wf.workflowName));
        PublishFormMacroTest.run(forms, PublishDataSet.defaults());
    }
}
