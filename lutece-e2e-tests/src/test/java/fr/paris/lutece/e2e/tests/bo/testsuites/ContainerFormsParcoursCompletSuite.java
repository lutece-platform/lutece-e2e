package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.suites.FormsParcoursCompletSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Parcours metier complet du plugin Forms, execute sur une instance demarree en conteneur.
 *
 * <p>Le parcours s'appuie sur des traitements qui batissent une adresse de retour vers le front
 * office — les demandes de correction et de complement adressees a l'usager. Cette adresse est
 * lue dans la configuration du site, sous le profil actif : l'eprouver en conteneur verifie que
 * l'instance de test est configuree pour cela, ce qu'une execution sur un site deja en service ne
 * dit pas.</p>
 *
 * <pre>
 *   mvn test -pl lutece-e2e-tests -Dtest=ContainerFormsParcoursCompletSuite \
 *     -Dlutece.image=${DOCKER_REGISTRY}/bild/p30/site-integration-forms:8.0.0-SNAPSHOT \
 *     -Dlutece.context.root=/lutece -Dtest.headless=true
 * </pre>
 *
 * <p>Une instance fraichement demarree refuse l'acces aux fonctions du plugin Forms tant que les
 * droits n'ont pas ete ouverts : la configuration RBAC precede donc le parcours.</p>
 *
 * <p>Prerequis : Docker ou Podman disponible, et acces au registre d'images.</p>
 */
@Suite
@SuiteDisplayName("Parcours complet Forms sur instance en conteneur")
@SelectClasses({
    ContainerSetup.class,          // 1. Demarre MariaDB puis Lutece, et publie l'URL de base
    RbacConfigurationTest.class,   // 2. Ouvre les droits : une instance neuve les refuse
    FormsParcoursCompletSuite.class     // 3. Parcours complet sur cette instance
})
public class ContainerFormsParcoursCompletSuite {
}
