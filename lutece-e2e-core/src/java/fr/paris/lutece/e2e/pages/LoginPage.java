package fr.paris.lutece.e2e.pages;

import com.microsoft.playwright.options.AriaRole;
import fr.paris.lutece.e2e.core.BrowserManager;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Page Object pour la page de connexion admin Lutece.
 * Basé sur l'implémentation fonctionnelle de lutece-e2e-tests-bo3.
 */
@Dependent
public class LoginPage extends BasePage {

    // ARIA Role locators (used in working implementation)
    private static final String USERNAME_FIELD = "Code d'accès. *";
    private static final String PASSWORD_FIELD = "Mot de passe *";
    private static final String LOGIN_BUTTON = "Se connecter";

    @Inject
    public LoginPage(BrowserManager browser) {
        super(browser);
    }

    public LoginPage navigateToLogin() {
        navigate("/jsp/admin/AdminLogin.jsp");
        return this;
    }

    public LoginPage fillUsername(String username) {
        // Utiliser le sélecteur ID directement (plus fiable)
        var field = page().locator("#access_code");
        field.waitFor();
        field.fill(username);
        return this;
    }

    public LoginPage fillPassword(String password) {
        // Utiliser le sélecteur ID directement (type=password n'a pas de rôle ARIA textbox)
        var field = page().locator("#password");
        field.waitFor();
        field.fill(password);
        return this;
    }

    public void clickLogin() {
        var button = page().getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(LOGIN_BUTTON));
        button.waitFor();
        button.click();
    }

    public void dismissWarningIfPresent() {
        try {
            page().getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("OK"))
                    .click(new com.microsoft.playwright.Locator.ClickOptions().setTimeout(2000));
        } catch (Exception e) {
            // Pas de message d'avertissement
        }
    }

    public void login(String username, String password) {
        navigateToLogin();
        dismissWarningIfPresent();
        fillUsername(username);
        fillPassword(password);
        clickLogin();
        waitForLoad();
    }

    public boolean hasErrorMessage() {
        // Vérifier si on est sur une page d'erreur (AdminMessage) avec un indicateur danger
        return page().locator(".card-status-start.bg-danger").isVisible()
            || page().url().contains("AdminMessage");
    }

    public String getErrorMessage() {
        // Le message est dans le card-body, pas dans card-status-start
        var cardBody = page().locator(".card-body p");
        if (cardBody.isVisible()) {
            return cardBody.textContent().trim();
        }
        // Fallback: chercher tout message visible
        var alertDanger = page().locator(".alert-danger, .text-danger");
        if (alertDanger.first().isVisible()) {
            return alertDanger.first().textContent().trim();
        }
        return "";
    }

    public boolean isLoginPage() {
        return page().url().contains("AdminLogin");
    }
}
