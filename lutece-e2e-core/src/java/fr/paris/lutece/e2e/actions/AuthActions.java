package fr.paris.lutece.e2e.actions;

import fr.paris.lutece.e2e.core.ActionResult;
import fr.paris.lutece.e2e.core.BrowserSession;
import fr.paris.lutece.e2e.pages.AdminMenuPage;
import fr.paris.lutece.e2e.pages.LoginPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Actions d'authentification.
 * Gestion robuste de la session avec vérification réelle de l'état.
 */
@ApplicationScoped
public class AuthActions {

    private static final Logger LOG = LogManager.getLogger(AuthActions.class);

    @Inject
    BrowserSession browser;

    @Inject
    LoginPage loginPage;

    @Inject
    AdminMenuPage adminMenuPage;

    private String currentUser = null;

    /**
     * Connexion a l'interface d'administration.
     * Effectue le login puis vérifie le succès en naviguant vers AdminMenu.
     */
    public ActionResult<String> login(String username, String password) {
        LOG.info("Tentative de connexion avec l'utilisateur: {}", username);

        try {
            // D'abord aller sur la page de login directement
            loginPage.navigateToLogin();
            browser.waitForLoad();

            // Vérifier si déjà connecté (redirigé vers AdminMenu au lieu de login)
            String currentUrl = browser.getCurrentUrl();
            if (currentUrl.contains("AdminMenu")) {
                currentUser = username;
                LOG.info("Déjà connecté - session active");
                return ActionResult.success(username, "Déjà connecté en tant que " + username);
            }

            // Si on n'est pas sur la page de login, y naviguer
            if (!currentUrl.contains("AdminLogin")) {
                loginPage.navigateToLogin();
                browser.waitForLoad();
            }

            // Effectuer la connexion
            LOG.info("Session non active, connexion en cours...");
            loginPage.dismissWarningIfPresent();
            loginPage.fillUsername(username);
            loginPage.fillPassword(password);
            loginPage.clickLogin();
            browser.waitForLoad();

            // Après le login, vérifier où on est
            currentUrl = browser.getCurrentUrl();
            LOG.info("URL après login: {}", currentUrl);

            // Si on est sur AdminMessage avec une erreur
            if (currentUrl.contains("AdminMessage")) {
                String error = loginPage.getErrorMessage();
                if (error != null && !error.isEmpty()) {
                    LOG.warn("Echec de connexion: {}", error);
                    currentUser = null;
                    return ActionResult.failure("Echec de connexion: " + error,
                            safeScreenshot("login-failed"));
                }
            }

            // Si on est toujours sur la page de login, c'est un échec
            if (currentUrl.contains("AdminLogin")) {
                currentUser = null;
                return ActionResult.failure("Echec de connexion - identifiants incorrects",
                        safeScreenshot("login-failed"));
            }

            // Naviguer vers AdminMenu pour vérifier que le login a réussi
            adminMenuPage.goToAdminMenu();
            browser.waitForLoad();

            if (isActuallyLoggedIn()) {
                currentUser = username;
                LOG.info("Connexion réussie pour {}", username);
                return ActionResult.success(username, "Connexion réussie",
                        safeScreenshot("login-success"));
            }

            currentUser = null;
            return ActionResult.failure("Echec de connexion - impossible d'accéder au menu admin",
                    safeScreenshot("login-unexpected"));

        } catch (Exception e) {
            LOG.error("Erreur lors de la connexion", e);
            currentUser = null;

            // Si la page/contexte est fermé, recréer le contexte au lieu de tenter un screenshot
            if (isTargetClosed(e)) {
                LOG.warn("Page ou contexte fermé, recreation du contexte navigateur");
                try {
                    browser.createNewContext();
                } catch (Exception ignored) {
                    LOG.error("Impossible de recreer le contexte", ignored);
                }
                return ActionResult.failure("Erreur de navigation: la page a été fermée. " +
                        "Verifiez que l'URL est accessible et que le certificat SSL est valide. " +
                        "Cause: " + e.getMessage());
            }

            try {
                return ActionResult.failure("Erreur: " + e.getMessage(),
                        browser.screenshot("login-error"));
            } catch (Exception screenshotError) {
                LOG.warn("Impossible de prendre un screenshot: {}", screenshotError.getMessage());
                return ActionResult.failure("Erreur: " + e.getMessage());
            }
        }
    }

    /**
     * Déconnexion.
     */
    public ActionResult<Void> logout() {
        LOG.info("Déconnexion...");

        try {
            // Naviguer vers le menu admin pour s'assurer qu'on est sur la bonne page
            adminMenuPage.goToAdminMenu();

            if (!isActuallyLoggedIn()) {
                currentUser = null;
                return ActionResult.success(null, "Pas de session active");
            }

            // Tenter la déconnexion
            try {
                adminMenuPage.logout();
            } catch (Exception e) {
                LOG.warn("Logout classique échoué, navigation vers logout URL");
                // Fallback: naviguer directement vers l'URL de logout
                browser.navigate("/jsp/admin/DoAdminLogout.jsp");
            }

            browser.waitForLoad();
            currentUser = null;

            LOG.info("Déconnexion réussie");
            return ActionResult.success(null, "Déconnexion réussie");

        } catch (Exception e) {
            LOG.error("Erreur lors de la déconnexion", e);
            currentUser = null;
            // Réinitialiser le contexte en cas d'erreur
            try {
                browser.createNewContext();
            } catch (Exception ignored) {}
            return ActionResult.failure("Erreur: " + e.getMessage());
        }
    }

    /**
     * Vérifie l'état réel de connexion en inspectant l'URL et le contenu de la page.
     */
    private boolean isActuallyLoggedIn() {
        try {
            browser.waitForLoad();
            String url = browser.getCurrentUrl();

            // Pages qui indiquent qu'on n'est PAS connecté
            if (url.contains("AdminLogin") || url.contains("AdminMessage")) {
                LOG.debug("Non connecté - sur page login/message: {}", url);
                return false;
            }

            // Vérifier si on est sur le menu admin (vraiment connecté)
            boolean loggedIn = url.contains("AdminMenu");
            LOG.debug("isActuallyLoggedIn: {} (url={})", loggedIn, url);
            return loggedIn;
        } catch (Exception e) {
            LOG.debug("Erreur lors de la vérification de connexion: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si une session est active.
     * Fait une vérification réelle de l'état.
     */
    public boolean isLoggedIn() {
        try {
            // Naviguer vers le menu admin pour vérifier
            adminMenuPage.goToAdminMenu();
            return isActuallyLoggedIn();
        } catch (Exception e) {
            LOG.debug("Erreur lors de la vérification isLoggedIn: {}", e.getMessage());
            currentUser = null;
            return false;
        }
    }

    /**
     * Retourne l'utilisateur connecté.
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Retourne les informations sur la session courante.
     */
    public ActionResult<String> whoami() {
        try {
            adminMenuPage.goToAdminMenu();
            if (isActuallyLoggedIn()) {
                return ActionResult.success(currentUser,
                        "Connecté en tant que: " + (currentUser != null ? currentUser : "utilisateur") +
                        " sur " + browser.getBaseUrl());
            }
        } catch (Exception e) {
            LOG.debug("Erreur whoami: {}", e.getMessage());
        }
        currentUser = null;
        return ActionResult.success(null, "Pas de session active");
    }

    /**
     * Prend un screenshot de manière sûre, retourne null si impossible.
     */
    private Path safeScreenshot(String name) {
        try {
            return browser.screenshot(name);
        } catch (Exception e) {
            LOG.warn("Impossible de prendre un screenshot '{}': {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si l'exception est due à une page/contexte fermé (TargetClosedError).
     */
    private boolean isTargetClosed(Exception e) {
        String message = e.getMessage();
        if (message != null && (message.contains("Target closed") || message.contains("target page")
                || message.contains("context or browser has been closed"))) {
            return true;
        }
        // Vérifier aussi la chaîne de causes
        Throwable cause = e.getCause();
        while (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null && (causeMsg.contains("Target closed")
                    || causeMsg.contains("target page")
                    || causeMsg.contains("context or browser has been closed"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return e.getClass().getSimpleName().contains("TargetClosedError");
    }

    /**
     * Force une nouvelle session en réinitialisant le contexte du navigateur.
     */
    public ActionResult<Void> resetSession() {
        LOG.info("Réinitialisation de la session...");
        try {
            browser.createNewContext();
            currentUser = null;
            return ActionResult.success(null, "Session réinitialisée");
        } catch (Exception e) {
            LOG.error("Erreur lors de la réinitialisation", e);
            return ActionResult.failure("Erreur: " + e.getMessage());
        }
    }
}
