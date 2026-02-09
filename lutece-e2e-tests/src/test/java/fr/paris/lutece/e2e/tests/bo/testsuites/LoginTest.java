package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import fr.paris.lutece.e2e.tests.bo.pages.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E pour la fonctionnalité de connexion Lutece.
 */
@DisplayName("Tests de connexion admin Lutece")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginTest extends BaseTest {

    private LoginPage loginPage;

    @BeforeEach
    void setupPages() {
        loginPage = new LoginPage(page, BASE_URL);
    }

    @Test
    @Order(1)
    @DisplayName("Connexion réussie avec identifiants valides")
    void testLoginSuccess() {
        // Given
        loginPage.navigate();

        // When
        AdminMenuPage adminMenu = loginPage.loginAs("admin", "adminadmin");

        // Then
        assertTrue(adminMenu.isLoggedIn(), "L'utilisateur devrait être connecté");
    }

    @Test
    @Order(2)
    @DisplayName("Échec de connexion avec mauvais mot de passe")
    void testLoginFailureWrongPassword() {
        // Given
        loginPage.navigate();

        // When
        loginPage.fillUsername("admin");
        loginPage.fillPassword("wrongpassword");
        loginPage.clickLogin();

        // Then
        assertTrue(loginPage.hasErrorMessage(), "Un message d'erreur devrait être affiché");
    }

    @Test
    @Order(3)
    @DisplayName("Échec de connexion avec utilisateur inexistant")
    void testLoginFailureWrongUser() {
        // Given
        loginPage.navigate();

        // When
        loginPage.fillUsername("utilisateur_inexistant");
        loginPage.fillPassword("password");
        loginPage.clickLogin();

        // Then
        assertTrue(loginPage.hasErrorMessage(), "Un message d'erreur devrait être affiché");
    }

    @Test
    @Order(4)
    @DisplayName("Champs obligatoires - formulaire vide")
    void testLoginEmptyFields() {
        // Given
        loginPage.navigate();

        // When
        loginPage.clickLogin();

        // Then - Le formulaire ne devrait pas être soumis ou une erreur devrait s'afficher
        String currentUrl = page.url();
        assertTrue(currentUrl.contains("AdminLogin"), "On devrait rester sur la page de login");
    }
}
