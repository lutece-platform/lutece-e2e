package fr.paris.lutece.e2e.tests.declaratif;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Le lecteur de suites, eprouve sans navigateur.
 *
 * <p>Separer la lecture de l'execution permet de verifier en quelques millisecondes ce qui
 * demanderait sinon un conteneur et un quart d'heure. Ces tests portent sur ce qu'un auteur de
 * fichier se trompera le plus souvent a ecrire : un mot hors vocabulaire, une condition qui vise
 * une question absente, une action dont l'etat n'existe pas.</p>
 */
@Epic("Suites declaratives")
@Feature("Lecture et validation d'un fichier de suite")
@Tag("unit")
@DisplayName("Lecture d'un fichier de suite")
class LecteurDeSuiteTest {

    @Test
    @DisplayName("La suite de reference est lue et validee")
    void suiteDeReference() {
        var description = LecteurDeSuite.depuis(Path.of("src/test/resources/suites/deontologie.e2e-suite.yml"));

        assertAll(
            () -> assertEquals("Declaration d'interets simplifiee", description.nom()),
            () -> assertEquals(8, description.workflow().etats().size(),
                "Les huit etats du workflow doivent etre lus"),
            () -> assertEquals(7, description.workflow().actions().size(),
                "Les sept actions du workflow doivent etre lues"),
            () -> assertEquals(8, description.formulaire().etapes().size(),
                "Les huit etapes du formulaire doivent etre lues"),
            () -> assertNull(description.formulaire().enchainement(),
                "Un enchainement « lineaire » ne produit pas de liaisons explicites"),
            () -> assertEquals(3, description.parcours().size(),
                "Le parcours compte une soumission et deux instructions"));
    }

    @Test
    @DisplayName("Un type de question hors vocabulaire est refuse, en listant ceux qui conviennent")
    void typeDeQuestionInconnu() {
        var echec = assertThrows(DescriptionInvalide.class, () -> lire("""
            suite: { nom: Essai }
            formulaire:
              titre: Essai
              etapes:
                - titre: Unique
                  questions:
                    - { type: zone-de-texte, titre: Nom }
            """));

        assertAll(
            () -> assertTrue(echec.getMessage().contains("zone-de-texte"),
                "Le message doit citer le mot fautif"),
            () -> assertTrue(echec.getMessage().contains("texte-long"),
                "Le message doit enumerer les types acceptes"),
            () -> assertTrue(echec.getMessage().contains("etape « Unique »")
                    && echec.getMessage().contains("« Nom »"),
                "Le message doit situer la question dans le fichier"));
    }

    @Test
    @DisplayName("Une condition qui vise une question absente est refusee")
    void conditionSansPilote() {
        var echec = assertThrows(DescriptionInvalide.class, () -> lire("""
            suite: { nom: Essai }
            formulaire:
              titre: Essai
              etapes:
                - titre: Unique
                  questions:
                    - type: texte
                      titre: Detail
                      affichee-si:
                        question: "Avez-vous ?"
                        vaut: Oui
            """));

        assertTrue(echec.getMessage().contains("Avez-vous ?"),
            "Le message doit citer la question introuvable : " + echec.getMessage());
    }

    @Test
    @DisplayName("Une condition sur une valeur que la question ne propose pas est refusee")
    void conditionSurValeurAbsente() {
        var echec = assertThrows(DescriptionInvalide.class, () -> lire("""
            suite: { nom: Essai }
            formulaire:
              titre: Essai
              etapes:
                - titre: Unique
                  questions:
                    - type: radio
                      titre: "Avez-vous ?"
                      choix: [Oui, Non]
                    - type: texte
                      titre: Detail
                      affichee-si:
                        question: "Avez-vous ?"
                        vaut: Peut-etre
            """));

        assertTrue(echec.getMessage().contains("Peut-etre"),
            "Le message doit citer la valeur impossible : " + echec.getMessage());
    }

    @Test
    @DisplayName("Une action dont l'etat de depart n'existe pas est refusee")
    void actionSurEtatInconnu() {
        var echec = assertThrows(DescriptionInvalide.class, () -> lire("""
            suite: { nom: Essai }
            workflow:
              nom: Essai
              etats: [Nouvelle]
              actions:
                - { nom: Traiter, de: Recue, vers: Nouvelle }
            """));

        assertTrue(echec.getMessage().contains("Recue"),
            "Le message doit citer l'etat inconnu : " + echec.getMessage());
    }

    @Test
    @DisplayName("Une question rangee dans un groupe absent de son etape est refusee")
    void groupeInconnu() {
        var echec = assertThrows(DescriptionInvalide.class, () -> lire("""
            suite: { nom: Essai }
            formulaire:
              titre: Essai
              etapes:
                - titre: Unique
                  groupes: [{ titre: Present }]
                  questions:
                    - { type: texte, titre: Nom, groupe: Absent }
            """));

        assertTrue(echec.getMessage().contains("Absent") && echec.getMessage().contains("Present"),
            "Le message doit citer le groupe absent et ceux qui existent : " + echec.getMessage());
    }

    @Test
    @DisplayName("Une suite qui ne decrit rien d'executable est refusee")
    void suiteVide() {
        var echec = assertThrows(DescriptionInvalide.class, () -> lire("suite: { nom: Essai }"));

        assertTrue(echec.getMessage().contains("rien a executer"),
            "Le message doit dire que la suite est sans objet : " + echec.getMessage());
    }

    @Test
    @DisplayName("Le vocabulaire ignore la casse, les accents et les separateurs")
    void vocabulairePermissifSurLaForme() {
        var description = lire("""
            suite: { nom: Essai }
            formulaire:
              titre: Essai
              etapes:
                - titre: Unique
                  questions:
                    - { type: "Liste_Déroulante", titre: Choix, choix: [A, B] }
            """);

        assertEquals(fr.paris.lutece.e2e.tests.macro.data.QuestionType.SELECT,
            description.formulaire().etapes().get(0).questions().get(0).type());
    }

    @Test
    @DisplayName("Le format JSON est accepte, le YAML en etant un sur-ensemble")
    void formatJsonAccepte() {
        var description = lire("""
            {"suite": {"nom": "Essai JSON"},
             "formulaire": {"titre": "Essai",
               "etapes": [{"titre": "Unique",
                 "questions": [{"type": "texte", "titre": "Nom"}]}]}}
            """);

        assertEquals("Essai JSON", description.nom());
    }

    private static DescriptionDeSuite lire(String contenu) {
        InputStream flux = new ByteArrayInputStream(contenu.getBytes(StandardCharsets.UTF_8));
        return LecteurDeSuite.lire(flux, "essai");
    }
}
