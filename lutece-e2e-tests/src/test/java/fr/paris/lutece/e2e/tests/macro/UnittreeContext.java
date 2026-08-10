package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexte partage explicitement entre briques macro du plugin Unittree (ancien style JSP).
 *
 * <p>Porte le {@link Page}, le {@code baseUrl}, un suffixe unique par run, et les unites creees
 * (id + libelle + id parent). Racine du site : id_unit = 0.</p>
 */
public class UnittreeContext {

    /** Id de l'unite racine du site. */
    public static final int ROOT_UNIT = 0;

    public final Page page;
    public final String baseUrl;
    public final String runSuffix;

    public final List<UnitRef> units = new ArrayList<>();

    public UnittreeContext(Page page, String baseUrl, String runSuffix) {
        this.page = page;
        this.baseUrl = baseUrl;
        this.runSuffix = runSuffix;
    }

    /** Reference d'une unite creee. */
    public static class UnitRef {
        public int id;
        public String label;
        public int parentId;

        public UnitRef() {}

        public UnitRef(int id, String label, int parentId) {
            this.id = id;
            this.label = label;
            this.parentId = parentId;
        }
    }

    public UnitRef unit(int index) {
        return units.get(index);
    }

    public UnitRef lastUnit() {
        return units.get(units.size() - 1);
    }
}
