package fr.paris.lutece.e2e.tests.bo.testsuites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite d'intégration avec Testcontainers.
 * Réutilise les mêmes classes de test que WorkflowFormsIntegrationSuite
 * mais dans un environnement Docker isolé.
 *
 * ContainerSetup démarre les conteneurs et configure l'URL avant les autres tests.
 * L'image Open Liberty expose le port 9090, Testcontainers mappe vers un port aléatoire.
 *
 * Usage:
 *   mvn test -Dtest=ContainerIntegrationSuite
 *   mvn test -Dtest=ContainerIntegrationSuite -Dlutece.image=mon-image:tag -Dlutece.context.root=/mon-context
 *
 * Paramètres:
 *   -Dlutece.image        : Image Docker Lutece Open Liberty (défaut: nexus-docker.../site-deontologie:1.0.0-SNAPSHOT)
 *   -Dlutece.context.root : Context root de l'application (défaut: /site-deontologie)
 *   -Dlutece.db.password  : Mot de passe de la base de données (défaut: lutece)
 */
@Suite
@SuiteDisplayName("Suite Container integration Workflow + Formulaires + Soumission")
@SelectClasses({
    ContainerSetup.class,           // 1. Démarre les conteneurs Docker
    RbacConfigurationTest.class,    // 2. Configure RBAC
    WorkflowCreationTest.class,     // 3. Crée le workflow
    FormsCreationTest.class,        // 4. Crée le formulaire
    FormsSubmissionTest.class       // 5. Soumet le formulaire
})
public class ContainerIntegrationSuite {
}
