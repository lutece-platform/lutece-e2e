package fr.paris.lutece.e2e.tests.suites;

import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.data.*;
import fr.paris.lutece.e2e.tests.macro.unittree.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Scenario metier : structurer l'organisation d'une direction et y affecter un agent instructeur.
 *
 * <p>Enchainement : creation d'une direction et de ses deux services -> affectation d'un agent au
 * service d'instruction -> reorganisation de la hierarchie (les deux services rattaches) -> renommage
 * du service porteur.</p>
 *
 * <p>Les trois unites sont d'abord creees a la racine puis rattachees par deplacement : c'est la
 * hierarchie telle qu'un chef de service la construit reellement (creation a plat, puis rattachement),
 * et cela reste le seul chemin ou {@code CreateUnit} peut relire l'unite dans l'arborescence racine.</p>
 *
 * <p>Deliberement hors scenario : {@code RemoveUnit}, qui cible {@code ctx.lastUnit()} — ici le service
 * porteur, devenu un noeud avec enfant, alors que la brique ne sait supprimer qu'une feuille.</p>
 *
 * <p>Execution :</p>
 * <pre>
 *   mvn -o test -pl lutece-e2e-tests \
 *     -Dtest=fr.paris.lutece.e2e.tests.suites.OrganisationServicesSuite \
 *     -Dlutece.base.url=http://localhost:9080/lutece-site -Dtest.headless=true
 * </pre>
 */
@Epic("Suites metier")
@Feature("Organisation des services")
@Tag("macro")
@Tag("suite")
@DisplayName("MU-01 : organisation des services instructeurs")
public class OrganisationServicesSuite extends MacroTest {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Direction et services crees, agent affecte, hierarchie reorganisee")
    void organisationDesServices() {
        String suffix = newSuffix();
        login();

        UnittreeContext org = new UnittreeContext(page, BASE_URL, suffix);

        // 1) La direction et ses deux services, crees a la racine du site.
        CreateUnitMacroTest.run(org, UnitDataSet.of("Direction des Solidarites"));   // index 0
        CreateUnitMacroTest.run(org, UnitDataSet.of("Service Controle"));            // index 1
        CreateUnitMacroTest.run(org, UnitDataSet.of("Service Instruction"));         // index 2

        // 2) L'agent instructeur. La brique affecte a ctx.lastUnit() : le service d'instruction doit
        //    donc etre la derniere unite creee.
        AddUsersToUnitMacroTest.run(org, UserAssignmentDataSet.of(1));

        // 3) Reorganisation, du bas vers le haut : le controle sous l'instruction, puis le sous-arbre
        //    ainsi forme sous la direction. L'ordre inverse echoue : MoveSubTree.jsp est un explorateur
        //    d'arborescence qui n'offre en radio que les unites du niveau courant (les filles de la
        //    racine), donc une unite deja rattachee n'est plus proposable comme parent.
        MoveUnitMacroTest.run(org, 1, 2);
        MoveUnitMacroTest.run(org, 2, 0);

        // 4) Le service porteur change de nom (cible ctx.lastUnit() = le service d'instruction).
        ModifyUnitMacroTest.run(org, UnitDataSet.of("Service Instruction et Controle"));
    }
}
