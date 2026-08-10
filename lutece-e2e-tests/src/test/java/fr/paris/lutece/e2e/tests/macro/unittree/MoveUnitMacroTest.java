package fr.paris.lutece.e2e.tests.macro.unittree;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.UnittreeContext;
import fr.paris.lutece.e2e.tests.macro.UnittreeSupport;
import fr.paris.lutece.e2e.tests.macro.data.UnitDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Brique macro : deplacer une unite (sous-arbre) sous un autre parent. Flux best-effort. */
@Epic("Unittree")
@Feature("Hierarchie")
@Story("Deplacer une unite")
@Tag("macro")
@Tag("unittree")
@Tag("brick")
public class MoveUnitMacroTest extends MacroTest {

    @Step("Deplacer une unite sous un autre parent")
    public static void run(UnittreeContext ctx, int movedIndex, int newParentIndex) {
        Assertions.assertTrue(ctx.units.size() > Math.max(movedIndex, newParentIndex),
            "Les unites a deplacer et le nouveau parent doivent exister");
        UnittreeContext.UnitRef moved = ctx.units.get(movedIndex);
        UnittreeContext.UnitRef newParent = ctx.units.get(newParentIndex);
        Page page = ctx.page;

        UnittreeSupport.navigate(ctx, UnittreeSupport.UT + "MoveSubTree.jsp?idUnit=" + moved.id);
        // Le nouveau parent se choisit via un radio bouton (name='idUnitParent', value=id), pas un select.
        Locator parentRadio = page.locator(
            "input[type=radio][name='idUnitParent'][value='" + newParent.id + "']");
        Assertions.assertTrue(parentRadio.count() > 0,
            "Radio du nouveau parent id=" + newParent.id + " introuvable dans le formulaire de deplacement");
        parentRadio.first().check();

        // Validation du deplacement : bouton submit name='move' (fallback sur un submit generique).
        Locator submit = page.locator("button[name='move']");
        if (submit.count() == 0) {
            submit = page.locator("button[type='submit'], input[type='submit']");
        }
        Assertions.assertTrue(submit.count() > 0 && submit.first().isVisible(),
            "Bouton de validation du deplacement introuvable");
        submit.first().click();
        page.waitForLoadState();

        Assertions.assertFalse(page.url().contains("MoveSubTree.jsp"),
            "Le deplacement aurait du rediriger hors de MoveSubTree ; url: " + page.url());
        moved.parentId = newParent.id;
    }

    @Test
    @DisplayName("Deplacer une unite sous une autre (auto-provisionnement de 2 unites)")
    void standalone() {
        login();
        UnittreeContext ctx = new UnittreeContext(page, BASE_URL, newSuffix());
        CreateUnitMacroTest.run(ctx, UnitDataSet.of("Service Cible"));
        CreateUnitMacroTest.run(ctx, UnitDataSet.of("Service A Deplacer"));
        run(ctx, 1, 0);
    }
}
