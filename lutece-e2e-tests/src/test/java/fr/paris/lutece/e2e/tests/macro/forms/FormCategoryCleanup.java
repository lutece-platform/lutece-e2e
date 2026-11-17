package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;

import java.util.ArrayList;

/** Nettoyage best-effort des categories de formulaire creees par un standalone (§4-bis.8). Pas une brique. */
final class FormCategoryCleanup {

    private FormCategoryCleanup() {}

    static void removeAll(FormsContext ctx) {
        for (Integer ignored : new ArrayList<>(ctx.categoryIds)) {
            try {
                RemoveFormCategoryMacroTest.run(ctx);
            } catch (Throwable t) {
                return;
            }
        }
    }
}
