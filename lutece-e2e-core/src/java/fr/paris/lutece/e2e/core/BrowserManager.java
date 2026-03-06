package fr.paris.lutece.e2e.core;

import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gestionnaire singleton du navigateur Playwright.
 * Gere le cycle de vie du navigateur et des contextes.
 */
@ApplicationScoped
public class BrowserManager {

    private static final Logger LOG = LogManager.getLogger(BrowserManager.class);

    // Configure le driver AVANT tout acces a Playwright
    static {
        PlaywrightDriverResolver.configure();
    }

    private String baseUrl;
    private boolean headless;
    private int slowMo;
    private int timeout;
    private int viewportWidth;
    private int viewportHeight;
    private String locale;
    private String screenshotsPath;
    private boolean baseUrlConfigured = false;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private static final Path AUTH_STATE_PATH = Paths.get("target/auth-state.json");

    private void loadConfig() {
        org.eclipse.microprofile.config.Config config =
            org.eclipse.microprofile.config.ConfigProvider.getConfig();

        var configuredUrl = config.getOptionalValue("lutece.base.url", String.class);
        if (configuredUrl.isPresent() && !configuredUrl.get().isEmpty()) {
            baseUrl = configuredUrl.get();
            baseUrlConfigured = true;
        } else {
            baseUrl = null;
            baseUrlConfigured = false;
        }
        headless = config.getOptionalValue("browser.headless", Boolean.class)
            .orElse(true);
        slowMo = config.getOptionalValue("browser.slowmo", Integer.class)
            .orElse(0);
        timeout = config.getOptionalValue("browser.timeout", Integer.class)
            .orElse(30000);
        viewportWidth = config.getOptionalValue("browser.viewport.width", Integer.class)
            .orElse(1920);
        viewportHeight = config.getOptionalValue("browser.viewport.height", Integer.class)
            .orElse(1080);
        locale = config.getOptionalValue("browser.locale", String.class)
            .orElse("fr-FR");
        screenshotsPath = config.getOptionalValue("screenshots.path", String.class)
            .orElse("target/screenshots");
    }

    @PostConstruct
    void init() {
        loadConfig();
        LOG.info("Initialisation BrowserManager - headless={}, baseUrl={}", headless, baseUrl);

        try {
            playwright = Playwright.create();
            LOG.info("Playwright instance created");

            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setSlowMo(headless ? 0 : slowMo));
            LOG.info("Chromium browser launched");

            createNewContext();
            LOG.info("BrowserManager initialization complete");
        } catch (Exception e) {
            LOG.error("Failed to initialize Playwright: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Playwright browser", e);
        }
    }

    @PreDestroy
    void cleanup() {
        LOG.info("Fermeture BrowserManager");
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Cree un nouveau contexte de navigation.
     */
    public void createNewContext() {
        if (context != null) {
            context.close();
        }
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(viewportWidth, viewportHeight)
                .setLocale(locale)
                .setIgnoreHTTPSErrors(true));
        page = context.newPage();
        page.setDefaultTimeout(timeout);
    }

    /**
     * Cree un contexte avec l'etat d'authentification sauvegarde.
     */
    public void createAuthenticatedContext() {
        if (context != null) {
            context.close();
        }
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(viewportWidth, viewportHeight)
                .setLocale(locale)
                .setIgnoreHTTPSErrors(true)
                .setStorageStatePath(AUTH_STATE_PATH));
        page = context.newPage();
        page.setDefaultTimeout(timeout);
    }

    /**
     * Sauvegarde l'etat d'authentification.
     */
    public void saveAuthState() {
        context.storageState(new BrowserContext.StorageStateOptions()
                .setPath(AUTH_STATE_PATH));
        LOG.info("Etat d'authentification sauvegarde dans {}", AUTH_STATE_PATH);
    }

    /**
     * Verifie si un etat d'authentification existe.
     */
    public boolean hasAuthState() {
        return AUTH_STATE_PATH.toFile().exists();
    }

    /**
     * Prend une capture d'ecran.
     */
    public Path screenshot(String name) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path path = Paths.get(screenshotsPath, name + "-" + timestamp + ".png");
        path.getParent().toFile().mkdirs();
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(path)
                .setFullPage(true));
        LOG.debug("Screenshot: {}", path);
        return path;
    }

    /**
     * Navigue vers une URL relative.
     */
    public void navigate(String relativePath) {
        String url = baseUrl + relativePath;
        LOG.debug("Navigation vers {}", url);
        page.navigate(url);
        page.waitForLoadState();
    }

    public Page getPage() {
        return page;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Definit l'URL de base du site Lutece.
     */
    public void setBaseUrl(String url) {
        if (url != null && !url.isEmpty()) {
            this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            this.baseUrlConfigured = true;
            LOG.info("URL de base configuree: {}", this.baseUrl);
        }
    }

    public boolean isBaseUrlConfigured() {
        return baseUrlConfigured && baseUrl != null && !baseUrl.isEmpty();
    }

    public String getCurrentUrl() {
        return page.url();
    }

    public String getPageContent() {
        return page.content();
    }

    public String getPageTitle() {
        return page.title();
    }

    public void waitForLoad() {
        page.waitForLoadState();
    }

    public Object evaluate(String script) {
        return page.evaluate(script);
    }

    public Object evaluate(String script, Object arg) {
        return page.evaluate(script, arg);
    }
}
