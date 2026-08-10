package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.pages.bo.LoginPage;
import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import org.junit.jupiter.api.TestInstance;

/**
 * Base des tests macro (briques) et des suites Java.
 *
 * <p>Fournit la creation d'un {@link FormsContext} frais avec login admin + suffixe unique. Herite du
 * cycle de vie Playwright de {@link BaseTest} : un contexte/page neuf est cree avant chaque {@code @Test}
 * (isolation), et le tracing/screenshot-on-failure Allure reste actif.</p>
 *
 * <p>Chaque brique expose une methode {@code static run(FormsContext ctx, XxxDataSet data)} reutilisable
 * (appelable depuis une suite qui thread le contexte) et un {@code @Test standalone()} qui construit un
 * contexte frais, s'auto-provisionne en appelant les briques amont, puis s'execute.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MacroTest extends BaseTest {

    protected static final String ADMIN_USER = config.getValue("test.admin.username", String.class);
    protected static final String ADMIN_PASS = config.getValue("test.admin.password", String.class);

    /**
     * Cree un contexte macro frais : login admin sur la page courante + suffixe unique par run.
     */
    protected FormsContext newLoggedInContext() {
        login();
        return new FormsContext(page, BASE_URL, newSuffix());
    }

    /** Suffixe unique par run (pour des noms non collisionnants). */
    protected String newSuffix() {
        return Long.toString(System.currentTimeMillis() % 1_000_000L);
    }

    /**
     * Effectue le login admin et neutralise une eventuelle page d'avertissement (ex: expiration de
     * mot de passe) affichee juste apres la connexion.
     */
    protected void login() {
        new LoginPage(page, BASE_URL).navigate().loginAs(ADMIN_USER, ADMIN_PASS);
        page.waitForLoadState();
        dismissOptionalWarning();
    }

    private void dismissOptionalWarning() {
        try {
            var okLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("OK"));
            if (okLink.count() > 0 && okLink.first().isVisible()) {
                okLink.first().click();
                page.waitForLoadState();
            }
        } catch (Exception ignored) {
            // avertissement absent : rien a faire
        }
    }
}
