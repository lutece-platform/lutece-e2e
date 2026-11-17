package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import fr.paris.lutece.e2e.pages.bo.LoginPage;
import fr.paris.lutece.e2e.tests.macro.forms.RemoveFormCategoryMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.RemoveFrontUserBOMacroTest;
import fr.paris.lutece.e2e.tests.macro.forms.UnpublishFormMacroTest;
import fr.paris.lutece.e2e.tests.macro.workflow.RemoveWorkflowMacroTest;
import io.qameta.allure.Step;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lectures d'etat partagees par les briques de non-regression Forms (option C : briques Verify* placees
 * derriere les briques existantes). Chaque lecture asserte d'abord un marqueur stable de l'ecran, pour
 * qu'une page d'erreur ou de login ne passe jamais pour une liste vide (SKILL-macro-plugin §4-bis.1).
 *
 * <p>Selecteurs releves sur le banc lpe2e forms 4.0.2-SNAPSHOT le 2026-10-27 (docs/TESTS-MACRO-FORMS-NONREG.md).</p>
 */
public final class FormsNonRegSupport {

    private FormsNonRegSupport() {}

    /** Textes feuilles du titre {@code h3} d'une carte d'etape : ses badges (« Initiale », « Finale »). */
    private static final String BADGES_JS = "h => [...h.querySelectorAll('span, .badge')].filter(s => s.children.length === 0)"
        + ".map(s => (s.innerText || s.textContent || '').replace(/\\s+/g, ' ').trim()).filter(t => t.length > 0)";

    /**
     * Badges exacts (« Initiale », « Finale ») de l'etape dans la liste des etapes du formulaire. Le badge est
     * un {@code @tag} rendu dans le titre {@code h3} de la carte {@code #step_<id>} (manage_steps.html).
     */
    public static Set<String> stepBadges(FormsContext ctx, int stepId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageSteps.jsp?view=manageSteps&id_form=" + ctx.formId);
        Locator card = ctx.page.locator("#step_" + stepId);
        Assertions.assertTrue(card.count() == 1 && card.isVisible(),
            "La carte #step_" + stepId + " doit etre presente dans la liste des etapes du formulaire " + ctx.formId);
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) card.locator("h3").first().evaluate(BADGES_JS);
        return new LinkedHashSet<>(texts);
    }

    /**
     * Transitions partant de l'etape : id -> textes feuilles (normalises) de leur carte. L'id est lu, par
     * parametre strict, sur le lien de suppression {@code view=confirmRemoveTransition&id_transition=N}
     * (manage_transitions.html). Les ids {@code modifyT__step_..} ne servent pas : le JS de la page retire les
     * div offcanvas apres le chargement. La remontee vers la carte s'arrete au premier {@code .card},
     * {@code tr} ou {@code .list-group-item} ; pas {@code li}, car le lien est lui-meme dans un {@code li} de
     * menu deroulant.
     */
    public static Map<Integer, List<String>> transitionsFrom(FormsContext ctx, int fromStepId) {
        // Amorce la vue liste : le JspBean ne peuple son formulaire courant que par elle (cf. CreateTransition).
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "ManageTransitions.jsp?view=manageTransitions&id_step=" + fromStepId);
        Assertions.assertEquals(1, ctx.page.locator("#modify_step").count(),
            "L'ecran des transitions de l'etape " + fromStepId + " doit afficher le formulaire #modify_step");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) ctx.page.evaluate(
            "() => [...document.querySelectorAll(\"a[href*='view=confirmRemoveTransition']\")].map(a => {"
                + "  const card = a.closest('.card, tr, .list-group-item') || a.parentElement;"
                + "  const leaves = [...card.querySelectorAll('*')].filter(x => x.children.length === 0 && !x.closest('script, style'))"
                + "   .map(x => (x.innerText || x.textContent || '').replace(/\\s+/g, ' ').trim()).filter(t => t.length > 0);"
                + "  return { href: a.getAttribute('href'), leaves: leaves }; })");
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer id = intParam((String) row.get("href"), "id_transition");
            Assertions.assertNotNull(id, "Lien de suppression de transition sans id_transition : " + row.get("href"));
            @SuppressWarnings("unchecked")
            List<String> leaves = (List<String>) row.get("leaves");
            result.put(id, leaves);
        }
        return result;
    }

    /**
     * Ids des reponses listees dans la multivue du formulaire courant, lus sur l'attribut
     * {@code data-url="...&id_form_response=N"} des lignes (include_manage_multiview_forms_table.html). La liste est
     * paginee (50 par defaut, {@code AbstractJspBean}) : la lecture force une page unique ({@code items_per_page}).
     */
    public static Set<Integer> multiviewResponseIds(FormsContext ctx) {
        MacroSupport.navigate(ctx,
            MacroSupport.FORMS + "MultiviewForms.jsp?current_selected_panel=forms&id_form=" + ctx.formId
                + "&items_per_page=100000&page_index=1");
        Assertions.assertEquals(1, ctx.page.locator("form#searchForm").count(),
            "La multivue doit afficher son formulaire de recherche #searchForm");
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) ctx.page.locator("[data-url*='id_form_response=']").evaluateAll(
            "els => els.map(e => e.getAttribute('data-url'))");
        Set<Integer> ids = new LinkedHashSet<>();
        for (String url : urls) {
            Integer id = intParam(url, "id_form_response");
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Texte de la ligne de la reponse dans la multivue (colonnes affichees, dont l'etat de workflow), lue sur
     * l'element qui porte {@code data-url=...id_form_response=N}. Null si la reponse n'est pas listee.
     */
    public static String multiviewRowText(FormsContext ctx, int responseId) {
        multiviewResponseIds(ctx);
        return (String) ctx.page.evaluate("id => { const e = [...document.querySelectorAll(\"[data-url*='id_form_response=']\")]"
            + ".find(x => new URLSearchParams(x.getAttribute('data-url').split('?')[1]).get('id_form_response') === String(id));"
            + " return e ? (e.closest('tr') || e).innerText.replace(/\\s+/g, ' ').trim() : null; }", responseId);
    }

    /**
     * Lance l'indexation incrementale des reponses depuis l'ecran « Indexation des contenus Forms »
     * ({@code ManageFormsSearchIndexation.jsp}, bouton {@code name=incremental}) : chemin reel, token compris.
     */
    public static void triggerIncrementalIndexing(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageFormsSearchIndexation.jsp");
        Locator button = ctx.page.locator("button[name='incremental']");
        Assertions.assertEquals(1, button.count(),
            "L'ecran d'indexation Forms doit proposer le bouton d'indexation incrementale");
        button.click();
        ctx.page.waitForLoadState();
    }

    /** Valeur entiere du parametre {@code key} d'une URL, par egalite stricte du nom (jamais un contains). */
    public static Integer intParam(String url, String key) {
        if (url == null) {
            return null;
        }
        String query = url.contains("?") ? url.substring(url.indexOf('?') + 1) : url;
        for (String pair : query.replace("&amp;", "&").split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                try {
                    return Integer.parseInt(pair.substring(eq + 1).replaceAll("[^0-9].*$", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Enregistre pour le nettoyage, <b>avant</b> toute assertion de la brique, les formulaires nouveaux de ce run (titre
     * qui contient {@code ctx.formTitle}, porteur du suffixe d'execution ; les formulaires d'un autre run sur le banc
     * partage ne sont jamais pris). Revue adversariale du 2026-10-30, constat 9 : une copie enregistree apres les
     * assertions restait sur le banc quand l'une d'elles echouait.
     */
    public static void registerCreatedForms(FormsContext ctx, List<Integer> fresh) {
        for (int id : fresh) {
            String title = formTitle(ctx, id);
            if (ctx.formTitle != null && title.contains(ctx.formTitle)) {
                ctx.createdForms.put(id, title);
            }
        }
    }

    /** Ids de {@code all} absents de {@code known} : identite par difference d'ensembles (§4-bis.3). */
    public static List<Integer> newIds(Set<Integer> all, List<Integer> known) {
        List<Integer> fresh = new ArrayList<>(all);
        fresh.removeAll(known);
        return fresh;
    }

    /**
     * Etapes du formulaire courant : id -> titre, lus sur les cartes {@code #step_<id>} de la liste des etapes
     * (manage_steps.html). Le titre est le texte du {@code h3} hors badges (noeud texte direct). Marqueur d'ecran :
     * le formulaire d'import d'etape {@code #importJsonModal} porteur de {@code id_form}.
     */
    public static Map<Integer, String> stepsOfForm(FormsContext ctx) {
        return stepsOfForm(ctx, ctx.formId);
    }

    /** Variante de {@link #stepsOfForm(FormsContext)} pour un autre formulaire (copie, import). */
    public static Map<Integer, String> stepsOfForm(FormsContext ctx, int formId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageSteps.jsp?view=manageSteps&id_form=" + formId);
        Assertions.assertEquals(1,
            ctx.page.locator("form#importJsonModal input[name='id_form'][value='" + formId + "']").count(),
            "La liste des etapes du formulaire " + formId + " doit etre affichee");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) ctx.page.evaluate(
            "() => [...document.querySelectorAll('[id^=\"step_\"]')].filter(c => /^step_[0-9]+$/.test(c.id)).map(c => {"
                + "  const h = c.querySelector('h3');"
                + "  const k = h ? h.cloneNode(true) : null;"
                + "  if (k) { k.querySelectorAll('.badge').forEach(b => b.remove()); }"
                + "  return { id: c.id, title: k ? (k.textContent || '').replace(/\\s+/g, ' ').trim() : '' }; })");
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map<String, Object> card : cards) {
            result.put(Integer.parseInt(((String) card.get("id")).substring("step_".length())), (String) card.get("title"));
        }
        return result;
    }

    /**
     * Composant d'une etape lu sur la liste des questions : question ou groupe, {@code id} = id de la question ou
     * du groupe, {@code display} / {@code parent} = id d'affichage et id d'affichage du groupe parent (0 = racine),
     * {@code icon} = classe {@code ti-*} du type (questions), {@code conditional} / {@code validated} = indicateurs de
     * condition d'affichage et de controle de validation.
     */
    public record Composite(boolean group, int id, int display, int parent, String title, String icon,
                            boolean conditional, boolean validated) {}

    /**
     * Composants de l'etape, questions et groupes, dans l'ordre d'affichage (manage_questions.html). Chaque composant
     * emet d'abord ses champs caches {@code own_display_id_composite} / {@code own_display_id_parent} dans un
     * {@code li[data-url=...id_question=N]}, puis son contenu : {@code h4} pour une question (icone du type, titre
     * {@code .searchable}), carte {@code .card-question#group-<id_display>} pour un groupe. Le gabarit referme mal son
     * balisage apres un groupe : un groupe qui suit un groupe, ou une question qui suit un groupe, sort de son
     * {@code li} (releve du 2026-10-30, un groupe y etait lu comme une question sans titre) ; avec des groupes imbriques,
     * il referme jusqu'a {@code #question-list} lui-meme et le contenu des composants suivants sort de la liste (releve du
     * 2026-11-10 : G1 { G2 { Qa }, Q3, Q4, Q5 }, le {@code h4} de Q5 hors de la liste). L'imbrication du DOM ne vaut donc
     * rien : chaque champ cache est apparie au contenu qui le suit dans l'ordre du <b>document</b>, a partir du premier
     * champ cache (les contenus qui le precedent ne sont pas des composants), en ne retenant que les titres de question
     * ({@code h4} portant {@code .searchable}) et les cartes de groupe ; l'appartenance a un groupe vient de
     * {@code own_display_id_parent}. Un contenu manquant ou en trop reste un echec. Marqueur d'ecran : le champ cache
     * {@code id_step} de l'etape.
     */
    public static List<Composite> compositesOfStep(FormsContext ctx, int stepId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);
        Assertions.assertTrue(ctx.page.locator("form input[name='id_step'][value='" + stepId + "']").count() >= 1,
            "La liste des questions de l'etape " + stepId + " doit etre affichee");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) ctx.page.evaluate(
            "() => { const n = e => ((e && e.textContent) || '').replace(/\\s+/g, ' ').trim(); const rows = [];"
                + " for (const e of document.querySelectorAll(\"input[name='own_display_id_composite'],"
                + "   h4:has(.searchable), .card-question[id^='group-']\")) {"
                + "  if (e.tagName === 'INPUT') {"
                + "   const p = e.parentElement.querySelector(\":scope > input[name='own_display_id_parent']\");"
                + "   rows.push({ display: e.value, parent: p ? p.value : null, url: e.parentElement.getAttribute('data-url'), contents: [] });"
                + "  } else if (rows.length === 0) {"
                + "   continue;"
                + "  } else if (e.tagName === 'H4') {"
                + "   rows[rows.length - 1].contents.push({ kind: 'Q', title: n(e.querySelector('.searchable')),"
                + "     icon: [...((e.querySelector(':scope > i') || {}).classList || [])].filter(c => c.startsWith('ti-')).join(' ') });"
                + "  } else {"
                + "   rows[rows.length - 1].contents.push({ kind: 'G', title: n(e.querySelector('.card-header h3')), card: e.id }); } }"
                + " for (const r of rows) {"
                + "  r.conditional = !!document.querySelector(\"a.btn-outline-secondary[href='#cond-\" + r.display + \"']\");"
                + "  r.validated = !!document.querySelector(\"a.btn-outline-secondary[href='#validate-\" + r.display + \"']\"); }"
                + " return rows; }");
        List<Composite> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) row.get("contents");
            Assertions.assertEquals(1, contents.size(), "Composant de l'etape " + stepId + " sans contenu unique : " + row);
            Map<String, Object> content = contents.get(0);
            Integer id = intParam((String) row.get("url"), "id_question");
            Assertions.assertTrue(id != null && row.get("display") != null && row.get("parent") != null,
                "Composant de l'etape " + stepId + " sans id_question ni id d'affichage : " + row);
            boolean group = "G".equals(content.get("kind"));
            if (group) {
                Assertions.assertEquals("group-" + row.get("display"), content.get("card"), "Carte de groupe mal appariee : " + row);
            }
            result.add(new Composite(group, id, Integer.parseInt((String) row.get("display")),
                Integer.parseInt((String) row.get("parent")), (String) content.get("title"),
                group ? "" : (String) content.get("icon"), Boolean.TRUE.equals(row.get("conditional")),
                Boolean.TRUE.equals(row.get("validated"))));
        }
        return result;
    }

    /** Questions de l'etape : id -> titre, ordre d'affichage, groupes exclus ({@link #compositesOfStep}). */
    public static Map<Integer, String> questionsOfStep(FormsContext ctx, int stepId) {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Composite composite : compositesOfStep(ctx, stepId)) {
            if (!composite.group()) {
                result.put(composite.id(), composite.title());
            }
        }
        return result;
    }

    /**
     * Href exact d'un lien de la page dont le parametre {@code key} vaut {@code id} (comparaison stricte via
     * {@link #intParam}), parmi les liens qui contiennent {@code marker}. Null si aucun ; echec si plusieurs.
     */
    public static String linkHref(FormsContext ctx, String marker, String key, int id) {
        @SuppressWarnings("unchecked")
        List<String> hrefs = (List<String>) ctx.page.locator("a[href*='" + marker + "']")
            .evaluateAll("els => els.map(e => e.getAttribute('href'))");
        List<String> matching = new ArrayList<>();
        for (String href : hrefs) {
            if (Integer.valueOf(id).equals(intParam(href, key)) && !matching.contains(href)) {
                matching.add(href);
            }
        }
        Assertions.assertTrue(matching.size() <= 1, "Plusieurs liens " + marker + " pour " + key + "=" + id + " : " + matching);
        return matching.isEmpty() ? null : matching.get(0);
    }

    /**
     * Clique une entree de menu deroulant comme l'usager : ouvre d'abord le bouton {@code data-bs-toggle=dropdown}
     * le plus proche qui contient l'entree, puis clique l'entree. L'entree doit etre unique.
     */
    public static void clickInDropdown(FormsContext ctx, Locator item, String label) {
        Assertions.assertEquals(1, item.count(), "Entree de menu « " + label + " » attendue une seule fois");
        Locator toggle = item.locator("xpath=ancestor::*[.//*[@data-bs-toggle='dropdown']][1]//*[@data-bs-toggle='dropdown']").first();
        if (!item.isVisible() && toggle.count() == 1) {
            toggle.click();
        }
        item.click();
        ctx.page.waitForLoadState();
    }

    /**
     * Valeurs de liste de la question : id_field -> libelle « titre > valeur », lus sur la carte
     * {@code #manage-field-options} de l'ecran de modification de la question (entries/all_entry_commons.html) :
     * l'id par parametre strict sur l'url de l'offcanvas {@code view=modifyField}, le libelle sur l'attribut
     * {@code title} du {@code span} de la ligne.
     */
    public static Map<Integer, String> listValues(FormsContext ctx, FormsContext.QuestionRef question) {
        Assertions.assertTrue(question.id > 0, "L'id de la question '" + question.title + "' doit etre resolu");
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=modifyQuestion&id_step="
            + question.stepId + "&id_question=" + question.id);
        Assertions.assertEquals(1, ctx.page.locator("#manage-field-options").count(),
            "La question " + question.id + " doit afficher sa liste de reponses (#manage-field-options)");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) ctx.page.evaluate(
            "() => [...document.querySelectorAll('#manage-field-options li')].map(li => {"
                + "  const a = li.querySelector(\"[data-lutece-load-content-url*='view=modifyField']\");"
                + "  const s = li.querySelector('span[title]');"
                + "  return { url: a ? a.getAttribute('data-lutece-load-content-url') : null,"
                + "           label: s ? s.getAttribute('title').replace(/\\s+/g, ' ').trim() : '' }; })");
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer id = intParam((String) row.get("url"), "id_field");
            Assertions.assertNotNull(id, "Valeur de liste sans id_field : " + row);
            result.put(id, (String) row.get("label"));
        }
        return result;
    }

    /**
     * Categories de formulaire : id -> texte de la ligne (« Code : C - Nom : N »), lus sur la liste
     * {@code ManageFormsCategories.jsp} (id par parametre strict sur l'url de l'offcanvas {@code view=modifyCategory}).
     */
    public static Map<Integer, String> formCategories(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageFormsCategories.jsp?view=manageCategories");
        Assertions.assertEquals(1, ctx.page.locator("input[name='search_forms']").count(),
            "La liste des categories de formulaire doit etre affichee");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) ctx.page.evaluate(
            "() => [...document.querySelectorAll(\"[data-lutece-load-content-url*='view=modifyCategory']\")].map(a => {"
                + "  const li = a.closest('li') || a.parentElement;"
                + "  const s = li.querySelector('.searchable');"
                + "  return { url: a.getAttribute('data-lutece-load-content-url'),"
                + "           label: s ? (s.textContent || '').replace(/\\s+/g, ' ').trim() : '' }; })");
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer id = intParam((String) row.get("url"), "id_category");
            Assertions.assertNotNull(id, "Categorie sans id_category : " + row);
            result.put(id, (String) row.get("label"));
        }
        return result;
    }

    /**
     * Confirme un AdminMessage de confirmation : soumet le formulaire dont l'action porte {@code action=<name>}
     * (l'action et son token sont rendus par le serveur). Echoue si la page n'est pas cette confirmation.
     */
    public static void confirmAdminMessage(FormsContext ctx, String actionName) {
        Locator submit = ctx.page.locator("form[action*='action=" + actionName + "'] button[type='submit']");
        Assertions.assertEquals(1, submit.count(),
            "Confirmation « " + actionName + " » attendue (url " + ctx.page.url() + ", alertes : "
                + ctx.page.locator(".alert, h1, h2").allInnerTexts() + ")");
        submit.click();
        ctx.page.waitForLoadState();
    }

    /**
     * Placement des questions de l'etape : id_question -> {id_display, id_display du parent (0 = racine)}, lus
     * sur les champs caches {@code own_display_id_composite} / {@code own_display_id_parent} ({@link #compositesOfStep}).
     * Groupes exclus.
     */
    public static Map<Integer, int[]> questionPlacement(FormsContext ctx, int stepId) {
        Map<Integer, int[]> result = new LinkedHashMap<>();
        for (Composite composite : compositesOfStep(ctx, stepId)) {
            if (!composite.group()) {
                result.put(composite.id(), new int[] {composite.display(), composite.parent()});
            }
        }
        return result;
    }

    /** Groupes de l'etape : id_display -> titre, lus sur les cartes {@code #group-<id_display>} (h3). */
    public static Map<Integer, String> groupsOfStep(FormsContext ctx, int stepId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=manageQuestions&id_step=" + stepId);
        Assertions.assertTrue(ctx.page.locator("form input[name='id_step'][value='" + stepId + "']").count() >= 1,
            "La liste des questions de l'etape " + stepId + " doit etre affichee");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) ctx.page.evaluate(
            "() => [...document.querySelectorAll('.card-question[id^=\"group-\"]')].map(c => ({ id: c.id,"
                + "  title: ((c.querySelector('.card-header h3') || {}).textContent || '').replace(/\\s+/g, ' ').trim() }))");
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map<String, Object> card : cards) {
            result.put(Integer.parseInt(((String) card.get("id")).substring("group-".length())), (String) card.get("title"));
        }
        return result;
    }

    /**
     * Fiche d'une question (ecran {@code view=modifyQuestion}, form {@code #modifyEntryForm}, entries/all_entry_commons.html) :
     * {@code type} = badge du titre de la fiche ; {@code mandatory} = interrupteur {@code #mandatory} (null pour les
     * types sans obligation) ; {@code values} = null sans liste de reponses ({@code #manage-field-options} absent),
     * « ref:<liste> » pour une liste de reference, sinon « titre > valeur » par ligne (« * » = valeur par defaut) ;
     * {@code fields} = id_field -> « titre > valeur ».
     */
    public record QuestionDetail(String type, Boolean mandatory, List<String> values, Map<Integer, String> fields) {}

    /** Lit la fiche de la question ({@link QuestionDetail}). */
    public static QuestionDetail questionDetail(FormsContext ctx, int stepId, int questionId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=modifyQuestion&id_step=" + stepId
            + "&id_question=" + questionId);
        // titre de carte en h3 (core 8.0) ou h2 (core 8.1, releve du 2026-11-17)
        Locator type = ctx.page.locator("#modifyEntryForm :is(h2, h3).card-title > span.badge");
        Assertions.assertEquals(1, type.count(), "La fiche de la question " + questionId + " doit afficher son type");
        @SuppressWarnings("unchecked")
        Map<String, Object> read = (Map<String, Object>) ctx.page.evaluate(
            "() => { const n = t => (t || '').replace(/\\s+/g, ' ').trim(); const m = document.querySelector('#mandatory');"
                + " const box = document.querySelector('#manage-field-options'); let values = null; const fields = {};"
                + " if (box) { const ul = box.querySelector('ul.list-group');"
                + "  if (ul) { values = [...ul.querySelectorAll('li')].map(li => {"
                + "    const s = li.querySelector('span[title]'); const label = s ? n(s.getAttribute('title')) : '';"
                + "    const a = li.querySelector(\"[data-lutece-load-content-url*='view=modifyField']\");"
                + "    if (a) { fields[a.getAttribute('data-lutece-load-content-url')] = label; }"
                + "    return label + (li.querySelector('.badge .ti-check') ? ' *' : ''); });"
                + "  } else { const r = document.querySelector(\"select[name='select_ref_list']\");"
                + "   values = ['ref:' + (r && r.selectedIndex >= 0 ? n(r.options[r.selectedIndex].text) : '?')]; } }"
                + " return { mandatory: m ? m.checked : null, values: values, fields: fields }; }");
        Map<Integer, String> fields = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> rawFields = (Map<String, Object>) read.get("fields");
        for (Map.Entry<String, Object> field : rawFields.entrySet()) {
            Integer id = intParam(field.getKey(), "id_field");
            Assertions.assertNotNull(id, "Valeur de liste sans id_field : " + field);
            fields.put(id, (String) field.getValue());
        }
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) read.get("values");
        return new QuestionDetail(type.textContent().replaceAll("\\s+", " ").trim(), (Boolean) read.get("mandatory"), values, fields);
    }

    /** Iterations d'un groupe, « min..max », lues sur son ecran de modification (modify_group.html). */
    public static String groupIterations(FormsContext ctx, int stepId, int groupId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageQuestions.jsp?view=modifyGroup&id_step=" + stepId
            + "&id_group=" + groupId);
        Locator min = ctx.page.locator("input[name='iterationMin']");
        Locator max = ctx.page.locator("input[name='iterationMax']");
        Assertions.assertTrue(min.count() == 1 && max.count() == 1,
            "L'ecran de modification du groupe " + groupId + " doit afficher ses iterations (url " + ctx.page.url() + ")");
        return min.inputValue() + ".." + max.inputValue();
    }

    /** Transition lue sur la vue graphique : id, etape source, priorite, controles (« validateur : valeur »), arrivee. */
    public record GraphTransition(int id, int from, int priority, List<String> controls, int to) {}

    /** Graphe d'un formulaire : ids des etapes dessinees et transitions. */
    public record FormGraph(Set<Integer> steps, List<GraphTransition> transitions) {}

    /**
     * Vue graphique du formulaire ({@code ManageSteps.jsp?view=graphicView&id_form=F}) : seul ecran qui donne l'id de
     * l'etape d'arrivee d'une transition (la liste des transitions n'en montre que le titre ; l'ecran de modification
     * ne propose pas l'arrivee actuelle, defaut A). Le serveur y ecrit une source Mermaid (FormGraphExportService) que
     * Mermaid remplace par un dessin au chargement : ses deux scripts sont neutralises le temps de la lecture. Lignes
     * lues (releve du 2026-10-30) : {@code S<etape>["[titre]...}, {@code S<etape> --> |" "| T<transition>{{"P<priorite>}
     * suivi d'une ligne « validateur : valeur » par controle, puis {@code "}}}, et {@code T<transition> --> |" "| S<etape>}.
     * Les transitions sont lues en base sans cache, quel que soit le formulaire de l'etape d'arrivee.
     */
    public static FormGraph formGraph(FormsContext ctx, int formId) {
        String mermaid = "**/mermaid.min.js";
        String panZoom = "**/svg-pan-zoom.js";
        ctx.page.route(mermaid, route -> route.fulfill(new Route.FulfillOptions().setContentType("application/javascript")
            .setBody("window.mermaid = { initialize: function() {}, init: function() {} };")));
        ctx.page.route(panZoom, route -> route.fulfill(new Route.FulfillOptions().setContentType("application/javascript")
            .setBody("window.svgPanZoom = function() { return { zoomIn() {}, zoomOut() {}, resetZoom() {} }; };")));
        String source;
        try {
            MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageSteps.jsp?view=graphicView&id_form=" + formId);
            Locator graph = ctx.page.locator("div.mermaid");
            Assertions.assertEquals(1, graph.count(), "La vue graphique du formulaire " + formId + " doit afficher un graphe");
            source = graph.textContent();
        } finally {
            ctx.page.unroute(mermaid);
            ctx.page.unroute(panZoom);
        }
        Assertions.assertTrue(source.trim().startsWith("flowchart"), "Source Mermaid attendue (formulaire " + formId + ") : " + source);
        Set<Integer> steps = new LinkedHashSet<>();
        Matcher node = Pattern.compile("(?m)^S(\\d+)\\[\"").matcher(source);
        while (node.find()) {
            steps.add(Integer.parseInt(node.group(1)));
        }
        Map<Integer, Integer> targets = new HashMap<>();
        Matcher arrival = Pattern.compile("(?m)^T(\\d+) --> \\|\" \"\\| S(\\d+)\\s*$").matcher(source);
        while (arrival.find()) {
            targets.put(Integer.parseInt(arrival.group(1)), Integer.parseInt(arrival.group(2)));
        }
        List<GraphTransition> transitions = new ArrayList<>();
        Matcher start = Pattern.compile("(?s)S(\\d+) --> \\|\" \"\\| T(\\d+)\\{\\{\"P(\\d+)(.*?)\"\\}\\}").matcher(source);
        while (start.find()) {
            int id = Integer.parseInt(start.group(2));
            Assertions.assertTrue(targets.containsKey(id), "Transition T" + id + " sans etape d'arrivee dans le graphe : " + source);
            List<String> controls = Arrays.stream(start.group(4).split("\\R")).map(String::trim).filter(c -> !c.isEmpty()).toList();
            transitions.add(new GraphTransition(id, Integer.parseInt(start.group(1)), Integer.parseInt(start.group(3)),
                controls, targets.get(id)));
        }
        Assertions.assertEquals(targets.size(), transitions.size(), "Transitions du graphe mal lues : " + source);
        return new FormGraph(steps, transitions);
    }

    /** Condition d'affichage lue sur sa fiche : etape et question pilotes (ids), validateur, valeur (id brut et libelle). */
    public record DisplayCondition(int pilotStepId, int pilotQuestionId, String validator, String value, String valueLabel) {}

    /**
     * Conditions d'affichage du composant {@code display} de l'etape, et operateur qui les combine. Chemin reel : vue
     * {@code ManageControls.jsp?view=manageConditionControl&id_target=<id_display>&control_type=CONDITIONAL} (elle remet
     * a zero l'etat de session du controleur), puis la fiche de chaque condition par son lien
     * {@code view=modifyConditionControl&id_control=N} (modify_condition_control.html). La fiche ne montre pas la
     * condition en clair mais ses listes, avec l'option enregistree marquee {@code selected} : etape pilote
     * ({@code id_step}, liste des etapes du formulaire de la question pilote), question pilote ({@code id_question}),
     * validateur, valeur ({@code value} : id de la valeur de liste pour « Valeur exacte »). Les ids permettent de
     * verifier que la condition d'une copie vise bien la copie, ce que les titres ne disent pas.
     */
    public static Map.Entry<String, List<DisplayCondition>> displayConditions(FormsContext ctx, int stepId, int display) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageControls.jsp?view=manageConditionControl&id_step=" + stepId
            + "&id_target=" + display + "&control_type=CONDITIONAL");
        Locator operator = ctx.page.locator("form#modifyLogicalOperator select[name='logical_operator'] option[selected]");
        Assertions.assertEquals(1, ctx.page.locator("form#modifyLogicalOperator").count(),
            "La vue des conditions du composant " + display + " doit etre affichee (url " + ctx.page.url() + ")");
        String logical = operator.count() == 1 ? operator.getAttribute("value") : "?";
        @SuppressWarnings("unchecked")
        List<String> hrefs = (List<String>) ctx.page.locator("a[href*='view=modifyConditionControl'][href*='id_control=']")
            .evaluateAll("els => [...new Set(els.map(e => e.getAttribute('href')))]");
        List<DisplayCondition> conditions = new ArrayList<>();
        for (String href : hrefs) {
            Integer idControl = intParam(href, "id_control");
            Assertions.assertNotNull(idControl, "Lien de condition sans id_control : " + href);
            follow(ctx, href);
            Assertions.assertEquals(1, ctx.page.locator("form#modify_condition_control").count(),
                "La fiche de la condition " + idControl + " doit etre affichee (url " + ctx.page.url() + ")");
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> read = (Map<String, Map<String, String>>) ctx.page.evaluate(
                "() => { const f = document.querySelector('#modify_condition_control');"
                    + " const pick = n => { const o = f.querySelector(\"select[name='\" + n + \"'] option[selected]\");"
                    + "  if (o) { return { value: o.value, text: o.textContent.replace(/\\s+/g, ' ').trim() }; }"
                    + "  const i = f.querySelector(\"input[name='\" + n + \"']\"); return i ? { value: i.value, text: i.value } : { value: '', text: '' }; };"
                    + " return { step: pick('id_step'), question: pick('id_question'), validator: pick('validatorName'), value: pick('value') }; }");
            conditions.add(new DisplayCondition(parseIntOr(read.get("step").get("value")), parseIntOr(read.get("question").get("value")),
                read.get("validator").get("text"), read.get("value").get("value"), read.get("value").get("text")));
        }
        return Map.entry(logical, conditions);
    }

    private static int parseIntOr(String value) {
        return value != null && value.matches("\\d+") ? Integer.parseInt(value) : -1;
    }

    /**
     * Signature de structure d'un formulaire, independante des ids, pour comparer un original et sa copie ou son
     * import (revue adversariale du 2026-10-30, constat 3). Une ligne par etape, dans l'ordre de la liste :
     * <ul>
     *   <li>rang, titre et badges (« Initiale », « Finale ») ;</li>
     *   <li>composants dans l'ordre d'affichage ({@link #compositesOfStep}) : groupe (titre, groupes parents,
     *   iterations) ou question (titre, groupes parents, type, obligation, valeurs de liste, presence d'un controle de
     *   validation, conditions d'affichage avec etape, question et valeur pilotes resolues dans ce formulaire, une
     *   reference restee dans l'original etant marquee « hors formulaire », {@link #displayConditions}) ;</li>
     *   <li>transitions par priorite ({@link #formGraph}) : etape d'arrivee designee par son <b>rang dans ce
     *   formulaire</b> (une arrivee restee dans l'original est marquee « hors formulaire »), controles, une valeur egale
     *   a l'id d'une valeur de liste du formulaire etant remplacee par son libelle.</li>
     * </ul>
     * Les transitions du graphe sont recoupees, par id, avec la liste des transitions de chaque etape.
     */
    public static List<String> structureOf(FormsContext ctx, int formId) {
        Map<Integer, String> steps = stepsOfForm(ctx, formId);
        List<Integer> order = new ArrayList<>(steps.keySet());
        Map<Integer, Set<String>> badges = new HashMap<>();
        for (int stepId : order) {
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) ctx.page.locator("#step_" + stepId + " h3").first().evaluate(BADGES_JS);
            badges.put(stepId, new TreeSet<>(texts));
        }
        FormGraph graph = formGraph(ctx, formId);
        Assertions.assertEquals(new HashSet<>(order), graph.steps(), "Etapes du graphe du formulaire " + formId);

        Map<Integer, String> fieldLabels = new HashMap<>();
        Map<Integer, String> questionTitles = new HashMap<>();
        Map<Integer, Set<Integer>> questionFields = new HashMap<>();
        Map<String, int[]> conditionTokens = new LinkedHashMap<>();
        Map<Integer, List<String>> components = new HashMap<>();
        for (int stepId : order) {
            List<Composite> composites = compositesOfStep(ctx, stepId);
            Map<Integer, Composite> byDisplay = new HashMap<>();
            composites.forEach(c -> byDisplay.put(c.display(), c));
            List<String> lines = new ArrayList<>();
            for (Composite c : composites) {
                String where = "{" + groupPath(c, byDisplay) + "}";
                if (c.group()) {
                    lines.add("G '" + c.title() + "' " + where + " iterations=" + groupIterations(ctx, stepId, c.id()));
                    continue;
                }
                QuestionDetail detail = questionDetail(ctx, stepId, c.id());
                questionTitles.put(c.id(), c.title());
                questionFields.put(c.id(), detail.fields().keySet());
                detail.fields().forEach((id, label) -> fieldLabels.put(id, "'" + c.title() + "' " + label));
                String condition = "";
                if (c.conditional()) {
                    // Resolue apres la lecture de toutes les etapes : la question pilote peut etre plus loin.
                    condition = " #condition" + c.display() + "#";
                    conditionTokens.put(condition, new int[] {stepId, c.display()});
                }
                lines.add("Q '" + c.title() + "' " + where + " [" + detail.type() + "]"
                    + (Boolean.TRUE.equals(detail.mandatory()) ? " obligatoire" : "")
                    + condition + (c.validated() ? " +controle" : "")
                    + (detail.values() == null ? "" : " valeurs=" + detail.values()));
            }
            components.put(stepId, lines);
        }
        for (Map.Entry<String, int[]> token : conditionTokens.entrySet()) {
            Map.Entry<String, List<DisplayCondition>> read = displayConditions(ctx, token.getValue()[0], token.getValue()[1]);
            List<String> described = new ArrayList<>();
            for (DisplayCondition d : read.getValue()) {
                int rank = order.indexOf(d.pilotStepId());
                // Pilote a liste de reponses : la valeur est l'id d'une de ses valeurs ; sinon, valeur brute.
                Set<Integer> pilotFields = questionFields.getOrDefault(d.pilotQuestionId(), Set.of());
                String value = pilotFields.isEmpty() ? "'" + d.value() + "'"
                    : d.value().matches("\\d+") && pilotFields.contains(Integer.parseInt(d.value()))
                        ? fieldLabels.get(Integer.parseInt(d.value()))
                        : "valeur hors formulaire '" + d.valueLabel() + "' (" + d.value() + ")";
                described.add((rank >= 0 ? "#" + rank : "etape hors formulaire") + " "
                    + (questionTitles.containsKey(d.pilotQuestionId()) ? "'" + questionTitles.get(d.pilotQuestionId()) + "'"
                        : "question hors formulaire") + " " + d.validator() + " " + value);
            }
            String replacement = " +condition(" + read.getKey() + ")" + described;
            components.replaceAll((stepId, lines) -> lines.stream().map(l -> l.replace(token.getKey(), replacement)).toList());
        }

        List<String> signature = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            int stepId = order.get(i);
            List<GraphTransition> outgoing = graph.transitions().stream().filter(t -> t.from() == stepId)
                .sorted(Comparator.comparingInt(GraphTransition::priority)).toList();
            Assertions.assertEquals(transitionsFrom(ctx, stepId).keySet(),
                outgoing.stream().map(GraphTransition::id).collect(Collectors.toSet()),
                "Transitions de l'etape " + stepId + " : liste de l'etape / vue graphique du formulaire " + formId);
            List<String> transitions = new ArrayList<>();
            for (GraphTransition t : outgoing) {
                int rank = order.indexOf(t.to());
                List<String> controls = t.controls().stream().map(c -> {
                    String[] parts = c.split(" : ", 2);
                    return parts.length == 2 && parts[1].matches("\\d+") && fieldLabels.containsKey(Integer.parseInt(parts[1]))
                        ? parts[0] + " : " + fieldLabels.get(Integer.parseInt(parts[1])) : c;
                }).toList();
                transitions.add("P" + t.priority() + " -> "
                    + (rank >= 0 ? "#" + rank + " '" + steps.get(t.to()) + "'" : "hors formulaire (etape " + t.to() + ")")
                    + (controls.isEmpty() ? "" : " controles=" + controls));
            }
            signature.add("etape #" + i + " '" + steps.get(stepId) + "' " + badges.get(stepId)
                + " composants=" + components.get(stepId) + " transitions=" + transitions);
        }
        return signature;
    }

    /** Groupes parents du composant, du plus externe au plus proche (« G1 > G2 »), vide a la racine. */
    private static String groupPath(Composite composite, Map<Integer, Composite> byDisplay) {
        List<String> path = new ArrayList<>();
        int parent = composite.parent();
        for (int depth = 0; parent != 0 && depth < 20; depth++) {
            Composite group = byDisplay.get(parent);
            if (group == null) {
                path.add(0, "groupe inconnu " + parent);
                break;
            }
            path.add(0, group.title());
            parent = group.parent();
        }
        return String.join(" > ", path);
    }

    /** Ids des formulaires de la liste {@code ManageForms} (lien « Parametres » {@code view=modifyForm&id_form=F}). */
    public static Set<Integer> formIds(FormsContext ctx) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=manageForms&items_per_page=100000");
        Assertions.assertEquals(1, ctx.page.locator("form#importJsonModal").count(), "La liste des formulaires doit etre affichee");
        @SuppressWarnings("unchecked")
        List<String> hrefs = (List<String>) ctx.page.locator("a[href*='view=modifyForm']")
            .evaluateAll("els => els.map(e => e.getAttribute('href'))");
        Set<Integer> ids = new LinkedHashSet<>();
        for (String href : hrefs) {
            Integer id = intParam(href, "id_form");
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** Titre enregistre du formulaire, relu sur son ecran de modification ({@code #title}). */
    public static String formTitle(FormsContext ctx, int formId) {
        MacroSupport.navigate(ctx, MacroSupport.FORMS + "ManageForms.jsp?view=modifyForm&id_form=" + formId);
        Locator title = ctx.page.locator("form#modify_form:has(input[name='id_form'][value='" + formId + "']) #title");
        Assertions.assertEquals(1, title.count(), "L'ecran de modification du formulaire " + formId + " doit etre affiche");
        return title.inputValue();
    }

    /** Navigue sur un href relatif de l'admin ({@code jsp/admin/...}) tel que la page le porte. */
    public static void follow(FormsContext ctx, String href) {
        MacroSupport.navigate(ctx, href.startsWith("/") ? href : "/" + href);
    }

    /** Ecran de gestion des usagers front-office MyLutece (module database). */
    public static final String MYLUTECE_DB = "/jsp/admin/plugins/mylutece/modules/database/";

    /**
     * Id MyLutece de l'usager front-office {@code login}, lu dans la liste des usagers apres recherche par login
     * (offcanvas « Rechercher » de l'ecran, lien « Supprimer l'utilisateur » de la ligne). -1 si absent.
     *
     * <p>L'absence est prouvee par l'ecran, pas par un lien introuvable (revue adversariale du 2026-10-30, constat 7) :
     * apres la recherche, le champ {@code search_login} garde le login cherche et la liste affiche son compteur
     * {@code .paginator-item-count} ; un compteur a 0 s'accompagne de l'etat vide {@code .empty} (releve du
     * 2026-11-04). Une page d'erreur ou de connexion fait donc echouer la lecture au lieu de passer pour « absent ».</p>
     */
    public static int frontUserId(FormsContext ctx, String login) {
        MacroSupport.navigate(ctx, MYLUTECE_DB + "ManageUsers.jsp?plugin_name=mylutece-database");
        // Le formulaire de recherche est dans l'offcanvas « Rechercher » (#offcanvasSearch).
        ctx.page.locator("a[href='#offcanvasSearch']").click();
        Locator search = ctx.page.locator("#offcanvasSearch input[name='search_login']");
        search.waitFor();
        search.fill(login);
        ctx.page.locator("#offcanvasSearch button[name='search_is_search']").click();
        ctx.page.waitForLoadState();
        Assertions.assertEquals(login, ctx.page.locator("#offcanvasSearch input[name='search_login']").inputValue(),
            "La liste des usagers doit etre filtree sur le login cherche (url " + ctx.page.url() + ")");
        Locator counter = ctx.page.locator(".paginator-item-count");
        Assertions.assertEquals(1, counter.count(), "La liste des usagers doit afficher son compteur (url " + ctx.page.url() + ")");
        String count = counter.innerText().trim();
        Assertions.assertTrue(count.matches("\\d+"), "Compteur d'usagers illisible : '" + count + "'");
        Object href = ctx.page.evaluate("login => { const a = [...document.querySelectorAll(\"a[href*='RemoveUser.jsp']\")]"
            + ".find(x => { const row = x.closest('tr, li, .card, .list-group-item'); return row && row.innerText.split(/\\s+/).includes(login); });"
            + " return a ? a.getAttribute('href') : null; }", login);
        if (href == null) {
            Assertions.assertTrue(!"0".equals(count) || ctx.page.locator(".empty").count() >= 1,
                "Liste vide sans etat vide affiche (url " + ctx.page.url() + ")");
            return -1;
        }
        Integer id = intParam((String) href, "mylutece_database_user_id");
        Assertions.assertNotNull(id, "Lien de suppression sans mylutece_database_user_id : " + href);
        return id;
    }

    /**
     * Nettoyage best-effort d'un {@code standalone()} : depublie le formulaire (un formulaire actif n'a pas le
     * meme message de confirmation de suppression) puis le supprime. N'ecrase jamais l'echec du test (§4-bis.8).
     */
    public static void cleanup(FormsContext ctx) {
        cleanupForms(ctx);
    }

    /**
     * {@link #cleanup(FormsContext)} puis suppression du workflow cree par le test (revue adversariale du 2026-10-30,
     * constat 5). Le workflow n'est supprime que si tous les formulaires l'ont ete : un formulaire restant garderait
     * un {@code id_workflow} orphelin. Ni desactivation ni nettoyage des ressources prealables : la suppression d'un
     * formulaire retire les ressources de workflow de ses reponses, et celle d'un workflow emporte les siennes.
     */
    public static void cleanup(FormsContext forms, WorkflowContext wf) {
        boolean formsRemoved = cleanupForms(forms);
        if (!formsRemoved || wf == null || wf.workflowId <= 0) {
            return;
        }
        try {
            // CreateWorkflow resout l'id par sous-chaine du nom dans la liste : ne supprimer que le workflow dont le
            // nom enregistre est exactement celui du contexte, jamais un reliquat d'un autre run.
            WorkflowSupport.navigate(wf, WorkflowSupport.WF + "ModifyWorkflow.jsp?id_workflow=" + wf.workflowId);
            Locator name = wf.page.locator("form:has(input[name='id_workflow'][value='" + wf.workflowId + "']) input#name");
            if (name.count() != 1 || !name.inputValue().equals(wf.workflowName)) {
                return;
            }
            RemoveWorkflowMacroTest.run(wf);
        } catch (Throwable ignored) {
            // best-effort : le nom du workflow porte le suffixe d'execution
        }
    }

    /**
     * Nettoyage des formulaires puis des categories du contexte ; vrai si plus aucun formulaire du contexte n'existe.
     * Les categories ne sont supprimees que si tous les formulaires l'ont ete : supprimer une categorie encore utilisee
     * est accepte et laisse {@code forms_form.id_category} orphelin (defaut B). Revue adversariale du 2026-10-30,
     * constat 6 : une suite qui echouait avant {@code RemoveFormCategory} laissait sa categorie.
     */
    private static boolean cleanupForms(FormsContext ctx) {
        if (ctx == null) {
            return true;
        }
        boolean formsRemoved = removeForms(ctx);
        if (formsRemoved) {
            while (!ctx.categoryIds.isEmpty()) {
                try {
                    RemoveFormCategoryMacroTest.run(ctx);
                } catch (Throwable ignored) {
                    break; // best-effort : le nom de la categorie porte le suffixe d'execution
                }
            }
        }
        return formsRemoved;
    }

    private static boolean removeForms(FormsContext ctx) {
        try {
            ensureAdminSession(ctx);
        } catch (Throwable ignored) {
            // best-effort : les etapes suivantes echoueront chacune en le disant
        }
        if (ctx.frontUserId > 0) {
            try {
                RemoveFrontUserBOMacroTest.run(ctx);
            } catch (Throwable ignored) {
                // best-effort : l'usager reste, son login porte le suffixe d'execution
            }
        }
        if (ctx.formId <= 0 && ctx.createdForms.isEmpty()) {
            return true;
        }
        boolean removed = ctx.formId <= 0;
        if (!removed) {
            try {
                UnpublishFormMacroTest.run(ctx);
            } catch (Throwable ignored) {
                // best-effort : la suppression est tentee quand meme
            }
            try {
                removeFormById(ctx, ctx.formId);
                removed = true;
                ctx.formId = -1;
                ctx.formTitle = null;
                ctx.steps.clear();
                ctx.questions.clear();
                ctx.groups.clear();
            } catch (Throwable ignored) {
                // best-effort : le formulaire reste, le run suivant utilise un autre suffixe
            }
        }
        // Copies et imports : meme chemin, chacun par son id.
        for (int extra : new ArrayList<>(ctx.createdForms.keySet())) {
            try {
                removeFormById(ctx, extra);
                ctx.createdForms.remove(extra);
            } catch (Throwable ignored) {
                // best-effort
            }
        }
        return removed && ctx.createdForms.isEmpty();
    }

    /**
     * Retablit la session admin si elle a ete perdue. {@code DoMyLuteceLogout.jsp} invalide toute la session HTTP,
     * admin comprise : apres la deconnexion d'un usager FO dans le meme navigateur, tout ecran BO redirige vers
     * {@code AdminMessage.jsp} (releve du 2026-11-12, suite FO dynamique dont le nettoyage tombait hors session).
     * Sans effet quand le menu d'administration s'affiche.
     */
    static void ensureAdminSession(FormsContext ctx) {
        MacroSupport.navigate(ctx, "/jsp/admin/AdminMenu.jsp");
        if (ctx.page.url().contains("AdminMenu.jsp")) {
            return;
        }
        Config config = ConfigProvider.getConfig();
        new LoginPage(ctx.page, ctx.baseUrl).navigate()
            .loginAs(config.getValue("test.admin.username", String.class), config.getValue("test.admin.password", String.class));
        ctx.page.waitForLoadState();
        MacroSupport.navigate(ctx, "/jsp/admin/AdminMenu.jsp");
        Assertions.assertTrue(ctx.page.url().contains("AdminMenu.jsp"),
            "La session admin doit etre retablie avant le nettoyage (url " + ctx.page.url() + ")");
    }

    /**
     * Supprime le formulaire {@code formId} par le chemin reel de la liste (lien {@code view=confirmRemoveForm} de sa
     * ligne, puis bouton OK du formulaire {@code action=removeForm&id_form=N} de l'AdminMessage, releve du 2026-11-12)
     * et prouve l'absence par l'id. Reserve au nettoyage : {@code RemoveForm} (brique suivie, non modifiee) prouve
     * l'absence par sous-chaine du titre et echoue a tort quand un autre formulaire du run contient ce titre (copie
     * « Copie de … », import), alors que la suppression a eu lieu (rejeu du 2026-11-12).
     */
    @Step("Nettoyage : supprimer le formulaire {formId}")
    static void removeFormById(FormsContext ctx, int formId) {
        if (!formIds(ctx).contains(formId)) {
            return;
        }
        String confirm = linkHref(ctx, "view=confirmRemoveForm", "id_form", formId);
        Assertions.assertNotNull(confirm, "Lien de suppression du formulaire " + formId + " introuvable dans la liste");
        follow(ctx, confirm);
        Locator ok = ctx.page.locator("form[action$='action=removeForm&id_form=" + formId + "'] button[type='submit']");
        Assertions.assertEquals(1, ok.count(), "Confirmation de suppression du formulaire " + formId + " attendue (url "
            + ctx.page.url() + ")");
        ok.click();
        ctx.page.waitForLoadState();
        Assertions.assertFalse(formIds(ctx).contains(formId), "Le formulaire " + formId + " ne doit plus etre liste");
    }
}
