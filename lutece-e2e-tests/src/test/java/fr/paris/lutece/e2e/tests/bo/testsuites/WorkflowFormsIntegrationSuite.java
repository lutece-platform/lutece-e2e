package fr.paris.lutece.e2e.tests.bo.testsuites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite d'integration qui execute les tests dans l'ordre :
 * 1. Creation et activation du workflow
 * 2. Creation et configuration du formulaire
 * 3. Soumission FO et validation BO
 *
 * Execution : mvn test -Dtest=WorkflowFormsIntegrationSuite
 */
@Suite
@SuiteDisplayName("Suite integration Workflow + Formulaires + Soumission")
@SelectClasses({
    RbacConfigurationTest.class,
    WorkflowCreationTest.class,
    FormsCreationTest.class,
    FormsSubmissionTest.class
})
public class WorkflowFormsIntegrationSuite {
}
