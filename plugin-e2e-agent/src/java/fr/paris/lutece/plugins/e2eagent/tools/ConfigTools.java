package fr.paris.lutece.plugins.e2eagent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.e2e.core.BrowserManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Tools LangChain4j pour la configuration de l'environnement.
 */
@ApplicationScoped
public class ConfigTools {

    @Inject
    BrowserManager browserManager;

    @Tool("Configure l'URL du site Lutece cible. " +
          "IMPORTANT: Cette action DOIT etre executee en premier si l'URL n'est pas configuree. " +
          "Exemple: http://localhost:9080/site-deontologie")
    public String setLuteceUrl(
            @P("URL complete du site Lutece (ex: http://localhost:9080/site-deontologie)") String url) {
        if (url == null || url.trim().isEmpty()) {
            return "ERREUR: L'URL ne peut pas etre vide. " +
                   "Veuillez fournir une URL valide (ex: http://localhost:9080/site-deontologie)";
        }

        // Validation basique de l'URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "ERREUR: L'URL doit commencer par http:// ou https://. " +
                   "Exemple: http://localhost:9080/site-deontologie";
        }

        browserManager.setBaseUrl(url.trim());
        return "URL configuree avec succes: " + browserManager.getBaseUrl() +
               ". Vous pouvez maintenant effectuer des actions sur le site Lutece.";
    }

    @Tool("Verifie si l'URL du site Lutece est configuree. " +
          "Si false, demander l'URL a l'utilisateur avant toute autre action.")
    public boolean isLuteceUrlConfigured() {
        return browserManager.isBaseUrlConfigured();
    }

    @Tool("Retourne l'URL actuelle du site Lutece configure.")
    public String getLuteceUrl() {
        if (browserManager.isBaseUrlConfigured()) {
            return "URL actuelle: " + browserManager.getBaseUrl();
        } else {
            return "ATTENTION: Aucune URL configuree. " +
                   "Veuillez d'abord configurer l'URL avec setLuteceUrl().";
        }
    }
}
