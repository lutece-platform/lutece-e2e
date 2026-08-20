package fr.paris.lutece.e2e.tests.bo.testsuites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite de l'ensemble des tests macro (briques).
 *
 * <p>Selectionne recursivement tout le package {@code fr.paris.lutece.e2e.tests.macro}, donc les
 * briques {@code forms}, {@code workflow} et {@code unittree}. Chaque brique s'execute ici en mode
 * {@code standalone()} : contexte Playwright neuf, login admin, auto-provisionnement via les briques
 * amont (voir {@code MacroTest}). Les tests sont donc totalement independants les uns des autres.</p>
 *
 * <p>Le scan par package est volontaire : toute nouvelle brique ajoutee sous {@code tests.macro} est
 * automatiquement incluse, sans modification de cette suite.</p>
 *
 * <p>Cette suite ne demarre aucun conteneur : elle cible l'instance designee par
 * {@code lutece.base.url} (defaut {@code http://localhost:9080/lutece}). Pour l'executer dans un
 * environnement Docker isole, utiliser {@link ContainerMacroIntegrationSuite}.</p>
 *
 * <p>Usage :</p>
 * <pre>
 *   mvn test -pl lutece-e2e-tests -Dtest=MacroTestsSuite
 *   mvn test -pl lutece-e2e-tests -Dtest=MacroTestsSuite -Dlutece.base.url=http://localhost:9080/lutece-site
 * </pre>
 */
@Suite
@SuiteDisplayName("Suite complete des tests macro (briques forms + workflow + unittree)")
@SelectPackages("fr.paris.lutece.e2e.tests.macro")
public class MacroTestsSuite {
}
