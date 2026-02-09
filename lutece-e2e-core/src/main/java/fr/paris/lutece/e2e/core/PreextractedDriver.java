package fr.paris.lutece.e2e.core;

import com.microsoft.playwright.impl.driver.Driver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Custom Playwright Driver implementation that uses a pre-extracted driver.
 * This bypasses the JAR extraction mechanism which fails on OpenLiberty
 * due to the wsjar filesystem incompatibility.
 */
public class PreextractedDriver extends Driver {

    private static final String[] DRIVER_PATHS = {
        System.getenv("PLAYWRIGHT_DRIVER_PATH"),
        System.getProperty("playwright.driver.path"),
        "/home/yahiaoui/lutece/workspace-site/playwright/lutece-e2e/playwright-driver/java/driver/linux/playwright.sh"
    };

    private final Path driverDir;

    public PreextractedDriver() {
        Path driverPath = findDriverPath();
        if (driverPath == null) {
            throw new RuntimeException(
                "Playwright driver not found. Please set PLAYWRIGHT_DRIVER_PATH environment variable " +
                "or playwright.driver.path system property to point to the playwright.sh script."
            );
        }
        // driverDir is the parent directory of playwright.sh
        this.driverDir = driverPath.getParent();
        System.out.println("[PreextractedDriver] Using driver directory: " + driverDir);
    }

    private static Path findDriverPath() {
        for (String pathStr : DRIVER_PATHS) {
            if (pathStr != null && !pathStr.isEmpty()) {
                Path path = Paths.get(pathStr).toAbsolutePath();
                if (Files.exists(path) && Files.isExecutable(path)) {
                    return path;
                }
            }
        }
        return null;
    }

    @Override
    protected Path driverDir() {
        return driverDir;
    }

    @Override
    protected void initialize(Boolean installBrowsers) {
        // No initialization needed - driver is pre-extracted
        System.out.println("[PreextractedDriver] Skipping initialization - using pre-extracted driver");
    }
}
