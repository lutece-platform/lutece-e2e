package fr.paris.lutece.e2e.web;

import fr.paris.lutece.e2e.core.BrowserManager;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import java.util.logging.Logger;

/**
 * Bean de demarrage qui initialise Playwright au lancement de l'application.
 * Force l'initialisation eager du BrowserManager.
 */
@Singleton
@Startup
public class PlaywrightStartupBean {

    private static final Logger LOGGER = Logger.getLogger(PlaywrightStartupBean.class.getName());

    @Inject
    private BrowserManager browserManager;

    @PostConstruct
    public void init() {
        LOGGER.info("Initialisation de Playwright au demarrage de l'application...");
        try {
            // Force l'initialisation du BrowserManager en accedant a une methode
            String baseUrl = browserManager.getBaseUrl();
            LOGGER.info("BrowserManager initialise - baseUrl: " + baseUrl);
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'initialisation de Playwright: " + e.getMessage());
            // Log full stack trace to help debugging
            Throwable cause = e;
            while (cause != null) {
                LOGGER.severe("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                for (StackTraceElement ste : cause.getStackTrace()) {
                    LOGGER.severe("    at " + ste.toString());
                }
                cause = cause.getCause();
            }
            // Ne pas faire echouer le demarrage de l'application
        }
    }
}
