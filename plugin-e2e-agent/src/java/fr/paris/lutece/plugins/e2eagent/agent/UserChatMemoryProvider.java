package fr.paris.lutece.plugins.e2eagent.agent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


/**
 * Fournit une ChatMemory isolee par session utilisateur (X-Session-Id).
 * Le nettoyage est declenche par l'expiration du HttpSession via
 * {@link fr.paris.lutece.plugins.e2eagent.web.ChatMemorySessionListener}.
 */
@ApplicationScoped
@Named("per-user-memory")
public class UserChatMemoryProvider implements ChatMemoryProvider {

    private static final Logger LOG = LogManager.getLogger(UserChatMemoryProvider.class);
    private static final int MAX_MESSAGES = 20;

    private final Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();

    @Override
    public ChatMemory get(Object memoryId) {
        return memories.computeIfAbsent(memoryId, id -> {
            LOG.info("Creation ChatMemory pour session {}", id);
            return MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(MAX_MESSAGES)
                    .build();
        });
    }

    /**
     * Verifie si une ChatMemory existe pour cette session (sans en creer une nouvelle).
     */
    public boolean hasMemory(Object memoryId) {
        return memories.containsKey(memoryId);
    }

    /**
     * Libere la memoire d'une session.
     * Appele par le HttpSessionListener a l'expiration de la session.
     */
    public void evict(Object memoryId) {
        ChatMemory removed = memories.remove(memoryId);
        if (removed != null) {
            LOG.info("ChatMemory evictee pour session {}", memoryId);
        }
    }

    /**
     * Nombre de sessions actives (monitoring/debug).
     */
    public int activeSessionCount() {
        return memories.size();
    }
}
