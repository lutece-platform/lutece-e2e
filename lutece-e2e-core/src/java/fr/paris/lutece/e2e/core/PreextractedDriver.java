package fr.paris.lutece.e2e.core;

import com.microsoft.playwright.impl.driver.Driver;
import org.apache.logging.log4j.status.StatusLogger;

import java.nio.file.Path;

/**
 * Implementation custom du Driver Playwright utilisant un driver pre-extrait.
 * Contourne l'incompatibilite du filesystem wsjar d'OpenLiberty
 * avec le mecanisme d'extraction du driver depuis le JAR.
 */
public class PreextractedDriver extends Driver {

    private static final StatusLogger LOG = StatusLogger.getLogger();

    private final Path driverDir;

    public PreextractedDriver() {
        Path driverPath = PlaywrightDriverResolver.getConfiguredDriverPath()
                .orElseThrow(() -> new IllegalStateException(
                        "Playwright driver not found. Set PLAYWRIGHT_DRIVER_PATH env var " +
                        "or playwright.driver.path system property."));
        this.driverDir = driverPath.getParent();
        LOG.info("Using pre-extracted driver directory: {}", driverDir);
    }

    @Override
    protected Path driverDir() {
        return driverDir;
    }

    @Override
    protected void initialize(Boolean installBrowsers) {
        // Driver pre-extrait, pas d'initialisation necessaire
    }
}
