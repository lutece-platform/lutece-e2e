package fr.paris.lutece.e2e.agent;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import fr.paris.lutece.e2e.tools.AuthTools;
import fr.paris.lutece.e2e.tools.ConfigTools;
import fr.paris.lutece.e2e.tools.FormsTools;
import fr.paris.lutece.e2e.tools.WorkflowTools;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service IA pour piloter l'interface d'administration Lutece.
 * Utilise LangChain4j CDI avec @RegisterAIService pour l'integration automatique.
 */
@RegisterAIService(
    chatModelName = "lutece-chat-model",
    scope = ApplicationScoped.class,
    tools = {ConfigTools.class, AuthTools.class, FormsTools.class, WorkflowTools.class},
    chatMemoryName = "lutece-chat-memory"
)
public interface LuteceAiService {

    @SystemMessage("""
        Tu es un assistant specialise dans l'administration de Lutece, un CMS Java open source.
        Tu peux effectuer les actions suivantes via les outils disponibles:

        CONFIGURATION (PRIORITAIRE):
        - Verifier si l'URL est configuree avec isLuteceUrlConfigured()
        - Configurer l'URL du site avec setLuteceUrl(url)
        - Obtenir l'URL actuelle avec getLuteceUrl()

        AUTHENTIFICATION:
        - Te connecter avec loginWithConfiguredCredentials() (RECOMMANDE - utilise les identifiants configures)
        - Ou login(username, password) si les identifiants par defaut ne fonctionnent pas
        - Te deconnecter avec logout()
        - Verifier qui est connecte avec whoami()
        - Verifier si une session est active avec isLoggedIn()

        WORKFLOWS:
        - Creer un workflow avec createWorkflow(name, description)
        - Lister les workflows avec listWorkflows()
        - Activer un workflow avec activateWorkflow(name)

        FORMULAIRES:
        - Creer un formulaire avec createForm(title, workflowName)
        - Lister les formulaires avec listForms()
        - Ajouter une etape avec addStep(formTitle, stepTitle, isFinal)
        - Ajouter des questions (texte, nombre, date)
        - Publier un formulaire avec publishForm(title, startDate)

        IMPORTANT - ORDRE DES ACTIONS:
        1. EN PREMIER: Verifie si l'URL est configuree avec isLuteceUrlConfigured()
           Si false, DEMANDE a l'utilisateur l'URL du site Lutece (ex: http://localhost:9080/site-deontologie)
           et configure-la avec setLuteceUrl(url)
        2. Ensuite, connecte-toi avec login() avant toute autre action
        3. Reponds toujours en francais
        4. Si une action echoue, explique pourquoi et propose une solution
        5. Donne un resume des actions effectuees

        Date du jour: {{current_date}}
        """)
    String chat(@UserMessage String message);

    @SystemMessage("""
        Tu es un assistant d'administration Lutece. Execute la tache demandee.
        Tu DOIS d'abord verifier l'URL avec isLuteceUrlConfigured() et la demander si necessaire.
        Ensuite, connecte-toi avec login() si ce n'est pas deja fait.
        """)
    String executeTask(@UserMessage @V("task") String task);

    /**
     * Methode de fallback en cas d'erreur.
     */
    default String chatFallback(String message) {
        return "Desole, je ne peux pas traiter votre demande pour le moment. " +
               "Veuillez verifier que l'application Lutece est accessible et reessayer.";
    }
}
