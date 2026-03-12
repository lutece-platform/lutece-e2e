package fr.paris.lutece.plugins.e2eagent.web;

import fr.paris.lutece.e2e.core.BrowserManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bean de demarrage qui initialise Playwright au lancement de l'application.
 * Force l'initialisation eager du BrowserManager.
 */
@ApplicationScoped
public class PlaywrightStartupBean {

    private static final Logger LOGGER = LogManager.getLogger(PlaywrightStartupBean.class);

    @Inject
    private BrowserManager browserManager;

    public void onApplicationStart(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOGGER.info("Initialisation de Playwright au demarrage...");
        try {
            String baseUrl = browserManager.getDefaultBaseUrl();
            LOGGER.info("BrowserManager initialise - defaultBaseUrl: {}", baseUrl);
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'initialisation de Playwright", e);
        }
    }
}
