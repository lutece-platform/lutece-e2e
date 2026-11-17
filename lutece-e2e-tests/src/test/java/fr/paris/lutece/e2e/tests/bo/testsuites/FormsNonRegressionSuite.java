package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.suites.FormsNonRegBackOfficeEntrySuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegCopySuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegCrossStepConditionSuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegCrossStepControlSuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegDynamicFOSuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegFrontOfficeSuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegNegativeSuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegResponsesSuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegStructureSuite;
import fr.paris.lutece.e2e.tests.suites.FormsNonRegVolumeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Non-regression complete du plugin forms : les dix suites, chacune independante (elle construit et supprime
 * ses donnees), dans l'ordre structure, copie, front-office, front-office dynamique, saisie BO, volume, reponses, refus,
 * condition d'affichage entre deux etapes, controle sur la question conditionnelle.
 *
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.FormsNonRegressionSuite \
 *     -Dlutece.base.url=http://localhost:18084/lutece -Dtest.headless=true
 * </pre>
 *
 * <p>Inventaire, constats et briques : {@code docs/TESTS-MACRO-FORMS-NONREG.md}.</p>
 */
@Suite
@SuiteDisplayName("Non-regression Forms")
@SelectClasses({
    FormsNonRegStructureSuite.class,
    FormsNonRegCopySuite.class,
    FormsNonRegFrontOfficeSuite.class,
    FormsNonRegDynamicFOSuite.class,
    FormsNonRegBackOfficeEntrySuite.class,
    FormsNonRegVolumeSuite.class,
    FormsNonRegResponsesSuite.class,
    FormsNonRegNegativeSuite.class,
    FormsNonRegCrossStepConditionSuite.class,
    FormsNonRegCrossStepControlSuite.class
})
public class FormsNonRegressionSuite {}
