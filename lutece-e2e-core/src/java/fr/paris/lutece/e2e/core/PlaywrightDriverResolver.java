package fr.paris.lutece.e2e.core;

import org.apache.logging.log4j.status.StatusLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Resout le chemin du driver Playwright pre-extrait.
 * <p>
 * Ordre de resolution :
 * <ol>
 *   <li>Variable d'environnement {@code PLAYWRIGHT_DRIVER_PATH}</li>
 *   <li>System property {@code playwright.driver.path}</li>
 *   <li>Chemin par defaut {@code $HOME/.playwright/driver/node} (1.58+)
 *       ou {@code $HOME/.playwright/driver/playwright.sh} (legacy)</li>
 * </ol>
 * <p>
 * Compatible Playwright 1.41 (playwright.sh) et 1.58+ (node + package/cli.js).
 * Utilise uniquement des system properties pour communiquer le chemin
 * a Playwright (pas de hack reflection sur {@code System.getenv()}).
 */
public final class PlaywrightDriverResolver {

    private static final StatusLogger LOG = StatusLogger.getLogger();

    private static final String ENV_DRIVER_PATH = "PLAYWRIGHT_DRIVER_PATH";
    private static final String PROP_DRIVER_PATH = "playwright.driver.path";
    private static final String PROP_DRIVER_IMPL = "playwright.driver.impl";
    private static final String DEFAULT_DRIVER_DIR = ".playwright/driver";

    // Playwright 1.58+ utilise node + package/cli.js (pas de playwright.sh)
    private static final String DRIVER_NODE = "node";
    // Playwright <= 1.51 utilisait playwright.sh
    private static final String DRIVER_SCRIPT_LEGACY = "playwright.sh";

    private PlaywrightDriverResolver() {
    }

    /**
     * Configure le driver Playwright via system properties.
     * Doit etre appele avant tout acces a {@code Playwright.create()}.
     */
    public static void configure() {
        Optional<Path> resolved = resolveDriverPath();

        if (resolved.isEmpty()) {
            LOG.info("No pre-extracted Playwright driver found, will use bundled driver");
            return;
        }

        Path driverExecutable = resolved.get();
        LOG.info("Using pre-extracted Playwright driver: {}", driverExecutable);

        System.setProperty(PROP_DRIVER_IMPL, PreextractedDriver.class.getName());
        System.setProperty(PROP_DRIVER_PATH, driverExecutable.toString());
    }

    /**
     * Resout le chemin du driver selon l'ordre de priorite.
     * Cherche d'abord 'node' (1.58+), puis 'playwright.sh' (legacy).
     */
    static Optional<Path> resolveDriverPath() {
        // 1. Variable d'environnement
        String envValue = System.getenv(ENV_DRIVER_PATH);
        if (envValue != null && !envValue.isEmpty()) {
            Path path = Paths.get(envValue).toAbsolutePath();
            if (isValidDriver(path)) {
                LOG.info("Driver resolved from env {}: {}", ENV_DRIVER_PATH, path);
                return Optional.of(path);
            }
            LOG.warn("Env {} points to invalid driver: {}", ENV_DRIVER_PATH, path);
        }

        // 2. System property
        String propValue = System.getProperty(PROP_DRIVER_PATH);
        if (propValue != null && !propValue.isEmpty()) {
            Path path = Paths.get(propValue).toAbsolutePath();
            if (isValidDriver(path)) {
                LOG.info("Driver resolved from property {}: {}", PROP_DRIVER_PATH, path);
                return Optional.of(path);
            }
            LOG.warn("Property {} points to invalid driver: {}", PROP_DRIVER_PATH, path);
        }

        // 3. Chemin par defaut $HOME/.playwright/driver/
        Path defaultDir = Paths.get(System.getProperty("user.home"), DEFAULT_DRIVER_DIR);

        // 3a. Playwright 1.58+ : chercher 'node'
        Path nodePath = defaultDir.resolve(DRIVER_NODE);
        if (isValidDriver(nodePath)) {
            LOG.info("Driver resolved from default location (1.58+): {}", nodePath);
            return Optional.of(nodePath);
        }

        // 3b. Legacy : chercher 'playwright.sh'
        Path legacyPath = defaultDir.resolve(DRIVER_SCRIPT_LEGACY);
        if (isValidDriver(legacyPath)) {
            LOG.info("Driver resolved from default location (legacy): {}", legacyPath);
            return Optional.of(legacyPath);
        }

        return Optional.empty();
    }

    /**
     * Retourne le chemin du driver deja resolu (apres appel a {@link #configure()}).
     */
    public static Optional<Path> getConfiguredDriverPath() {
        String value = System.getProperty(PROP_DRIVER_PATH);
        if (value != null && !value.isEmpty()) {
            return Optional.of(Paths.get(value));
        }
        return Optional.empty();
    }

    private static boolean isValidDriver(Path path) {
        return Files.exists(path) && Files.isExecutable(path);
    }
}
