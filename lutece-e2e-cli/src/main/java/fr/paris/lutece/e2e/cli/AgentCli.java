package fr.paris.lutece.e2e.cli;

import fr.paris.lutece.e2e.agent.LuteceAiService;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import java.util.Scanner;
import java.util.UUID;

/**
 * Interface CLI interactive pour l'agent Lutece.
 * Utilise LangChain4j CDI avec @RegisterAIService.
 */
public class AgentCli {

    private static final String BANNER = """

            ╔═══════════════════════════════════════════════════════════════╗
            ║           LUTECE E2E AGENT - Assistant IA                     ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║  Commandes:                                                   ║
            ║    quit, exit  - Quitter                                      ║
            ║    clear       - Nouvelle session                             ║
            ║    help        - Afficher l'aide                              ║
            ╚═══════════════════════════════════════════════════════════════╝

            Exemples de requetes:
              - "Connecte-toi a Lutece"
              - "Cree un workflow simple avec deux etats"
              - "Cree un formulaire de contact avec nom, email et message"

            """;

    private static final String HELP = """

            === AIDE ===

            L'agent peut effectuer les actions suivantes sur Lutece:

            AUTHENTIFICATION:
              - Se connecter/deconnecter
              - Verifier la session

            WORKFLOWS:
              - Creer un workflow
              - Ajouter des etats (initial, intermediaires, final)
              - Ajouter des actions (transitions entre etats)
              - Activer/desactiver un workflow

            FORMULAIRES:
              - Creer un formulaire (avec ou sans workflow)
              - Ajouter des etapes
              - Ajouter des questions (texte, nombre, date, etc.)
              - Configurer les transitions entre etapes
              - Publier sur le portail

            EXEMPLES:
              "Connecte-toi puis cree un workflow de validation simple"
              "Cree un formulaire de demande de conge avec les champs: nom, dates, motif"
              "Liste les workflows existants"

            """;

    public static void main(String[] args) {
        System.out.println(BANNER);

        // Verifier les variables d'environnement (Azure OpenAI par defaut)
        String azureKey = System.getenv("azure.openai.api.key");
        String azureEndpoint = System.getenv("azure.openai.endpoint");
        String anthropicKey = System.getenv("ANTHROPIC_API_KEY");
        String openaiKey = System.getenv("OPENAI_API_KEY");

        if ((azureKey == null || azureKey.isBlank() || azureEndpoint == null || azureEndpoint.isBlank())
            && (anthropicKey == null || anthropicKey.isBlank())
            && (openaiKey == null || openaiKey.isBlank())) {
            System.err.println("ERREUR: Aucune cle API configuree");
            System.err.println("Options:");
            System.err.println("  - Azure OpenAI: azure.openai.api.key, azure.openai.endpoint, azure.openai.deployment.name");
            System.err.println("  - Anthropic: ANTHROPIC_API_KEY");
            System.err.println("  - OpenAI: OPENAI_API_KEY");
            System.exit(1);
        }

        System.out.println("Initialisation du conteneur CDI...");

        try (SeContainer container = SeContainerInitializer.newInstance().initialize()) {
            // LuteceAiService est genere automatiquement par langchain4j-cdi
            LuteceAiService aiService = container.select(LuteceAiService.class).get();
            String sessionId = UUID.randomUUID().toString();

            System.out.println("Agent pret. Session: " + sessionId.substring(0, 8) + "...\n");

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n> ");
                    String input = scanner.nextLine().trim();

                    if (input.isEmpty()) {
                        continue;
                    }

                    // Commandes speciales
                    if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                        System.out.println("Au revoir!");
                        break;
                    }

                    if (input.equalsIgnoreCase("clear")) {
                        sessionId = UUID.randomUUID().toString();
                        System.out.println("Nouvelle session: " + sessionId.substring(0, 8) + "...");
                        // Note: avec langchain4j-cdi, la memoire est geree par le bean ApplicationScoped
                        // Pour vraiment effacer, il faudrait recreer le container
                        continue;
                    }

                    if (input.equalsIgnoreCase("help") || input.equals("?")) {
                        System.out.println(HELP);
                        continue;
                    }

                    // Envoyer au LLM via LuteceAiService
                    try {
                        System.out.println("\nReflexion...\n");
                        String response = aiService.chat(input);
                        System.out.println(response);
                    } catch (Exception e) {
                        System.err.println("Erreur: " + e.getMessage());
                        if (e.getMessage() != null && e.getMessage().contains("api")) {
                            System.err.println("Verifiez votre cle API");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur fatale: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
