package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/** Helpers Playwright partages par les briques macro Unittree (ancien style JSP-par-operation). */
public final class UnittreeSupport {

    public static final String UT = "/jsp/admin/plugins/unittree/";

    private UnittreeSupport() {}

    public static void navigate(UnittreeContext ctx, String relativeUrl) {
        ctx.page.navigate(ctx.baseUrl + relativeUrl);
        ctx.page.waitForLoadState();
    }

    /**
     * Navigue vers l'arborescence des unites et retourne l'id de l'unite dont le libelle correspond,
     * ou -1. Le lien d'unite est {@code ManageUnits.jsp?idUnit=<id>} (title = libelle).
     */
    public static int extractUnitId(UnittreeContext ctx, String label) {
        navigate(ctx, UT + "ManageUnits.jsp");
        Locator link = ctx.page.locator(
            "a[href*='idUnit=']:has-text('" + label + "'), a[href*='idUnit='][title*='" + label + "']").first();
        try {
            link.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(10_000));
        } catch (RuntimeException notFound) {
            return -1;
        }
        String href = link.getAttribute("href");
        if (href == null || !href.contains("idUnit=")) {
            return -1;
        }
        return Integer.parseInt(href.split("idUnit=")[1].split("&")[0].split("#")[0]);
    }

    public static boolean isTextVisible(Page page, String text) {
        try {
            Locator loc = page.getByText(text);
            return loc.count() > 0 && loc.first().isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
