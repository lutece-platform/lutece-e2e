package fr.paris.lutece.e2e.tests.bo.config;

import com.microsoft.playwright.*;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.*;

/**
 * Classe de base pour tous les tests Playwright.
 * Gère le cycle de vie du navigateur et des contextes.
 * Utilise MicroProfile Config pour la gestion des configurations.
 */
public abstract class BaseTest {

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    // Configuration MicroProfile
    protected static final Config config = ConfigProvider.getConfig();

    // BASE_URL - peut être mis à jour par ContainerSetup
    protected static String BASE_URL = config.getValue("lutece.base.url", String.class);

    /**
     * Met à jour l'URL de base. Appelé par ContainerSetup pour les tests en conteneur.
     */
    public static void updateBaseUrl(String url) {
        BASE_URL = url;
    }
    protected static final boolean HEADLESS = config.getValue("test.headless", Boolean.class);
    protected static final int TIMEOUT = config.getValue("test.timeout", Integer.class);
    protected static final int SLOW_MO = config.getValue("test.slowmo", Integer.class);
    protected static final int VIEWPORT_WIDTH = config.getValue("test.viewport.width", Integer.class);
    protected static final int VIEWPORT_HEIGHT = config.getValue("test.viewport.height", Integer.class);
    protected static final String LOCALE = config.getValue("test.locale", String.class);
    protected static final String SCREENSHOTS_PATH = config.getValue("test.screenshots.path", String.class);

    private static final java.nio.file.Path AUTH_STATE_PATH =
        java.nio.file.Paths.get("target/auth-state.json");

    /**
     * Sauvegarde l'etat d'authentification (cookies, localStorage) apres login.
     * A appeler apres un login reussi dans la premiere classe de test.
     */
    protected void saveAuthState() {
        context.storageState(new BrowserContext.StorageStateOptions()
            .setPath(AUTH_STATE_PATH));
    }

    /**
     * Cree un contexte avec l'etat d'authentification sauvegarde.
     * Evite de refaire le login UI.
     */
    protected BrowserContext createAuthenticatedContext() {
        return browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            .setLocale(LOCALE)
            .setIgnoreHTTPSErrors(true)
            .setStorageStatePath(AUTH_STATE_PATH));
    }

    /**
     * Verifie si un etat d'authentification sauvegarde existe.
     */
    protected static boolean hasAuthState() {
        return java.nio.file.Files.exists(AUTH_STATE_PATH);
    }

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(HEADLESS)
            .setSlowMo(HEADLESS ? 0 : SLOW_MO));
    }

    @BeforeEach
    protected void createContextAndPage() {
        context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            .setLocale(LOCALE)
            .setIgnoreHTTPSErrors(true));

        page = context.newPage();
        page.setDefaultTimeout(TIMEOUT);
    }

    @AfterEach
    protected void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Prend une capture d'écran en cas d'échec.
     */
    protected void takeScreenshot(String name) {
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(java.nio.file.Paths.get(SCREENSHOTS_PATH + "/" + name + ".png"))
            .setFullPage(true));
    }
}
