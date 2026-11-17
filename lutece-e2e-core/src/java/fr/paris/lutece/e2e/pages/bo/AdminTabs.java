package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Resolution des onglets du back office, quel que soit leur markup.
 *
 * <p>La macro {@code @tabLink} du core ne rend pas tous les onglets de la meme facon :</p>
 * <ul>
 *   <li>onglet intra-page ({@code href="#panneau"}) : {@code role="tab"} + {@code aria-selected} ;</li>
 *   <li>onglet qui ouvre un autre ecran ({@code href} vers une JSP) : depuis LUT-33387, simple lien
 *       de navigation {@code a.nav-link}, l'onglet courant portant {@code aria-current="page"}.
 *       Avant LUT-33387, ces liens portaient eux aussi {@code role="tab"}.</li>
 * </ul>
 *
 * <p>Un {@code getByRole(TAB)} seul ne trouve donc plus les onglets de navigation (workflow,
 * formulaire, etape, multivue). Les deux formes sont acceptees ici pour rester compatible avec les
 * cores anterieurs.</p>
 */
public final class AdminTabs {

    private static final String NAV_TAB_LINKS = "ul.nav-tabs a.nav-link, ul.card-header-tabs a.nav-link";

    private AdminTabs() {
    }

    /**
     * Onglet dont l'intitule contient {@code name} (meme semantique que {@code getByRole} : sous-chaine,
     * casse ignoree).
     */
    public static Locator tab(Page page, String name) {
        return page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(name))
            .or(page.locator(NAV_TAB_LINKS).filter(new Locator.FilterOptions().setHasText(name)))
            .first();
    }

    /**
     * Indique si l'onglet est l'onglet courant ({@code aria-selected} pour un onglet intra-page,
     * {@code aria-current="page"} pour un onglet de navigation).
     */
    public static boolean isActive(Locator tab) {
        return "true".equals(tab.getAttribute("aria-selected"))
            || "page".equals(tab.getAttribute("aria-current"));
    }
}
