package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Helpers Playwright partages par les briques macro Forms : navigation par URL et extraction
 * d'identifiants depuis les pages d'administration (mirroir des patterns existants des Page Objects).
 */
public final class MacroSupport {

    public static final String FORMS = "/jsp/admin/plugins/forms/";

    private MacroSupport() {}

    public static void navigate(FormsContext ctx, String relativeUrl) {
        ctx.page.navigate(ctx.baseUrl + relativeUrl);
        ctx.page.waitForLoadState();
        dismissCookieBanner(ctx.page);
    }

    /**
     * Neutralise la banniere de consentement cookies du front-office (qui recouvre la page et
     * intercepte les interactions). No-op sur les pages d'administration (banniere absente).
     * Best-effort : ne leve jamais et ne bloque pas.
     */
    public static void dismissCookieBanner(Page page) {
        // La banniere n'existe que sur le front-office (pages /jsp/site) et est injectee en JS
        // APRES l'evenement load : on attend brievement son apparition avant de la fermer.
        if (!page.url().contains("/jsp/site")) {
            return;
        }
        try {
            // Il peut exister plusieurs boutons "Tout refuser" (un cache dans un template + un
            // visible) : on attend l'apparition puis on clique le PREMIER VISIBLE.
            Locator candidates = page.locator(
                "button:has-text('Tout refuser'), button:has-text('Tout accepter')");
            candidates.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(3000));
            int n = candidates.count();
            for (int i = 0; i < n; i++) {
                Locator b = candidates.nth(i);
                if (b.isVisible()) {
                    b.click(new Locator.ClickOptions().setTimeout(4000));
                    page.waitForLoadState();
                    return;
                }
            }
        } catch (RuntimeException ignored) {
            // banniere absente / deja fermee (cookie deja pose) : rien a faire
        }
    }

    /**
     * Navigue vers la liste des formulaires et retourne l'id du formulaire dont le titre correspond,
     * ou -1 si absent.
     */
    public static int extractFormId(FormsContext ctx, String formTitle) {
        // ManageForms est pagine cote serveur (50/page par defaut) : on force une grande taille de
        // page pour que TOUS les formulaires soient dans le DOM, sinon les plus recents (au-dela de
        // la 1re page) sont invisibles quand la liste grossit.
        navigate(ctx, FORMS + "ManageForms.jsp?view=manageForms&items_per_page=100000");
        // La liste est filtree cote client (LuteceSearchList) : tous les formulaires sont dans le
        // DOM. On attend qu'au moins un lien de formulaire soit attache, puis on parcourt les liens
        // id_form et on retient celui dont le texte ou l'attribut title contient le titre cible.
        Locator anyFormLink = ctx.page.locator("a[href*='id_form=']").first();
        try {
            anyFormLink.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED)
                .setTimeout(15_000));
        } catch (RuntimeException notFound) {
            return -1;
        }
        for (Locator link : ctx.page.locator("a[href*='id_form=']").all()) {
            String text = safe(link.textContent());
            String title = safe(link.getAttribute("title"));
            if (text.contains(formTitle) || title.contains(formTitle)) {
                return parseId(link.getAttribute("href"), "id_form=");
            }
        }
        return -1;
    }

    /**
     * Navigue vers la gestion des etapes d'un formulaire et retourne l'id de l'etape dont le nom
     * correspond (derniere occurrence, comme les Page Objects existants).
     */
    public static int extractStepId(FormsContext ctx, int formId, String stepName) {
        navigate(ctx, FORMS + "ManageSteps.jsp?view=manageSteps&id_form=" + formId);
        Locator stepLink = ctx.page.locator("a.searchable",
            new Page.LocatorOptions().setHasText(stepName)).last();
        String href = stepLink.getAttribute("href");
        return parseId(href, "id_step=");
    }

    private static int parseId(String href, String key) {
        if (href == null || !href.contains(key)) {
            return -1;
        }
        return Integer.parseInt(href.split(key)[1].split("&")[0].split("#")[0]);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
