package fr.paris.lutece.e2e.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.e2e.actions.AuthActions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Tools LangChain4j pour l'authentification.
 */
@ApplicationScoped
public class AuthTools {

    @Inject
    AuthActions authActions;

    @Inject
    @ConfigProperty(name = "lutece.admin.username", defaultValue = "admin")
    String configuredUsername;

    @Inject
    @ConfigProperty(name = "lutece.admin.password", defaultValue = "adminadmin")
    String configuredPassword;

    @Tool("Se connecter a l'interface d'administration Lutece avec les identifiants configures. " +
          "UTILISER CETTE METHODE PAR DEFAUT pour la connexion. " +
          "Retourne un message de confirmation ou d'erreur.")
    public String loginWithConfiguredCredentials() {
        var result = authActions.login(configuredUsername, configuredPassword);
        return result.toToolMessage();
    }

    @Tool("Se connecter a l'interface d'administration Lutece avec des identifiants personnalises. " +
          "Utiliser uniquement si les identifiants par defaut ne fonctionnent pas. " +
          "Retourne un message de confirmation ou d'erreur.")
    public String login(
            @P("Nom d'utilisateur (ex: admin)") String username,
            @P("Mot de passe") String password) {
        var result = authActions.login(username, password);
        return result.toToolMessage();
    }

    @Tool("Se deconnecter de l'interface d'administration Lutece.")
    public String logout() {
        var result = authActions.logout();
        return result.toToolMessage();
    }

    @Tool("Verifier si une session est active et avec quel utilisateur.")
    public String whoami() {
        var result = authActions.whoami();
        return result.toToolMessage();
    }

    @Tool("Verifier si l'utilisateur est connecte. Retourne true ou false.")
    public boolean isLoggedIn() {
        return authActions.isLoggedIn();
    }

    @Tool("Reinitialiser la session du navigateur. " +
          "Utile en cas de problemes de session ou d'etat incoherent.")
    public String resetSession() {
        var result = authActions.resetSession();
        return result.toToolMessage();
    }
}
