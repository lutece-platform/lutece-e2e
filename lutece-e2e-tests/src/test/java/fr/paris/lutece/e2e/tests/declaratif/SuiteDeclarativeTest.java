package fr.paris.lutece.e2e.tests.declaratif;

import fr.paris.lutece.e2e.tests.macro.Evidence;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Joue la suite decrite par le fichier du site teste.
 *
 * <p>C'est le point d'entree unique des suites declaratives : un seul test, dont le contenu depend
 * du fichier que porte le site. La demarche n'a plus besoin de sa classe Java — elle a son fichier,
 * versionne avec elle, modifiable par qui la connait sans toucher au depot de tests.</p>
 *
 * <p>Le fichier est designe par {@code -Dsuite.fichier}. En CI il est extrait de l'image du site,
 * a cote de son {@code .e2e-config.json} ; en local il suffit de pointer un chemin.</p>
 *
 * <pre>
 *   mvn test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.declaratif.SuiteDeclarativeTest \
 *     -Dsuite.fichier=/chemin/vers/.e2e-suite.yml \
 *     -Dlutece.base.url=https://mon-site/lutece
 * </pre>
 */
@Epic("Suites declaratives")
@Feature("Execution d'une suite decrite par le site")
@Tag("macro")
@Tag("suite")
@DisplayName("Suite declarative : la demarche decrite par le fichier du site")
public class SuiteDeclarativeTest extends MacroTest {

    /** Propriete designant le fichier a jouer. */
    public static final String PROPRIETE_FICHIER = "suite.fichier";

    /** Nom du fichier cherche par defaut a la racine du projet. */
    public static final String FICHIER_PAR_DEFAUT = ".e2e-suite.yml";

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Lit le fichier de suite porte par le site, batit ce qu'il decrit — workflow, "
        + "formulaire, controles — puis joue le parcours qu'il demande : soumissions en front "
        + "office et actions de workflow sur les reponses recues.")
    @DisplayName("Jouer la suite decrite par le fichier du site")
    void jouerLaSuiteDecrite() {
        Path fichier = fichierDeSuite();
        DescriptionDeSuite description = LecteurDeSuite.depuis(fichier);

        Evidence.contexteDExecution(BASE_URL);
        Evidence.texte("Suite jouee", description.nom()
            + (description.description().isBlank() ? "" : " — " + description.description())
            + System.lineSeparator() + "Decrite par : " + fichier);

        login();
        new ExecuteurDeSuite(page, BASE_URL, newSuffix()).executer(description);
    }

    /**
     * Chemin du fichier a jouer.
     *
     * <p>Le message d'absence dit ou le poser plutot que de constater qu'il manque : c'est la
     * premiere chose que rencontre un site qui adopte les suites declaratives.</p>
     *
     * @return le chemin du fichier
     */
    private static Path fichierDeSuite() {
        String declare = System.getProperty(PROPRIETE_FICHIER);
        Path fichier = declare == null || declare.isBlank()
            ? Path.of(FICHIER_PAR_DEFAUT)
            : Path.of(declare);

        Assertions.assertTrue(Files.isReadable(fichier),
            "Aucun fichier de suite a jouer : « " + fichier + " » est introuvable. Le designer avec "
            + "-D" + PROPRIETE_FICHIER + "=<chemin>, ou poser un « " + FICHIER_PAR_DEFAUT + " » a la "
            + "racine. En CI, ce fichier est extrait de l'image du site, a cote de son .e2e-config.json.");
        return fichier;
    }
}
