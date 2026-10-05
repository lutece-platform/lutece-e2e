package fr.paris.lutece.e2e.tests.declaratif;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le validateur de la skill connait-il les memes mots que le lecteur ?
 *
 * <p>La skill {@code creer-suite-e2e} embarque un validateur en Python, pour qu'un site puisse
 * verifier sa suite sans disposer de ce depot ni d'un navigateur. Il reproduit donc le vocabulaire
 * de {@link Vocabulaire}, et deux listes recopiees divergent toujours : un type ajoute ici serait
 * refuse la-bas, et le site s'entendrait dire que son fichier est fautif alors qu'il est juste.</p>
 *
 * <p>Ce test compare les deux. Il echoue au moment ou l'ecart nait, et non le jour ou quelqu'un
 * bute dessus.</p>
 */
@Epic("Suites declaratives")
@Feature("Lecture et validation d'un fichier de suite")
@Tag("unit")
@DisplayName("Vocabulaire du validateur de la skill")
class VocabulaireDuValidateurTest {

    /** Le validateur embarque par la skill. */
    private static final Path VALIDATEUR =
        Path.of(".claude/skills/creer-suite-e2e/scripts/valider-suite.py");

    @Test
    @DisplayName("Le validateur de la skill connait exactement les memes mots que le lecteur")
    void vocabulairesAlignes() throws IOException {
        Path validateur = resoudre();
        assertTrue(Files.isReadable(validateur),
            "Le validateur de la skill est introuvable : " + validateur.toAbsolutePath()
            + ". S'il a ete deplace, corriger ce test ; s'il a ete supprime, un site ne peut plus "
            + "verifier sa suite sans ce depot.");

        String source = Files.readString(validateur);
        Set<String> questionsPython = listePython(source, "TYPES_DE_QUESTION");
        Set<String> tachesPython = listePython(source, "TYPES_DE_TACHE");

        assertAll(
            () -> assertEquals(motsDuLecteur("QUESTIONS"), questionsPython,
                "Les types de question du validateur et du lecteur ont diverge"),
            () -> assertEquals(motsDuLecteur("TACHES"), tachesPython,
                "Les types de tache du validateur et du lecteur ont diverge"));
    }

    /**
     * Le chemin du validateur, que le test soit lance depuis la racine ou depuis le module.
     *
     * @return le chemin existant, ou celui depuis la racine si aucun ne convient
     */
    private static Path resoudre() {
        return Files.isReadable(VALIDATEUR) ? VALIDATEUR : Path.of("..").resolve(VALIDATEUR);
    }

    /**
     * Les mots d'une table du vocabulaire Java, lus dans sa source.
     *
     * <p>La lecture passe par la source plutot que par la classe : {@link Vocabulaire} n'expose pas
     * ses tables, et les ouvrir pour ce seul test elargirait son contrat public sans raison.</p>
     *
     * @param table « QUESTIONS » ou « TACHES »
     * @return les mots declares
     */
    private static Set<String> motsDuLecteur(String table) throws IOException {
        Path source = Path.of("src/test/java/fr/paris/lutece/e2e/tests/declaratif/Vocabulaire.java");
        if (!Files.isReadable(source)) {
            source = Path.of("lutece-e2e-tests").resolve(source);
        }
        String appel = "QUESTIONS".equals(table) ? "question" : "tache";
        Matcher m = Pattern.compile(appel + "\\(\"([^\"]+)\"").matcher(Files.readString(source));
        Set<String> mots = new LinkedHashSet<>();
        while (m.find()) {
            mots.add(m.group(1));
        }
        assertTrue(mots.size() > 10, "Aucun mot lu dans " + table + " : le format de "
            + "Vocabulaire.java a change, ce test ne mesure plus rien");
        return mots;
    }

    /**
     * Les chaines d'une liste Python, lues dans la source du validateur.
     *
     * @param source source du validateur
     * @param nom    nom de la liste
     * @return les mots declares
     */
    private static Set<String> listePython(String source, String nom) {
        Matcher bloc = Pattern.compile(nom + "\\s*=\\s*\\[(.*?)\\]", Pattern.DOTALL).matcher(source);
        assertTrue(bloc.find(), "Liste « " + nom + " » introuvable dans le validateur de la skill");
        Matcher mot = Pattern.compile("\"([^\"]+)\"").matcher(bloc.group(1));
        Set<String> mots = new LinkedHashSet<>();
        while (mot.find()) {
            mots.add(mot.group(1));
        }
        return mots;
    }
}
