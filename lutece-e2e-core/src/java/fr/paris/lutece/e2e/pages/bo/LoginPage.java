package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour la page de connexion admin Lutece.
 */
public class LoginPage {

    private final Page page;
    private final String baseUrl;

    // Locators
    private static final String USERNAME_FIELD = "Code d'accès. *";
    private static final String PASSWORD_FIELD = "Mot de passe *";
    private static final String LOGIN_BUTTON = "Se connecter";

    public LoginPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Navigue vers la page de login.
     */
    public LoginPage navigate() {
        page.navigate(baseUrl + "/jsp/admin/AdminLogin.jsp");
        return this;
    }

    /**
     * Remplit le champ code d'accès.
     * Utilise l'id stable #access_code (le libellé accessible varie selon la version du thème).
     */
    public LoginPage fillUsername(String username) {
        page.locator("#access_code").fill(username);
        return this;
    }

    /**
     * Remplit le champ mot de passe (id stable #password).
     */
    public LoginPage fillPassword(String password) {
        page.locator("#password").fill(password);
        return this;
    }

    /**
     * Clique sur le bouton de connexion.
     */
    public void clickLogin() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(LOGIN_BUTTON)).click();
    }

    /**
     * Effectue une connexion complète.
     */
    public AdminMenuPage loginAs(String username, String password) {
        fillUsername(username);
        fillPassword(password);
        clickLogin();
        return new AdminMenuPage(page, baseUrl);
    }

    /**
     * Vérifie si un message d'erreur est affiché.
     */
    public boolean hasErrorMessage() {
        return page.locator(".card-status-start.bg-danger").isVisible();
    }

    /**
     * Récupère le texte du message d'erreur.
     */
    public String getErrorMessage() {
        return page.locator(".card-status-start.bg-danger").textContent();
    }
}
