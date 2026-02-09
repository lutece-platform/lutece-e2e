package fr.paris.lutece.e2e.core;

import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire singleton du navigateur Playwright.
 * Gere le cycle de vie du navigateur et des contextes.
 */
@ApplicationScoped
public class BrowserManager {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserManager.class);

    // Static initialization block to configure Playwright driver BEFORE any Playwright code runs
    static {
        configurePlaywrightDriverStatic();
    }

    /**
     * Static configuration of Playwright driver path.
     * Must run before any Playwright class is loaded.
     */
    private static void configurePlaywrightDriverStatic() {
        // Check environment variable first
        String driverPath = System.getenv("PLAYWRIGHT_DRIVER_PATH");

        // Fall back to system property
        if (driverPath == null || driverPath.isEmpty()) {
            driverPath = System.getProperty("playwright.driver.path");
        }

        // Fall back to hardcoded path for OpenLiberty deployment
        if (driverPath == null || driverPath.isEmpty()) {
            // Try common locations
            String[] possiblePaths = {
                "/home/yahiaoui/lutece/workspace-site/playwright/lutece-e2e/playwright-driver/java/driver/linux/playwright.sh",
                System.getProperty("user.home") + "/.playwright/driver/playwright.sh"
            };
            for (String path : possiblePaths) {
                if (new File(path).exists() && new File(path).canExecute()) {
                    driverPath = path;
                    break;
                }
            }
        }

        if (driverPath != null && new File(driverPath).exists()) {
            System.out.println("[BrowserManager] Configuring Playwright driver: " + driverPath);

            // Set the custom driver implementation class
            // This tells Playwright to use our PreextractedDriver instead of the default DriverJar
            System.setProperty("playwright.driver.impl", "fr.paris.lutece.e2e.core.PreextractedDriver");
            System.out.println("[BrowserManager] Set playwright.driver.impl to PreextractedDriver");

            // Also set environment variables for the custom driver to use
            try {
                setEnvVariableStatic("PLAYWRIGHT_DRIVER_PATH", driverPath);
                System.out.println("[BrowserManager] Set PLAYWRIGHT_DRIVER_PATH env var successfully");

                // Also set node path
                File driverFile = new File(driverPath);
                String nodePath = new File(driverFile.getParent(), "node").getAbsolutePath();
                if (new File(nodePath).exists()) {
                    setEnvVariableStatic("PLAYWRIGHT_NODEJS_PATH", nodePath);
                    System.out.println("[BrowserManager] Configuring Node.js: " + nodePath);
                }

                // Also set as system properties for the custom driver
                System.setProperty("playwright.driver.path", driverPath);
            } catch (Exception e) {
                System.err.println("[BrowserManager] Warning: Could not set environment variables via reflection: " + e.getMessage());
                // The custom driver can still use system property
            }
        } else {
            System.out.println("[BrowserManager] No pre-extracted Playwright driver found at: " + driverPath);
            System.out.println("[BrowserManager] Will attempt to use bundled driver (may fail with wsjar filesystem)");
        }
    }

    @SuppressWarnings("unchecked")
    private static void setEnvVariableStatic(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        Class<?> cl = env.getClass();
        Field field = cl.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writableEnv = (Map<String, String>) field.get(env);
        writableEnv.put(key, value);
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

    // Chargement de la config via MicroProfile Config directement
    private void loadConfig() {
        org.eclipse.microprofile.config.Config config =
            org.eclipse.microprofile.config.ConfigProvider.getConfig();

        // L'URL de base peut être configurée via config ou dynamiquement par l'utilisateur
        var configuredUrl = config.getOptionalValue("lutece.base.url", String.class);
        if (configuredUrl.isPresent() && !configuredUrl.get().isEmpty()) {
            baseUrl = configuredUrl.get();
            baseUrlConfigured = true;
        } else {
            baseUrl = null; // Sera demandé à l'utilisateur
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

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private static final Path AUTH_STATE_PATH = Paths.get("target/auth-state.json");

    @PostConstruct
    void init() {
        loadConfig();
        LOG.info("Initialisation BrowserManager - headless={}, baseUrl={}", headless, baseUrl);

        // Configure Playwright driver path for OpenLiberty compatibility (redundant but logs status)
        configurePlaywrightDriver();

        try {
            LOG.info("Creating Playwright instance...");
            LOG.info("PLAYWRIGHT_DRIVER_PATH env = {}", System.getenv("PLAYWRIGHT_DRIVER_PATH"));
            LOG.info("PLAYWRIGHT_NODEJS_PATH env = {}", System.getenv("PLAYWRIGHT_NODEJS_PATH"));

            playwright = Playwright.create();
            LOG.info("Playwright instance created successfully");

            LOG.info("Launching Chromium browser...");
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setSlowMo(headless ? 0 : slowMo));
            LOG.info("Chromium browser launched successfully");

            createNewContext();
            LOG.info("BrowserManager initialization complete");
        } catch (Exception e) {
            LOG.error("Failed to initialize Playwright: {}", e.getMessage(), e);
            // Log the full stack trace
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Playwright browser", e);
        }
    }

    /**
     * Configure Playwright driver path to use pre-extracted driver.
     * This is necessary for OpenLiberty which uses wsjar filesystem
     * that is incompatible with Playwright's JAR extraction mechanism.
     */
    private void configurePlaywrightDriver() {
        // Check for driver path from config or environment
        org.eclipse.microprofile.config.Config config =
            org.eclipse.microprofile.config.ConfigProvider.getConfig();

        String driverPath = config.getOptionalValue("playwright.driver.path", String.class)
            .orElse(System.getenv("PLAYWRIGHT_DRIVER_PATH"));

        if (driverPath == null) {
            // Default path if not configured
            driverPath = System.getProperty("playwright.driver.path");
        }

        if (driverPath != null) {
            Path path = Paths.get(driverPath);
            if (Files.exists(path) && Files.isExecutable(path)) {
                LOG.info("Using pre-extracted Playwright driver: {}", driverPath);
                // Set environment variable for Playwright using reflection
                setEnvVariable("PLAYWRIGHT_DRIVER_PATH", driverPath);

                // Also set node path if available
                String nodePath = path.getParent().resolve("node").toString();
                if (Files.exists(Paths.get(nodePath))) {
                    setEnvVariable("PLAYWRIGHT_NODEJS_PATH", nodePath);
                    LOG.info("Using Node.js at: {}", nodePath);
                }
            } else {
                LOG.warn("Playwright driver not found or not executable at: {}", driverPath);
            }
        } else {
            LOG.info("No pre-extracted Playwright driver configured, using bundled driver");
        }

        // Log current environment for debugging
        LOG.debug("PLAYWRIGHT_DRIVER_PATH={}", System.getenv("PLAYWRIGHT_DRIVER_PATH"));
        LOG.debug("PLAYWRIGHT_NODEJS_PATH={}", System.getenv("PLAYWRIGHT_NODEJS_PATH"));
    }

    /**
     * Set an environment variable at runtime using reflection.
     * This is a workaround for OpenLiberty not properly passing env vars.
     */
    @SuppressWarnings("unchecked")
    private void setEnvVariable(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
            LOG.debug("Set environment variable {}={}", key, value);
        } catch (Exception e) {
            LOG.warn("Could not set environment variable {} - trying ProcessBuilder approach", key, e);
            // Fallback: set as system property (some Playwright versions check this)
            System.setProperty(key, value);
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

    /**
     * Retourne la page courante.
     */
    public Page getPage() {
        return page;
    }

    /**
     * Retourne l'URL de base.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Définit l'URL de base du site Lutece.
     * Permet une configuration dynamique par l'utilisateur.
     */
    public void setBaseUrl(String url) {
        if (url != null && !url.isEmpty()) {
            // Normaliser l'URL (supprimer le slash final si présent)
            this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            this.baseUrlConfigured = true;
            LOG.info("URL de base configurée: {}", this.baseUrl);
        }
    }

    /**
     * Vérifie si l'URL de base est configurée.
     */
    public boolean isBaseUrlConfigured() {
        return baseUrlConfigured && baseUrl != null && !baseUrl.isEmpty();
    }

    /**
     * Retourne l'URL courante.
     */
    public String getCurrentUrl() {
        return page.url();
    }

    /**
     * Retourne le contenu HTML de la page.
     */
    public String getPageContent() {
        return page.content();
    }

    /**
     * Retourne le titre de la page.
     */
    public String getPageTitle() {
        return page.title();
    }

    /**
     * Attend que la page soit chargee.
     */
    public void waitForLoad() {
        page.waitForLoadState();
    }

    /**
     * Execute du JavaScript sur la page.
     */
    public Object evaluate(String script) {
        return page.evaluate(script);
    }

    /**
     * Execute du JavaScript avec un argument.
     */
    public Object evaluate(String script, Object arg) {
        return page.evaluate(script, arg);
    }
}
