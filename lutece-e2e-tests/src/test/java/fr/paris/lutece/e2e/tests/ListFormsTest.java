package fr.paris.lutece.e2e.tests;

import fr.paris.lutece.e2e.actions.AuthActions;
import fr.paris.lutece.e2e.actions.FormsActions;
import fr.paris.lutece.e2e.pages.BasePage;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@EnableAutoWeld
@AddPackages({AuthActions.class, BasePage.class})
@DisplayName("Test liste des formulaires")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListFormsTest {

    @Inject
    AuthActions authActions;

    @Inject
    FormsActions formsActions;

    @Test
    @Order(1)
    @DisplayName("Connexion")
    void testLogin() {
        var result = authActions.login("admin", "adminadmin");
        assertTrue(result.isSuccess(), "Connexion: " + result.getError());
    }

    @Test
    @Order(2)
    @DisplayName("Liste des formulaires")
    void testListForms() {
        var result = formsActions.navigateToList();
        assertTrue(result.isSuccess(), "Liste: " + result.getError());
        
        String list = result.getData();
        System.out.println("=== LISTE DES FORMULAIRES ===");
        System.out.println(list);
        System.out.println("=============================");
        
        assertNotNull(list, "La liste ne devrait pas être null");
    }

    @AfterAll
    void cleanup() {
        authActions.logout();
    }
}
