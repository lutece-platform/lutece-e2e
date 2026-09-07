package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.bo.testsuites.ContainerSetup;
import fr.paris.lutece.e2e.tests.bo.testsuites.RbacConfigurationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Harnais conteneur de {@link OrganisationServicesSuite} : demarre MariaDB + Lutece, ouvre les droits
 * de l'admin (une image fraiche n'accorde pas UNIT_MANAGEMENT), puis joue le scenario.
 *
 * <pre>
 *   mvn -o clean test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.OrganisationServicesContainerHarness \
 *     -Dlutece.image=nexus-docker-fastdeploy.api.paris.mdp/bild/f98/site-deontologie:1.0.3-SNAPSHOT \
 *     -Dtest.headless=true
 * </pre>
 */
@Suite
@SelectClasses({
    ContainerSetup.class,
    RbacConfigurationTest.class,
    OrganisationServicesSuite.class
})
public class OrganisationServicesContainerHarness {
}
