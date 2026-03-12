package fr.paris.lutece.e2e.core;

import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire singleton du processus Chromium.
 * Gere le cycle de vie de Playwright et du navigateur (couteux a creer).
 * Les contextes et pages sont geres par {@link BrowserSession} (un par requete).
 */
@ApplicationScoped
public class BrowserManager {

    private static final Logger LOG = LogManager.getLogger(BrowserManager.class);

    // Configure le driver AVANT tout acces a Playwright
    static {
        PlaywrightDriverResolver.configure();
    }

    private String defaultBaseUrl;
    private boolean headless;
    private int slowMo;
    private int timeout;
    private int viewportWidth;
    private int viewportHeight;
    private String locale;
    private String screenshotsPath;
    private boolean defaultBaseUrlConfigured = false;

    /** URLs par session utilisateur (sessionId -> baseUrl). */
    private final ConcurrentHashMap<String, String> sessionUrls = new ConcurrentHashMap<>();

    private Playwright playwright;
    private Browser browser;

    private void loadConfig() {
        org.eclipse.microprofile.config.Config config =
            org.eclipse.microprofile.config.ConfigProvider.getConfig();

        var configuredUrl = config.getOptionalValue("lutece.base.url", String.class);
        if (configuredUrl.isPresent() && !configuredUrl.get().isEmpty()) {
            defaultBaseUrl = configuredUrl.get();
            defaultBaseUrlConfigured = true;
        } else {
            defaultBaseUrl = null;
            defaultBaseUrlConfigured = false;
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
            .orElse(java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"),
                    "lutece-e2e", "screenshots").toString());
    }

    @PostConstruct
    void init() {
        loadConfig();
        LOG.info("Initialisation BrowserManager - headless={}, defaultBaseUrl={}", headless, defaultBaseUrl);

        try {
            playwright = Playwright.create();
            LOG.info("Playwright instance created");

            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setSlowMo(headless ? 0 : slowMo));
            LOG.info("Chromium browser launched");
        } catch (Exception e) {
            LOG.error("Failed to initialize Playwright: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Playwright browser", e);
        }
    }

    @PreDestroy
    void cleanup() {
        LOG.info("Fermeture BrowserManager");
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
    }

    /**
     * Retourne le browser Chromium. Relance si necessaire.
     */
    public synchronized Browser getBrowser() {
        ensureBrowserAlive();
        return browser;
    }

    /**
     * Retourne l'URL par defaut (depuis la config MicroProfile).
     */
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * Retourne l'URL pour une session donnee, ou l'URL par defaut si aucune surcharge.
     */
    public String getBaseUrlForSession(String sessionId) {
        if (sessionId != null) {
            String sessionUrl = sessionUrls.get(sessionId);
            if (sessionUrl != null) {
                return sessionUrl;
            }
        }
        return defaultBaseUrl;
    }

    /**
     * Configure l'URL pour une session specifique (n'affecte pas les autres sessions).
     */
    public void setBaseUrlForSession(String sessionId, String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        String cleanUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        if (sessionId != null) {
            sessionUrls.put(sessionId, cleanUrl);
            LOG.info("URL configuree pour session {}: {}", sessionId, cleanUrl);
        } else {
            this.defaultBaseUrl = cleanUrl;
            this.defaultBaseUrlConfigured = true;
            LOG.info("URL par defaut configuree: {}", cleanUrl);
        }
    }

    /**
     * Verifie si l'URL est configuree pour une session donnee.
     */
    public boolean isBaseUrlConfiguredForSession(String sessionId) {
        if (sessionId != null && sessionUrls.containsKey(sessionId)) {
            return true;
        }
        return defaultBaseUrlConfigured && defaultBaseUrl != null && !defaultBaseUrl.isEmpty();
    }

    /**
     * Supprime l'URL d'une session (nettoyage).
     */
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessionUrls.remove(sessionId);
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public String getLocale() {
        return locale;
    }

    public String getScreenshotsPath() {
        return screenshotsPath;
    }

    public boolean isHeadless() {
        return headless;
    }

    /**
     * Verifie que le browser est vivant, le relance si necessaire.
     */
    private void ensureBrowserAlive() {
        boolean needsRelaunch = (browser == null);
        if (!needsRelaunch) {
            try {
                browser.contexts();
            } catch (Exception e) {
                LOG.warn("Browser mort, relance: {}", e.getMessage());
                needsRelaunch = true;
            }
        }
        if (needsRelaunch) {
            try { if (browser != null) browser.close(); } catch (Exception ignored) {}
            try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}

            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setSlowMo(headless ? 0 : slowMo));
            LOG.info("Browser relance avec succes");
        }
    }
}
