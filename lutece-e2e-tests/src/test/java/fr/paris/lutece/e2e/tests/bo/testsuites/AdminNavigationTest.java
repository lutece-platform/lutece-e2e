package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import fr.paris.lutece.e2e.tests.bo.pages.*;
import com.microsoft.playwright.Page;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E pour la navigation dans l'admin Lutece.
 */
@DisplayName("Tests de navigation admin Lutece")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminNavigationTest extends BaseTest {

    private LoginPage loginPage;
    private AdminMenuPage adminMenu;

    @BeforeEach
    void loginAndSetupPages() {
        loginPage = new LoginPage(page, BASE_URL);
        loginPage.navigate();
        adminMenu = loginPage.loginAs("admin", "adminadmin");
    }

    @Test
    @Order(1)
    @DisplayName("Accès au menu Système")
    void testAccessSystemMenu() {
        // When
        adminMenu.clickSystemMenu();

        // Then - Vérifier que le sous-menu s'affiche
        page.waitForTimeout(500); // Petit délai pour l'animation du menu
        assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                   new Page.GetByRoleOptions().setName("Gestion des propriétés du site")).isVisible(),
                   "Le sous-menu Système devrait être visible");
    }

    @Test
    @Order(2)
    @DisplayName("Accès à la gestion des propriétés du site")
    void testAccessSiteProperties() {
        // When
        SitePropertiesPage propertiesPage = adminMenu.goToSiteProperties();

        // Then
        assertTrue(propertiesPage.isDisplayed(),
                   "La page des propriétés du site devrait être affichée");
    }

    @Test
    @Order(3)
    @DisplayName("Navigation vers l'onglet Propriétés par défaut")
    void testNavigateToDefaultPropertiesTab() {
        // Given
        SitePropertiesPage propertiesPage = adminMenu.goToSiteProperties();

        // When
        propertiesPage.clickDefaultPropertiesTab();

        // Then
        assertTrue(propertiesPage.isDefaultPropertiesTabActive(),
                   "L'onglet Propriétés par défaut devrait être actif");
    }

    @Test
    @Order(4)
    @DisplayName("Accès au menu Gestionnaires")
    void testAccessGestionnairesMenu() {
        // When
        adminMenu.clickGestionnairesMenu();

        // Then
        page.waitForTimeout(500);
        // Vérifier que le sous-menu s'affiche (adapter selon votre UI)
        assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                   new Page.GetByRoleOptions().setName(" Gestionnaires")).isVisible(),
                   "Le menu Gestionnaires devrait être accessible");
    }
}
