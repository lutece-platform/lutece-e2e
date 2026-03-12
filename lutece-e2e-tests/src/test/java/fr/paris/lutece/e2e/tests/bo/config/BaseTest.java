package fr.paris.lutece.e2e.tests.bo.config;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Proxy;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Classe de base pour tous les tests Playwright.
 * Gère le cycle de vie du navigateur et des contextes.
 * Utilise MicroProfile Config pour la gestion des configurations.
 *
 * Fonctionnalités:
 * - Playwright Tracing: chaque test est tracé, la trace est sauvegardée uniquement en cas d'échec
 * - Allure Report: screenshots et traces sont attachés automatiquement aux rapports
 *
 * Note: les tests PER_CLASS qui gèrent leur propre contexte (override createContextAndPage)
 * doivent appeler startTracing() après avoir créé le contexte pour bénéficier du tracing.
 */
@ExtendWith(ScreenshotOnFailureExtension.class)
public abstract class BaseTest {

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    // Flag pour savoir si le tracing a ete demarre sur le contexte courant
    private boolean tracingStarted = false;

    // Configuration MicroProfile
    protected static final Config config = ConfigProvider.getConfig();

    // BASE_URL - peut être mis à jour par ContainerSetup via updateBaseUrl()
    // Utilise getOptionalValue pour eviter ExceptionInInitializerError en mode conteneur
    // (ContainerSetup definit l'URL dynamiquement avant les autres tests)
    protected static String BASE_URL = config.getOptionalValue("lutece.base.url", String.class)
        .orElse("http://localhost:9080/lutece");

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

    private static final Path AUTH_STATE_PATH =
        Paths.get("target/auth-state.json");

    private static final Path TRACES_DIR = Paths.get("target/traces");

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
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(HEADLESS)
            .setSlowMo(HEADLESS ? 0 : SLOW_MO);

        // Configurer le proxy Chromium uniquement pour les URLs internes *.mdp
        // En mode conteneur (Testcontainers), BASE_URL est localhost => pas de proxy
        // En mode externe sur le reseau interne (*.mdp), le proxy est necessaire
        if (BASE_URL.contains(".mdp")) {
            String proxyServer = getEnvOrNull("HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy");
            if (proxyServer != null) {
                Proxy proxy = new Proxy(proxyServer);
                String bypass = getEnvOrNull("NO_PROXY", "no_proxy");
                if (bypass != null) {
                    proxy.setBypass(bypass);
                }
                launchOptions.setProxy(proxy);
            }
        }

        browser = playwright.chromium().launch(launchOptions);
    }

    /**
     * Retourne la premiere variable d'environnement non vide parmi les noms donnes, ou null.
     */
    private static String getEnvOrNull(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    @BeforeEach
    protected void createContextAndPage() {
        context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            .setLocale(LOCALE)
            .setIgnoreHTTPSErrors(true));

        startTracing();

        page = context.newPage();
        page.setDefaultTimeout(TIMEOUT);
    }

    /**
     * Demarre le tracing Playwright sur le contexte courant.
     * Appele automatiquement par createContextAndPage().
     * Les tests PER_CLASS qui gerent leur propre contexte doivent appeler
     * cette methode apres avoir cree le contexte.
     */
    protected void startTracing() {
        if (context != null && !tracingStarted) {
            context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(false));
            tracingStarted = true;
        }
    }

    @AfterEach
    protected void closeContext() {
        if (context != null) {
            stopTracing();
            context.close();
        }
    }

    /**
     * Arrete le tracing proprement. Ne fait rien si le tracing n'a pas ete demarre.
     */
    private void stopTracing() {
        if (tracingStarted) {
            try {
                context.tracing().stop();
            } catch (Exception e) {
                // Ignorer si le tracing est deja arrete (ex: sauvegarde par l'extension)
            }
            tracingStarted = false;
        }
    }

    /**
     * Sauvegarde la trace Playwright pour le test en cours.
     * Appelee par ScreenshotOnFailureExtension en cas d'echec.
     */
    protected Path saveTrace(String testName) {
        if (!tracingStarted) {
            return null;
        }
        try {
            java.nio.file.Files.createDirectories(TRACES_DIR);
            Path tracePath = TRACES_DIR.resolve(testName + ".zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
            tracingStarted = false;
            return tracePath;
        } catch (Exception e) {
            System.err.println("[WARN] Failed to save trace for test '" + testName + "': " + e.getMessage());
            return null;
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
            .setPath(Paths.get(SCREENSHOTS_PATH + "/" + name + ".png"))
            .setFullPage(true));
    }
}
