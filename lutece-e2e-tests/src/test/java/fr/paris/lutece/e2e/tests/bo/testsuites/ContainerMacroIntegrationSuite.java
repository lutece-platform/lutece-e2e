package fr.paris.lutece.e2e.tests.bo.testsuites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite globale : {@link ContainerIntegrationSuite} suivie de {@link MacroTestsSuite}.
 *
 * <p>Enchainement :</p>
 * <ol>
 *   <li>{@link ContainerIntegrationSuite} : {@code ContainerSetup} demarre MariaDB + Lutece via
 *       Testcontainers et publie l'URL dynamique ({@code BaseTest.updateBaseUrl}), puis RBAC,
 *       workflow, formulaire et soumission FO s'executent sur ce conteneur.</li>
 *   <li>{@link MacroTestsSuite} : l'ensemble des briques macro ({@code tests.macro} et ses
 *       sous-packages) s'execute sur la meme instance conteneurisee, chacune avec son propre
 *       contexte Playwright et son propre provisionnement.</li>
 * </ol>
 *
 * <p><b>Pourquoi deux sous-suites et non une liste de classes ?</b> Le moteur JUnit Jupiter est
 * execute avant le moteur {@code junit-platform-suite}. Melanger {@code ContainerIntegrationSuite}
 * (suite) et des classes de test macro (Jupiter) dans un meme {@code @SelectClasses} ferait tourner
 * les tests macro <i>avant</i> le demarrage des conteneurs, donc contre l'URL par defaut. En
 * n'incluant que des suites, l'ordre de declaration ci-dessous est respecte : les conteneurs sont
 * demarres avant les tests macro. Les conteneurs restent actifs jusqu'a la fin de la JVM
 * (shutdown hook de {@code ContainerSetup}), ce qui couvre les deux sous-suites.</p>
 *
 * <p>Usage :</p>
 * <pre>
 *   mvn test -pl lutece-e2e-tests -Dtest=ContainerMacroIntegrationSuite -Dtest.headless=true
 *   mvn test -pl lutece-e2e-tests -Dtest=ContainerMacroIntegrationSuite \
 *     -Dlutece.image=nexus-docker-fastdeploy.api.paris.mdp/bild/p30/site-integration-forms:8.0.0-SNAPSHOT \
 *     -Dlutece.context.root=/lutece -Dtest.headless=true
 * </pre>
 *
 * <p>Prerequis : Docker/Podman disponible, et acces au registre d'images (VPN Ville de Paris pour
 * {@code nexus-docker-fastdeploy.api.paris.mdp}).</p>
 *
 * <p>Parametres : voir {@link ContainerIntegrationSuite} ({@code lutece.image},
 * {@code lutece.context.root}, {@code lutece.db.password}).</p>
 */
@Suite
@SuiteDisplayName("Suite globale Container + tous les tests macro")
@SelectClasses({
    ContainerIntegrationSuite.class,  // 1. Conteneurs Docker + parcours workflow/formulaire/soumission
    MacroTestsSuite.class             // 2. Ensemble des briques macro sur la meme instance
})
public class ContainerMacroIntegrationSuite {
}
