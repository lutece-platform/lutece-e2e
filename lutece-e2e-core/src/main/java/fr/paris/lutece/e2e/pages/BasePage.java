package fr.paris.lutece.e2e.pages;

import com.microsoft.playwright.Page;
import fr.paris.lutece.e2e.core.BrowserManager;

/**
 * Classe de base pour tous les Page Objects.
 */
public abstract class BasePage {

    protected final BrowserManager browser;

    protected BasePage(BrowserManager browser) {
        this.browser = browser;
    }

    protected Page page() {
        return browser.getPage();
    }

    protected String baseUrl() {
        return browser.getBaseUrl();
    }

    protected void waitForLoad() {
        browser.waitForLoad();
    }

    protected void navigate(String relativePath) {
        browser.navigate(relativePath);
    }
}
