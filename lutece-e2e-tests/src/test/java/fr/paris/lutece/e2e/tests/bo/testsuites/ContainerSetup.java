package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import fr.paris.lutece.e2e.tests.bo.containers.LuteceContainer;
import org.junit.jupiter.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classe de configuration qui démarre les conteneurs Docker avant les tests.
 * Doit être exécutée en premier dans la suite ContainerIntegrationSuite.
 */
@DisplayName("Setup Conteneurs Docker")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContainerSetup {

    private static final Logger LOGGER = LogManager.getLogger(ContainerSetup.class);

    // Conteneurs partagés (statiques pour être accessibles par d'autres tests si nécessaire)
    private static Network network;
    private static MariaDBContainer<?> mariadb;
    private static LuteceContainer lutece;
    private static boolean containersStarted = false;

    @BeforeAll
    void startContainers() {
        // Ne démarrer qu'une seule fois
        if (containersStarted) {
            LOGGER.info("Conteneurs déjà démarrés");
            return;
        }

        LOGGER.info("=== Démarrage de l'environnement Testcontainers ===");

        // Créer le réseau partagé
        network = Network.newNetwork();

        // Démarrer MariaDB
        LOGGER.info("Démarrage de MariaDB...");
        mariadb = new MariaDBContainer<>("mariadb:10.11")
            .withNetwork(network)
            .withNetworkAliases("mariadb")
            .withDatabaseName("core")
            .withUsername("lutece")
            .withPassword("lutece");
        mariadb.start();
        LOGGER.info("MariaDB démarré sur: {}:{}", mariadb.getHost(), mariadb.getMappedPort(3306));

        // Récupérer l'IP du conteneur MariaDB dans le réseau partagé
        // Bypass la résolution DNS (aardvark-dns) qui peut échouer sous Podman
        String mariaDbIp = mariadb.getContainerInfo()
            .getNetworkSettings().getNetworks()
            .values().iterator().next().getIpAddress();
        LOGGER.info("MariaDB IP dans le réseau partagé: {}", mariaDbIp);

        // Récupérer les paramètres de l'image
        String luteceImage = System.getProperty("lutece.image", "nexus-docker-fastdeploy.api.paris.mdp/bild/f98/site-deontologie:1.0.0-SNAPSHOT");
        String contextRoot = System.getProperty("lutece.context.root", "/lutece");
        String dbPassword = System.getProperty("lutece.db.password", "lutece");

        LOGGER.info("Démarrage de Lutece - Image: {}, Context: {}", luteceImage, contextRoot);

        // Démarrer Lutece (port 9090 dans le conteneur, port aléatoire sur l'hôte)
        // Utilise l'IP directe au lieu de l'alias DNS pour compatibilité Podman
        lutece = new LuteceContainer(luteceImage, contextRoot)
            .withSharedNetwork(network, "lutece")
            .withMariaDB(mariaDbIp, 3306, "core", "lutece", dbPassword);

        lutece.start();

        // Configurer l'URL de base pour les tests AVANT le health check
        // pour que les tests suivants utilisent l'URL du conteneur même si le health check échoue
        String baseUrl = lutece.getBaseURL();
        System.setProperty("lutece.base.url", baseUrl);
        BaseTest.updateBaseUrl(baseUrl);

        // Attendre que Lutece soit prêt
        waitForApplication();

        containersStarted = true;
        LOGGER.info("=== Environnement prêt - URL: {} ===", baseUrl);

        // Ajouter shutdown hook pour nettoyage
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopContainersStatic));
    }

    private void waitForApplication() {
        LOGGER.info("Attente de Lutece sur {}...", lutece.getBaseURL());
        int maxWait = 180; // secondes - donner plus de temps pour le déploiement
        for (int i = 0; i < maxWait / 5; i++) {
            if (lutece.isApplicationReady()) {
                LOGGER.info("Lutece prêt après {} secondes", i * 5);
                return;
            }
            LOGGER.debug("Lutece pas encore prêt, tentative {}/{}", i + 1, maxWait / 5);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for Lutece", e);
            }
        }
        // Log container status before failing
        LOGGER.error("Container running: {}, URL: {}", lutece.isRunning(), lutece.getBaseURL());
        throw new RuntimeException("Lutece non disponible après " + maxWait + " secondes sur " + lutece.getBaseURL());
    }

    @Test
    @Order(1)
    @DisplayName("Vérification que les conteneurs sont démarrés")
    void testContainersRunning() {
        assertTrue(containersStarted, "Les conteneurs devraient être démarrés");
        assertTrue(mariadb.isRunning(), "MariaDB devrait être en cours d'exécution");
        assertTrue(lutece.isRunning(), "Lutece devrait être en cours d'exécution");

        String baseUrl = System.getProperty("lutece.base.url");
        LOGGER.info("Tests utiliseront l'URL: {}", baseUrl);
        assertTrue(baseUrl != null && baseUrl.contains("localhost"), "L'URL de base devrait être configurée");
    }

    @AfterAll
    void stopContainers() {
        // Les conteneurs seront arrêtés par le shutdown hook
        LOGGER.info("Tests terminés - les conteneurs seront arrêtés à la fin de la JVM");
    }

    private void stopContainersStatic() {
        LOGGER.info("=== Arrêt de l'environnement Testcontainers ===");

        if (lutece != null && lutece.isRunning()) {
            lutece.stop();
            LOGGER.info("Lutece arrêté");
        }
        if (mariadb != null && mariadb.isRunning()) {
            mariadb.stop();
            LOGGER.info("MariaDB arrêté");
        }
        if (network != null) {
            network.close();
            LOGGER.info("Réseau fermé");
        }
    }

    // Accesseurs statiques pour d'autres classes si nécessaire
    public static LuteceContainer getLuteceContainer() {
        return lutece;
    }

    public static MariaDBContainer<?> getMariaDBContainer() {
        return mariadb;
    }

    public static boolean isContainersStarted() {
        return containersStarted;
    }
}
