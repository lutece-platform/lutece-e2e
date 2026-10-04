package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.suites.FormsDeclarationInteretsSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Declaration d'interets simplifiee, executee sur une instance demarree en conteneur.
 *
 * <p>La suite s'appuie sur une demande de correction de saisie, qui batit pour le declarant une
 * adresse de retour vers le front office. Cette adresse est lue dans la configuration du site,
 * sous le profil actif : l'eprouver en conteneur verifie que l'instance de test est configuree
 * pour cela, ce qu'une execution sur un site deja en service ne dit pas.</p>
 *
 * <pre>
 *   mvn test -pl lutece-e2e-tests -Dtest=ContainerFormsDeclarationInteretsSuite \
 *     -Dlutece.image=${DOCKER_REGISTRY}/bild/p30/site-integration-forms:8.0.0-SNAPSHOT \
 *     -Dlutece.context.root=/lutece -Dtest.headless=true
 * </pre>
 *
 * <p>Une instance fraichement demarree refuse l'acces aux fonctions du plugin Forms tant que les
 * droits n'ont pas ete ouverts : la configuration RBAC precede donc la suite.</p>
 *
 * <p>Prerequis : Docker ou Podman disponible, et acces au registre d'images.</p>
 */
@Suite
@SuiteDisplayName("Declaration d'interets sur instance en conteneur")
@SelectClasses({
    ContainerSetup.class,                   // 1. Demarre MariaDB puis Lutece, et publie l'URL de base
    RbacConfigurationTest.class,            // 2. Ouvre les droits : une instance neuve les refuse
    FormsDeclarationInteretsSuite.class     // 3. Formulaire, workflow, soumissions et instruction
})
public class ContainerFormsDeclarationInteretsSuite {
}
