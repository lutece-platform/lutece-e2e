package fr.paris.lutece.e2e.web;

import fr.paris.lutece.e2e.agent.LuteceAiService;
import fr.paris.lutece.e2e.core.BrowserManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API REST pour communiquer avec l'agent Lutece.
 */
@ApplicationScoped
@Path("/agent")
public class AgentResource {

    private static final Logger LOGGER = Logger.getLogger(AgentResource.class.getName());

    @Inject
    private LuteceAiService aiService;

    @Inject
    private BrowserManager browserManager;

    /**
     * Endpoint de chat avec l'agent.
     */
    @POST
    @Path("/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chat(ChatRequest request) {
        LOGGER.info("Requete recue: " + request.getMessage());

        try {
            String response = aiService.chat(request.getMessage());
            LOGGER.info("Reponse: " + response);
            return Response.ok(new ChatResponse(response, false)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chat", e);
            return Response.ok(new ChatResponse(
                "Erreur: " + e.getMessage(), true)).build();
        }
    }

    /**
     * Endpoint de sante.
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok("{\"status\":\"UP\"}").build();
    }

    /**
     * Obtenir la configuration actuelle.
     */
    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() {
        try {
            ConfigResponse config = new ConfigResponse();
            config.setUrl(browserManager.getBaseUrl());
            config.setConfigured(browserManager.isBaseUrlConfigured());
            return Response.ok(config).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur getConfig", e);
            return Response.ok(new ConfigResponse(null, false, "Erreur: " + e.getMessage())).build();
        }
    }

    /**
     * Configurer l'URL du site Lutece.
     */
    @POST
    @Path("/config/url")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setUrl(UrlRequest request) {
        try {
            LOGGER.info("Configuration URL: " + request.getUrl());

            if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
                return Response.ok(new ConfigResponse(null, false, "L'URL ne peut pas etre vide")).build();
            }

            String url = request.getUrl().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return Response.ok(new ConfigResponse(null, false, "L'URL doit commencer par http:// ou https://")).build();
            }

            browserManager.setBaseUrl(url);
            return Response.ok(new ConfigResponse(browserManager.getBaseUrl(), true, "URL configuree avec succes")).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur setUrl", e);
            return Response.ok(new ConfigResponse(null, false, "Erreur: " + e.getMessage())).build();
        }
    }

    /**
     * Tester la connexion a une URL et l'authentification.
     */
    @POST
    @Path("/config/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testConnection(UrlRequest request) {
        try {
            LOGGER.info("Test connexion URL: " + request.getUrl());

            if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
                return Response.ok(new TestResponse(false, 0, 0, "L'URL ne peut pas etre vide", false, false)).build();
            }

            String urlStr = request.getUrl().trim();
            if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                return Response.ok(new TestResponse(false, 0, 0, "L'URL doit commencer par http:// ou https://", false, false)).build();
            }

            // Enlever le slash final si present
            if (urlStr.endsWith("/")) {
                urlStr = urlStr.substring(0, urlStr.length() - 1);
            }

            long startTime = System.currentTimeMillis();

            // Etape 1: Tester la connexion au site
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Lutece-E2E-Agent/1.0");

            int statusCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            connection.disconnect();

            if (statusCode < 200 || statusCode >= 400) {
                return Response.ok(new TestResponse(false, statusCode, responseTime,
                    "Erreur HTTP " + statusCode, false, false)).build();
            }

            // Etape 2: Tester l'authentification si credentials fournis
            String username = request.getUsername();
            String password = request.getPassword();

            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                return Response.ok(new TestResponse(true, statusCode, responseTime,
                    "Site accessible (HTTP " + statusCode + ")", false, false)).build();
            }

            // Tester l'authentification Lutece
            startTime = System.currentTimeMillis();
            TestResponse authResult = testLuteceAuth(urlStr, username.trim(), password);
            authResult.setResponseTime(System.currentTimeMillis() - startTime);

            return Response.ok(authResult).build();

        } catch (java.net.ConnectException e) {
            LOGGER.log(Level.WARNING, "Connexion refusee", e);
            return Response.ok(new TestResponse(false, 0, 0, "Connexion refusee - Le serveur est-il demarre?", false, false)).build();
        } catch (java.net.SocketTimeoutException e) {
            LOGGER.log(Level.WARNING, "Timeout", e);
            return Response.ok(new TestResponse(false, 0, 0, "Timeout - Le serveur ne repond pas", false, false)).build();
        } catch (java.net.UnknownHostException e) {
            LOGGER.log(Level.WARNING, "Hote inconnu", e);
            return Response.ok(new TestResponse(false, 0, 0, "Hote inconnu - Verifiez l'URL", false, false)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur test connexion", e);
            return Response.ok(new TestResponse(false, 0, 0, "Erreur: " + e.getMessage(), false, false)).build();
        }
    }

    /**
     * Teste l'authentification sur un site Lutece.
     */
    private TestResponse testLuteceAuth(String baseUrl, String username, String password) {
        HttpURLConnection conn = null;
        String cookies = "";
        try {
            // Etape 1: Acceder a la page de login pour obtenir les cookies de session et le token CSRF
            String loginPageUrl = baseUrl + "/jsp/admin/AdminLogin.jsp";
            URL url = new URL(loginPageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Lutece-E2E-Agent/1.0");

            int loginPageStatus = conn.getResponseCode();

            // Extraire tous les cookies
            cookies = extractCookies(conn);

            // Lire le contenu de la page pour extraire le token CSRF
            String csrfToken = "";
            if (loginPageStatus == 200) {
                StringBuilder loginPageContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        loginPageContent.append(line).append("\n");
                    }
                }
                csrfToken = extractCsrfToken(loginPageContent.toString());
                LOGGER.info("Token CSRF extrait: " + csrfToken);
            }
            conn.disconnect();

            if (loginPageStatus != 200) {
                return new TestResponse(true, loginPageStatus, 0,
                    "Site accessible, page login non trouvee (HTTP " + loginPageStatus + ")", true, false);
            }

            LOGGER.info("Page login accessible, cookies: " + cookies);

            // Etape 2: Soumettre le formulaire de login avec le token CSRF
            String doLoginUrl = baseUrl + "/jsp/admin/DoAdminLogin.jsp";
            url = new URL(doLoginUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Lutece-E2E-Agent/1.0");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (!cookies.isEmpty()) {
                conn.setRequestProperty("Cookie", cookies);
            }

            // Construire les donnees POST avec le token CSRF
            StringBuilder postDataBuilder = new StringBuilder();
            postDataBuilder.append("access_code=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
            postDataBuilder.append("&password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8));
            if (!csrfToken.isEmpty()) {
                postDataBuilder.append("&token=").append(URLEncoder.encode(csrfToken, StandardCharsets.UTF_8));
            }
            String postData = postDataBuilder.toString();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int authStatus = conn.getResponseCode();
            String location = conn.getHeaderField("Location");

            // Mettre a jour les cookies avec les nouveaux
            String newCookies = extractCookies(conn);
            if (!newCookies.isEmpty()) {
                cookies = mergeCookies(cookies, newCookies);
            }
            conn.disconnect();

            LOGGER.info("DoAdminLogin response: status=" + authStatus + ", location=" + location);

            // Etape 3: Suivre la redirection et verifier la page finale
            if ((authStatus == 302 || authStatus == 303) && location != null) {
                // Construire l'URL complete si relative
                String redirectUrl = location;
                if (location.startsWith("/")) {
                    URL baseUrlObj = new URL(baseUrl);
                    redirectUrl = baseUrlObj.getProtocol() + "://" + baseUrlObj.getHost()
                        + (baseUrlObj.getPort() > 0 ? ":" + baseUrlObj.getPort() : "") + location;
                } else if (!location.startsWith("http")) {
                    redirectUrl = baseUrl + "/" + location;
                }

                LOGGER.info("Following redirect to: " + redirectUrl);

                // Acceder a la page de redirection
                url = new URL(redirectUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Lutece-E2E-Agent/1.0");
                if (!cookies.isEmpty()) {
                    conn.setRequestProperty("Cookie", cookies);
                }

                int finalStatus = conn.getResponseCode();
                String finalUrl = conn.getURL().toString();

                // Lire le contenu de la page finale
                StringBuilder responseContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line).append("\n");
                    }
                } catch (Exception e) {
                    LOGGER.warning("Erreur lecture reponse: " + e.getMessage());
                }
                conn.disconnect();

                String content = responseContent.toString();
                String contentLower = content.toLowerCase();

                // Critere de succes: redirection vers AdminMenu.jsp
                boolean isAdminMenu = finalUrl.contains("AdminMenu");

                // Critere d'echec: redirection vers AdminMessage.jsp ou AdminLogin.jsp
                boolean isErrorPage = finalUrl.contains("AdminMessage");
                boolean isLoginPage = finalUrl.contains("AdminLogin")
                    || content.contains("name=\"access_code\"")
                    || content.contains("DoAdminLogin.jsp");

                LOGGER.info("Final page: url=" + finalUrl + ", status=" + finalStatus
                    + ", isAdminMenu=" + isAdminMenu + ", isErrorPage=" + isErrorPage + ", isLoginPage=" + isLoginPage);

                // Succes: redirection vers AdminMenu
                if (isAdminMenu) {
                    return new TestResponse(true, finalStatus, 0,
                        "Authentification reussie", true, true);
                }

                // Echec: redirection vers AdminMessage (page d'erreur)
                if (isErrorPage) {
                    return new TestResponse(true, finalStatus, 0,
                        "Echec authentification - identifiants incorrects", true, false);
                }

                // Echec: retour a la page de login
                if (isLoginPage) {
                    return new TestResponse(true, finalStatus, 0,
                        "Echec authentification - redirection vers login", true, false);
                }

                // Cas inconnu - considerer comme succes si pas de page d'erreur
                return new TestResponse(true, finalStatus, 0,
                    "Site accessible, verifiez manuellement", true, false);
            }

            // Pas de redirection - verifier le contenu direct
            return new TestResponse(true, authStatus, 0,
                "Site accessible, verifiez manuellement l'authentification", true, false);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur test authentification", e);
            return new TestResponse(true, 0, 0,
                "Site accessible, erreur test auth: " + e.getMessage(), true, false);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Extrait les cookies des headers de reponse.
     */
    private String extractCookies(HttpURLConnection conn) {
        StringBuilder cookieBuilder = new StringBuilder();
        for (int i = 0; ; i++) {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);
            if (headerName == null && headerValue == null) break;
            if ("Set-Cookie".equalsIgnoreCase(headerName)) {
                // Extraire juste le nom=valeur avant le ;
                String cookie = headerValue.split(";")[0];
                if (cookieBuilder.length() > 0) {
                    cookieBuilder.append("; ");
                }
                cookieBuilder.append(cookie);
            }
        }
        return cookieBuilder.toString();
    }

    /**
     * Fusionne les anciens et nouveaux cookies.
     */
    private String mergeCookies(String oldCookies, String newCookies) {
        if (oldCookies.isEmpty()) return newCookies;
        if (newCookies.isEmpty()) return oldCookies;
        return oldCookies + "; " + newCookies;
    }

    /**
     * Extrait le token CSRF du contenu HTML de la page de login.
     */
    private String extractCsrfToken(String htmlContent) {
        // Chercher le pattern: <input type="hidden" name="token" value="xxx" />
        String searchPattern = "name=\"token\"";
        int tokenNameIndex = htmlContent.indexOf(searchPattern);
        if (tokenNameIndex == -1) {
            // Essayer avec guillemets simples
            searchPattern = "name='token'";
            tokenNameIndex = htmlContent.indexOf(searchPattern);
        }
        if (tokenNameIndex == -1) {
            return "";
        }

        // Chercher value= avant ou apres
        int searchStart = Math.max(0, tokenNameIndex - 100);
        int searchEnd = Math.min(htmlContent.length(), tokenNameIndex + 100);
        String context = htmlContent.substring(searchStart, searchEnd);

        // Pattern: value="xxx"
        int valueIndex = context.indexOf("value=\"");
        if (valueIndex != -1) {
            int start = valueIndex + 7;
            int end = context.indexOf("\"", start);
            if (end != -1) {
                return context.substring(start, end);
            }
        }

        // Pattern: value='xxx'
        valueIndex = context.indexOf("value='");
        if (valueIndex != -1) {
            int start = valueIndex + 7;
            int end = context.indexOf("'", start);
            if (end != -1) {
                return context.substring(start, end);
            }
        }

        return "";
    }

    /**
     * Requete de configuration URL.
     */
    public static class UrlRequest {
        private String url;
        private String username;
        private String password;

        public UrlRequest() {}

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Reponse de configuration.
     */
    public static class ConfigResponse {
        private String url;
        private boolean configured;
        private String message;

        public ConfigResponse() {}

        public ConfigResponse(String url, boolean configured) {
            this.url = url;
            this.configured = configured;
        }

        public ConfigResponse(String url, boolean configured, String message) {
            this.url = url;
            this.configured = configured;
            this.message = message;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isConfigured() {
            return configured;
        }

        public void setConfigured(boolean configured) {
            this.configured = configured;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Reponse de test de connexion.
     */
    public static class TestResponse {
        private boolean success;
        private int statusCode;
        private long responseTime;
        private String message;
        private boolean authTested;
        private boolean authSuccess;

        public TestResponse() {}

        public TestResponse(boolean success, int statusCode, long responseTime, String message,
                           boolean authTested, boolean authSuccess) {
            this.success = success;
            this.statusCode = statusCode;
            this.responseTime = responseTime;
            this.message = message;
            this.authTested = authTested;
            this.authSuccess = authSuccess;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public long getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(long responseTime) {
            this.responseTime = responseTime;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isAuthTested() {
            return authTested;
        }

        public void setAuthTested(boolean authTested) {
            this.authTested = authTested;
        }

        public boolean isAuthSuccess() {
            return authSuccess;
        }

        public void setAuthSuccess(boolean authSuccess) {
            this.authSuccess = authSuccess;
        }
    }

    /**
     * Requete de chat.
     */
    public static class ChatRequest {
        private String message;

        public ChatRequest() {}

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Reponse de chat.
     */
    public static class ChatResponse {
        private String response;
        private boolean error;

        public ChatResponse() {}

        public ChatResponse(String response, boolean error) {
            this.response = response;
            this.error = error;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public boolean isError() {
            return error;
        }

        public void setError(boolean error) {
            this.error = error;
        }
    }
}
