package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.containers.LuteceContainer;
import fr.paris.lutece.e2e.pages.bo.AdminMenuPage;
import fr.paris.lutece.e2e.pages.bo.LoginPage;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import fr.paris.lutece.e2e.tests.bo.config.ScreenshotOnFailureExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de connexion BO avec Testcontainers (MariaDB + Lutece).
 */
@Testcontainers
@ExtendWith(ScreenshotOnFailureExtension.class)
@DisplayName("Test de connexion BO avec Testcontainers")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginContainerTest {

    private static final Logger LOGGER = LogManager.getLogger(LoginContainerTest.class);

    // Réseau partagé
    private static final Network NETWORK = Network.newNetwork();

    // Conteneur MariaDB
    @Container
    private static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:10.11")
        .withNetwork(NETWORK)
        .withNetworkAliases("mariadb")
        .withDatabaseName("core")
        .withUsername("lutece")
        .withPassword("lutece");

    // Conteneur Lutece
    private static LuteceContainer lutece;

    // Playwright
    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    private String baseUrl;

    @BeforeAll
    void setup() {
        LOGGER.info("=== Démarrage de l'environnement Testcontainers ===");

        // Démarrer Lutece avec l'image configurée
        String luteceImage = System.getProperty("lutece.image", "rafikyahiaoui/lutece-site-8");
        String contextRoot = System.getProperty("lutece.context.root", "/lutece");
        LOGGER.info("Using Lutece image: {}", luteceImage);

        lutece = new LuteceContainer(luteceImage, contextRoot)
            .withSharedNetwork(NETWORK, "lutece")
            .withMariaDB("mariadb", 3306, "core", "lutece", "lutece");

        lutece.start();
        baseUrl = lutece.getBaseURL();
        LOGGER.info("Lutece démarré sur: {}", baseUrl);

        // Attendre que l'application soit prête
        waitForApplication();

        // Démarrer Playwright
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(true));

        context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(1920, 1080)
            .setLocale("fr-FR")
            .setIgnoreHTTPSErrors(true));
        fr.paris.lutece.e2e.tests.bo.config.BaseTest.blockThirdPartyRequests(context);

        page = context.newPage();
        page.setDefaultTimeout(10000);
    }

    private void waitForApplication() {
        LOGGER.info("Attente de Lutece...");
        for (int i = 0; i < 60; i++) {
            if (lutece.isApplicationReady()) {
                LOGGER.info("Lutece prêt après {} secondes", i * 2);
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Lutece non disponible après 120 secondes");
    }

    @AfterAll
    void teardown() {
        LOGGER.info("=== Arrêt de l'environnement ===");
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        if (lutece != null && lutece.isRunning()) lutece.stop();
    }

    @Test
    @DisplayName("Connexion réussie avec identifiants valides")
    void testLoginSuccess() {
        LOGGER.info("Test de connexion au BO...");

        LoginPage loginPage = new LoginPage(page, baseUrl);
        loginPage.navigate();

        // Screenshot de la page de login
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(java.nio.file.Paths.get("target/screenshots/container-login-page.png")));

        LOGGER.info("Page de login chargée: {}", page.url());

        // Connexion
        AdminMenuPage adminMenu = loginPage.loginAs("admin", "adminadmin");

        // Attendre le chargement
        page.waitForLoadState();

        // Screenshot après connexion
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(java.nio.file.Paths.get("target/screenshots/container-after-login.png")));

        LOGGER.info("Après connexion - URL: {}", page.url());

        // Vérification
        assertTrue(adminMenu.isLoggedIn(), "L'utilisateur devrait être connecté");

        LOGGER.info("Connexion réussie !");
    }
}
