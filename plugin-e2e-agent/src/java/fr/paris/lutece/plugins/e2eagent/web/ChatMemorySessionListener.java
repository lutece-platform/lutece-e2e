package fr.paris.lutece.plugins.e2eagent.web;

import fr.paris.lutece.plugins.e2eagent.agent.UserChatMemoryProvider;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nettoie les ChatMemory a l'expiration du HttpSession.
 * Chaque HttpSession peut contenir plusieurs X-Session-Id (un par onglet).
 * Le timeout de session est configure dans server.xml :
 * {@code <httpSession invalidationTimeout="30m"/>}
 */
@WebListener
public class ChatMemorySessionListener implements HttpSessionListener {

    private static final Logger LOG = LogManager.getLogger(ChatMemorySessionListener.class);

    /** Cle d'attribut HttpSession stockant les X-Session-Id enregistres. */
    static final String ATTR_SESSION_IDS = "e2e.agent.sessionIds";

    @Inject
    private UserChatMemoryProvider chatMemoryProvider;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession httpSession = event.getSession();
        @SuppressWarnings("unchecked")
        Set<String> sessionIds = (Set<String>) httpSession.getAttribute(ATTR_SESSION_IDS);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        LOG.info("HttpSession {} expiree — eviction de {} ChatMemory: {}",
                httpSession.getId(), sessionIds.size(), sessionIds);

        for (String sessionId : sessionIds) {
            chatMemoryProvider.evict(sessionId);
        }
    }

    /**
     * Enregistre un X-Session-Id dans le HttpSession courant.
     * Appele par AgentResource a chaque requete.
     */
    @SuppressWarnings("unchecked")
    public static void registerSessionId(HttpSession httpSession, String sessionId) {
        if (httpSession == null || sessionId == null || sessionId.isEmpty()) {
            return;
        }
        Set<String> ids = (Set<String>) httpSession.getAttribute(ATTR_SESSION_IDS);
        if (ids == null) {
            ids = ConcurrentHashMap.newKeySet();
            httpSession.setAttribute(ATTR_SESSION_IDS, ids);
        }
        ids.add(sessionId);
    }
}
