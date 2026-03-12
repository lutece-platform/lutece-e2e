package fr.paris.lutece.e2e.core;

import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Session navigateur par requete HTTP.
 * Chaque requete obtient son propre contexte et page Playwright, isoles des autres.
 * Le contexte est automatiquement ferme a la fin de la requete par CDI.
 */
@RequestScoped
public class BrowserSession {

    private static final Logger LOG = LogManager.getLogger(BrowserSession.class);

    private static final Path AUTH_STATE_DIR = Paths.get(
            System.getProperty("java.io.tmpdir"), "lutece-e2e");

    @Inject
    BrowserManager browserManager;

    private BrowserContext context;
    private Page page;
    private String sessionId;

    @PostConstruct
    void init() {
        createNewContext();
        LOG.debug("BrowserSession initialisee pour le thread {}", Thread.currentThread().getName());
    }

    @PreDestroy
    void cleanup() {
        LOG.debug("Fermeture BrowserSession pour le thread {}", Thread.currentThread().getName());
        safeCloseContext();
    }

    /**
     * Cree un nouveau contexte de navigation.
     */
    public void createNewContext() {
        safeCloseContext();
        context = browserManager.getBrowser().newContext(new Browser.NewContextOptions()
                .setViewportSize(browserManager.getViewportWidth(), browserManager.getViewportHeight())
                .setLocale(browserManager.getLocale())
                .setIgnoreHTTPSErrors(true));
        page = context.newPage();
        page.setDefaultTimeout(browserManager.getTimeout());
    }

    /**
     * Cree un contexte avec l'etat d'authentification sauvegarde.
     */
    public void createAuthenticatedContext() {
        safeCloseContext();
        context = browserManager.getBrowser().newContext(new Browser.NewContextOptions()
                .setViewportSize(browserManager.getViewportWidth(), browserManager.getViewportHeight())
                .setLocale(browserManager.getLocale())
                .setIgnoreHTTPSErrors(true)
                .setStorageStatePath(getAuthStatePath()));
        page = context.newPage();
        page.setDefaultTimeout(browserManager.getTimeout());
    }

    /**
     * Sauvegarde l'etat d'authentification.
     */
    public void saveAuthState() {
        Path authPath = getAuthStatePath();
        authPath.getParent().toFile().mkdirs();
        context.storageState(new BrowserContext.StorageStateOptions()
                .setPath(authPath));
        LOG.info("Etat d'authentification sauvegarde dans {}", authPath);
    }

    /**
     * Verifie si un etat d'authentification existe.
     */
    public boolean hasAuthState() {
        return getAuthStatePath().toFile().exists();
    }

    /**
     * Retourne le chemin de l'etat d'authentification pour cette session.
     */
    private Path getAuthStatePath() {
        String suffix = (sessionId != null) ? "-" + sessionId : "";
        return AUTH_STATE_DIR.resolve("auth-state" + suffix + ".json");
    }

    /**
     * Definit l'identifiant de session pour isoler l'URL et l'etat d'auth.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * Configure l'URL pour cette session (persiste dans BrowserManager).
     */
    public void setBaseUrl(String url) {
        browserManager.setBaseUrlForSession(sessionId, url);
    }

    /**
     * Prend une capture d'ecran.
     */
    public Path screenshot(String name) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path path = Paths.get(browserManager.getScreenshotsPath(), name + "-" + timestamp + ".png");
        path.getParent().toFile().mkdirs();
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(path)
                .setFullPage(true));
        LOG.debug("Screenshot: {}", path);
        return path;
    }

    /**
     * Navigue vers une URL relative.
     * En cas de TargetClosedError, recree le contexte et retente une fois.
     */
    public void navigate(String relativePath) {
        String url = getBaseUrl() + relativePath;
        LOG.debug("Navigation vers {}", url);
        try {
            page.navigate(url);
            page.waitForLoadState();
        } catch (Exception e) {
            if (isTargetClosed(e)) {
                LOG.warn("Page/contexte ferme, recreation et retry: {}", url);
                createNewContext();
                page.navigate(url);
                page.waitForLoadState();
            } else {
                throw e;
            }
        }
    }

    public Page getPage() {
        return page;
    }

    public String getBaseUrl() {
        return browserManager.getBaseUrlForSession(sessionId);
    }

    public boolean isBaseUrlConfigured() {
        return browserManager.isBaseUrlConfiguredForSession(sessionId);
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

    private void safeCloseContext() {
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                LOG.debug("Contexte deja ferme: {}", e.getMessage());
            }
            context = null;
            page = null;
        }
    }

    private boolean isTargetClosed(Exception e) {
        if (e.getClass().getSimpleName().contains("TargetClosedError")) {
            return true;
        }
        String msg = e.getMessage();
        if (msg != null && (msg.contains("Target closed") || msg.contains("target page")
                || msg.contains("context or browser has been closed"))) {
            return true;
        }
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause.getClass().getSimpleName().contains("TargetClosedError")) {
                return true;
            }
            String causeMsg = cause.getMessage();
            if (causeMsg != null && (causeMsg.contains("Target closed")
                    || causeMsg.contains("context or browser has been closed"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
