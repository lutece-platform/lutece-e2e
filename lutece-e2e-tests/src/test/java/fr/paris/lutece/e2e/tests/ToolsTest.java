package fr.paris.lutece.e2e.tests;

import fr.paris.lutece.e2e.tools.AuthTools;
import fr.paris.lutece.e2e.tools.FormsTools;
import fr.paris.lutece.e2e.tools.WorkflowTools;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests des Tools LangChain4j.
 * Verifie que les tools fonctionnent correctement comme des fonctions appelables par un agent IA.
 */
@EnableAutoWeld
@AddPackages(AuthTools.class)
@DisplayName("Tests des Tools LangChain4j")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToolsTest {

    @Inject
    AuthTools authTools;

    @Inject
    WorkflowTools workflowTools;

    @Inject
    FormsTools formsTools;

    private String runSuffix;
    private String workflowName;
    private String formTitle;

    @BeforeAll
    void setup() {
        runSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        workflowName = "ToolTest Workflow " + runSuffix;
        formTitle = "ToolTest Form " + runSuffix;
    }

    @Test
    @Order(1)
    @DisplayName("Tool: login")
    void testLoginTool() {
        String result = authTools.login("admin", "adminadmin");

        System.out.println("Login result: " + result);
        assertTrue(result.contains("Succes") || result.contains("succes") || result.contains("SUCCESS")
            || result.contains("OK") || result.contains("restauree"),
            "Le login devrait reussir: " + result);
    }

    @Test
    @Order(2)
    @DisplayName("Tool: isLoggedIn")
    void testIsLoggedInTool() {
        boolean loggedIn = authTools.isLoggedIn();

        System.out.println("Is logged in: " + loggedIn);
        assertTrue(loggedIn, "L'utilisateur devrait etre connecte");
    }

    @Test
    @Order(3)
    @DisplayName("Tool: whoami")
    void testWhoamiTool() {
        String result = authTools.whoami();

        System.out.println("Whoami result: " + result);
        assertTrue(result.contains("admin") || result.contains("Admin"),
            "Le resultat devrait contenir admin: " + result);
    }

    @Test
    @Order(4)
    @DisplayName("Tool: createWorkflow")
    void testCreateWorkflowTool() {
        String result = workflowTools.createWorkflow(workflowName, "Workflow cree par tool test");

        System.out.println("Create workflow result: " + result);
        assertTrue(result.contains("Succes") || result.contains("succes") || result.contains("cree"),
            "Le workflow devrait etre cree: " + result);
    }

    @Test
    @Order(5)
    @DisplayName("Tool: listWorkflows")
    void testListWorkflowsTool() {
        String result = workflowTools.listWorkflows();

        System.out.println("List workflows result: " + result);
        assertNotNull(result, "La liste ne devrait pas etre null");
    }

    @Test
    @Order(6)
    @DisplayName("Tool: listForms")
    void testListFormsTool() {
        String result = formsTools.listForms();

        System.out.println("List forms result: " + result);
        assertNotNull(result, "La liste ne devrait pas etre null");
    }

    @Test
    @Order(7)
    @DisplayName("Tool: logout")
    void testLogoutTool() {
        String result = authTools.logout();

        System.out.println("Logout result: " + result);
        assertTrue(result.contains("Succes") || result.contains("succes") || result.contains("deconnect")
            || result.contains("OK") || result.contains("session"),
            "La deconnexion devrait reussir: " + result);
    }

    @AfterAll
    void cleanup() {
        // Rien a faire, logout deja fait dans le dernier test
    }
}
