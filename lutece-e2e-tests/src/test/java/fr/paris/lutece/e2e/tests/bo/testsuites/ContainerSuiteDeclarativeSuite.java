package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.declaratif.SuiteDeclarativeTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Joue la suite decrite par un fichier, sur une instance demarree en conteneur.
 *
 * <p>Le pendant conteneur de {@link SuiteDeclarativeTest} : l'instance est montee, ses droits
 * ouverts, puis le fichier designe par {@code -Dsuite.fichier} est execute. C'est la forme que
 * prend la pipeline pour un site dont la demarche est decrite plutot que codee.</p>
 *
 * <pre>
 *   mvn test -pl lutece-e2e-tests -Dtest=ContainerSuiteDeclarativeSuite \
 *     -Dsuite.fichier=/chemin/vers/.e2e-suite.yml \
 *     -Dlutece.image=${DOCKER_REGISTRY}/bild/p30/site-integration-forms:8.0.0-SNAPSHOT \
 *     -Dlutece.context.root=/lutece -Dtest.headless=true
 * </pre>
 *
 * <p>Prerequis : Docker ou Podman disponible, et acces au registre d'images.</p>
 */
@Suite
@SuiteDisplayName("Suite declarative sur instance en conteneur")
@SelectClasses({
    ContainerSetup.class,          // 1. Demarre MariaDB puis Lutece, et publie l'URL de base
    RbacConfigurationTest.class,   // 2. Ouvre les droits : une instance neuve les refuse
    SuiteDeclarativeTest.class     // 3. Joue le fichier de suite du site
})
public class ContainerSuiteDeclarativeSuite {
}
