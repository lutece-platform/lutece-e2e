package fr.paris.lutece.e2e.tests.macro.scenarios;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.data.UnitDataSet;
import fr.paris.lutece.e2e.tests.macro.data.UserAssignmentDataSet;
import fr.paris.lutece.e2e.tests.macro.unittree.AddUsersToUnitMacroTest;
import fr.paris.lutece.e2e.tests.macro.unittree.CreateUnitMacroTest;

/**
 * Entites organisationnelles sur lesquelles un workflow peut s'appuyer.
 *
 * <p>Fragment de scenario reutilisable : toute suite ayant besoin d'une organisation l'obtient
 * ici, sans la redecrire. Les libelles sont exposes pour que les controles d'une suite puissent
 * s'y referer.</p>
 */
public final class OrganisationScenario {

    public static final String UNITE_DIRECTION = "Direction des demarches";

    public static final String UNITE_SERVICE = "Service instruction";


    private OrganisationScenario() {
    }

/**
     * Entites organisationnelles et affectation d'un agent.
     *
     * <p>Les deux unites sont creees a la racine plutot qu'imbriquees : la page de creation sous
     * parent depend d'un identifiant que la liste des unites n'expose pas de maniere fiable selon
     * les sites, ce qui rendait la brique tributaire de l'environnement sans rien apporter au
     * scenario — l'affectation d'une ressource a une entite, elle, est bien couverte.</p>
     *
     * @param page   page Playwright pilotant le navigateur
     * @param baseUrl adresse du site cible
     * @param suffix suffixe unique du run, pour des libelles non collisionnants
     * @return le contexte unittree alimente
     */
    @Step("Mettre en place l'organisation")
    public static UnittreeContext construire(Page page, String baseUrl, String suffix) {
        UnittreeContext units = new UnittreeContext(page, baseUrl, suffix);
        CreateUnitMacroTest.run(units, UnitDataSet.of(UNITE_DIRECTION));
        CreateUnitMacroTest.run(units, UnitDataSet.of(UNITE_SERVICE));
        AddUsersToUnitMacroTest.run(units, UserAssignmentDataSet.defaults());
        return units;
    }
}
