package fr.paris.lutece.e2e.tests.bo.containers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Conteneur Testcontainers pour l'application Lutece sur Open Liberty.
 * Basé sur l'image Docker nexus-docker-fastdeploy.api.paris.mdp/bild/f98/site-deontologie:1.0.0-SNAPSHOT.
 */
public class LuteceContainer extends GenericContainer<LuteceContainer> {

    private static final Logger LOGGER = LogManager.getLogger(LuteceContainer.class);

    public static final int HTTP_PORT = 9090;
    public static final int HTTPS_PORT = 9443;
    public static final String DEFAULT_CONTEXT_ROOT = "/lutece";

    private final String contextRoot;

    /**
     * Crée un conteneur Lutece avec l'image par défaut.
     */
    public LuteceContainer() {
        this("nexus-docker-fastdeploy.api.paris.mdp/bild/f98/site-deontologie:1.0.0-SNAPSHOT", DEFAULT_CONTEXT_ROOT);
    }

    /**
     * Crée un conteneur Lutece avec une image personnalisée.
     *
     * @param imageName   Nom de l'image Docker
     * @param contextRoot Context root de l'application (ex: /site-deontologie)
     */
    public LuteceContainer(String imageName, String contextRoot) {
        super(DockerImageName.parse(imageName));
        this.contextRoot = contextRoot;
        configureContainer();
    }

    private void configureContainer() {
        // Exposer les ports HTTP et HTTPS
        withExposedPorts(HTTP_PORT, HTTPS_PORT);

        // Proprietes systeme Java pour OpenLiberty (via JVM_ARGS)
        withEnv("JVM_ARGS", "-Dotel.sdk.disabled=true -Dmp.config.profile=test");

        // Attendre que Liberty soit prêt (message CWWKF0011I = Server ready)
        // 5 minutes pour permettre à Liquibase de terminer les migrations
        waitingFor(Wait.forLogMessage(".*CWWKF0011I.*", 1)
            .withStartupTimeout(Duration.ofMinutes(5)));

        // Ajouter les logs du conteneur aux logs de test
        // Slf4jLogConsumer requiert un logger SLF4J, le pont log4j-slf4j2-impl route vers Log4j2
        withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(LuteceContainer.class)).withPrefix("lutece"));
    }

    /**
     * Configure la connexion à la base de données MariaDB.
     *
     * @param mariaDbHost     Alias réseau du conteneur MariaDB
     * @param mariaDbPort     Port MariaDB (généralement 3306)
     * @param databaseName    Nom de la base de données
     * @param username        Utilisateur de la base
     * @param password        Mot de passe
     * @return this
     */
    public LuteceContainer withMariaDB(String mariaDbHost, int mariaDbPort,
                                        String databaseName, String username, String password) {
        // Variables Open Liberty server.xml pour la DataSource
        withEnv("portal.serverName", mariaDbHost);
        withEnv("portal.port", String.valueOf(mariaDbPort));
        withEnv("portal.dbname", databaseName);
        withEnv("portal.user", username);
        withEnv("portal.password", password);

        // Sous Podman, l'auto-detection JDBC de Liberty echoue et retombe sur H2
        // (classes H2 fournies par la feature persistence-3.1).
        // On force les classes MySQL sur le <jdbcDriver> pour contourner l'auto-detection.
        // On garde <properties> generique (pas <properties.mysql>) pour ne pas changer
        // le comportement de connexion (MysqlConnectionPoolDataSource vs DataSource generique).
        // withCommand (CMD) est ignore par certaines images Liberty, on surcharge l'ENTRYPOINT.
        // Le patch est conditionnel : certaines images ont deja les classes JDBC specifiees,
        // les ajouter en doublon cause une erreur XML (CWWKG0014E: AttributeNotUnique).
        // Only patch if the image doesn't already specify JDBC driver classes
        withCreateContainerCmdModifier(cmd ->
            cmd.withEntrypoint("bash", "-c",
                "if ! grep -q 'javax.sql.DataSource=' /config/server.xml; then " +
                "sed -i 's|libraryRef=\"jdbcLib\"|libraryRef=\"jdbcLib\" " +
                "javax.sql.DataSource=\"com.mysql.cj.jdbc.MysqlDataSource\" " +
                "javax.sql.ConnectionPoolDataSource=\"com.mysql.cj.jdbc.MysqlConnectionPoolDataSource\" " +
                "javax.sql.XADataSource=\"com.mysql.cj.jdbc.MysqlXADataSource\"|' /config/server.xml; " +
                "fi && " +
                // Certaines images (ex: p30/site-integration-forms) declarent une DataSource MariaDB
                // (org.mariadb.jdbc.MariaDbDataSource) mais un fileset jdbcLib qui n'inclut que
                // 'mysql-connector*.jar' (absent de l'image) -> la librairie jdbcLib est vide et Liberty
                // leve DSRA4000E / ClassNotFoundException. Le driver reellement present est
                // mariadb-java-client-*.jar. On elargit donc l'include du fileset pour couvrir les deux
                // pilotes (idempotent : si l'include ne matche pas, aucun changement).
                "sed -i 's|includes=\"mysql-connector[^\"]*\"|includes=\"mariadb-java-client*.jar,mysql-connector*.jar\"|' /config/server.xml && " +
                "exec /opt/ol/helpers/runtime/docker-server.sh /opt/ol/wlp/bin/server run defaultServer"));

        return this;
    }

    /**
     * Configure le conteneur pour utiliser un réseau partagé.
     *
     * @param network      Réseau Docker partagé
     * @param networkAlias Alias réseau pour ce conteneur
     * @return this
     */
    public LuteceContainer withSharedNetwork(Network network, String networkAlias) {
        withNetwork(network);
        withNetworkAliases(networkAlias);
        return this;
    }

    /**
     * Configure le conteneur pour utiliser un port fixe sur l'hôte.
     *
     * @param hostPort Port sur l'hôte (ex: 9080)
     * @return this
     */
    public LuteceContainer withFixedExposedPort(int hostPort) {
        addFixedExposedPort(hostPort, HTTP_PORT);
        return this;
    }

    /**
     * Retourne l'URL de base HTTP de l'application.
     * Ex: http://localhost:32789/site-deontologie
     */
    public String getBaseURL() {
        return String.format("http://%s:%d%s",
            getHost(),
            getMappedPort(HTTP_PORT),
            contextRoot);
    }

    /**
     * Retourne l'URL de base HTTPS de l'application.
     */
    public String getSecureBaseURL() {
        return String.format("https://%s:%d%s",
            getHost(),
            getMappedPort(HTTPS_PORT),
            contextRoot);
    }

    /**
     * Retourne le port HTTP mappé.
     */
    public int getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Retourne le port HTTPS mappé.
     */
    public int getHttpsPort() {
        return getMappedPort(HTTPS_PORT);
    }

    /**
     * Vérifie si l'application est prête en testant la page de login.
     * La page de login ne fonctionne que si la base de données est initialisée.
     */
    public boolean isApplicationReady() {
        try {
            String healthUrl = getBaseURL() + "/jsp/admin/AdminLogin.jsp";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                new java.net.URL(healthUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                // Vérifier que la page contient le formulaire de login (base de données prête)
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("access_code") || line.contains("AdminLogin") || line.contains("password")) {
                            LOGGER.info("Application prête - page de login chargée");
                            return true;
                        }
                    }
                }
                LOGGER.info("Page retournée (200) mais pas de formulaire de login");
                return false;
            }
            LOGGER.info("Health check {} returned {}", healthUrl, responseCode);
            return false;
        } catch (Exception e) {
            LOGGER.info("Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
