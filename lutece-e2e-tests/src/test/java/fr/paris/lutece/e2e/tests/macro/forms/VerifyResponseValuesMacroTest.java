package fr.paris.lutece.e2e.tests.macro.forms;

import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.FormsNonRegSupport;
import fr.paris.lutece.e2e.tests.macro.MacroSupport;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FieldValueDataSet;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.ResponseCheckDataSet;
import fr.paris.lutece.e2e.tests.macro.data.ResponseValuesDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import fr.paris.lutece.e2e.tests.macro.data.StepTargetDataSet;
import fr.paris.lutece.e2e.tests.macro.data.TransitionTargetsDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Brique de verification : les dernieres reponses enregistrees portent les valeurs saisies, relues dans leur detail BO.
 *
 * <p>Se place derriere {@code VerifyResponseRecorded}, qui ne prouve que le nombre de reponses nouvelles. Chemin
 * reel : le lien de la ligne de la multivue ({@code data-url=...view=view_form_response_details&id_form_response=N}).
 * Le detail ({@code #form-response-details}) liste une ligne {@code li[displaycontrol=<question>_<iteration>]} par
 * reponse : titre de la question en {@code > .row > .col-sm-3}, valeur en {@code > .row > .col-sm-9} (releve du 2026-10-30). Depuis
 * forms 4.1.0 (LUT-33387/LUT-33153) l'attribut est {@code data-display-control} (releve du 2026-11-17) : les deux formes
 * sont acceptees. Effet
 * prouve : chaque table attendue correspond a exactement une des N dernieres reponses, valeur par valeur.</p>
 *
 * <p>Les questions de {@code data.unanswered()} doivent etre sans valeur dans la reponse retenue (ligne absente ou
 * vide) : preuve qu'une question masquee n'a pas enregistre une saisie anterieure (2026-11-12).</p>
 *
 * <p>Lit : {@code ctx.formId}, les N derniers ids de {@code ctx.responseIds}. Ecrit : rien.</p>
 */
@Epic("Forms")
@Feature("Reponses")
@Story("Verifier les valeurs d'une reponse")
@Tag("macro")
@Tag("forms")
@Tag("brick")
@Tag("nonreg")
public class VerifyResponseValuesMacroTest extends MacroTest {

    private static final String ROWS_JS = "rows => rows.map(r => ({"
        + " title: ((r.querySelector(':scope > .row > .col-sm-3') || {}).innerText || '').trim(),"
        + " value: ((r.querySelector(':scope > .row > .col-sm-9') || {}).innerText || '').trim() }))";

    @Step("Verifier les valeurs des dernieres reponses dans leur detail BO")
    public static void run(FormsContext ctx, ResponseValuesDataSet data) {
        Assertions.assertTrue(ctx.formId > 0, "Un formulaire doit exister (ctx.formId)");
        int n = data.count();
        Assertions.assertTrue(ctx.responseIds.size() >= n,
            n + " reponse(s) enregistree(s) attendue(s) (VerifyResponseRecorded) : " + ctx.responseIds);
        List<Integer> ids = ctx.responseIds.subList(ctx.responseIds.size() - n, ctx.responseIds.size());

        FormsNonRegSupport.multiviewResponseIds(ctx);
        Map<Integer, String> detailUrls = new LinkedHashMap<>();
        for (int id : ids) {
            List<?> urls = (List<?>) ctx.page.locator("[data-url*='view_form_response_details'][data-url*='id_form_response=" + id + "']")
                .evaluateAll("els => [...new Set(els.map(e => e.getAttribute('data-url')))]");
            String url = urls.stream().map(String::valueOf)
                .filter(u -> Integer.valueOf(id).equals(FormsNonRegSupport.intParam(u, "id_form_response")))
                .findFirst().orElse(null);
            Assertions.assertNotNull(url, "La multivue doit proposer le lien de detail de la reponse " + id + " : " + urls);
            detailUrls.put(id, url);
        }

        Map<Integer, Map<String, String>> actual = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : detailUrls.entrySet()) {
            actual.put(entry.getKey(), readDetail(ctx, entry.getKey(), entry.getValue()));
        }

        List<Map<String, String>> unmatched = new ArrayList<>();
        Map<Integer, Map<String, String>> remaining = new LinkedHashMap<>(actual);
        for (Map<String, String> expected : data.expected()) {
            Integer match = remaining.entrySet().stream()
                .filter(e -> expected.entrySet().stream().allMatch(kv -> kv.getValue().equals(e.getValue().get(kv.getKey())))
                    && data.unanswered().stream().allMatch(t -> e.getValue().getOrDefault(t, "").isBlank()))
                .map(Map.Entry::getKey).findFirst().orElse(null);
            if (match == null) {
                unmatched.add(expected);
            } else {
                remaining.remove(match);
            }
        }
        Assertions.assertTrue(unmatched.isEmpty(), "Valeurs attendues sans reponse correspondante : " + unmatched
            + (data.unanswered().isEmpty() ? "" : " (sans valeur pour " + data.unanswered() + ")")
            + " ; reponses relues sans correspondance : " + remaining);
    }

    private static Map<String, String> readDetail(FormsContext ctx, int id, String url) {
        MacroSupport.navigate(ctx, "/" + url.replaceFirst("^/", ""));
        Assertions.assertEquals(1, ctx.page.locator("#form-response-details #steps-content").count(),
            "Le detail de la reponse " + id + " doit etre affiche (url " + ctx.page.url() + ")");
        String breadcrumb = ctx.page.locator(".breadcrumb").innerText().replaceAll("\\s+", " ");
        Assertions.assertTrue(Pattern.compile("n° " + id + "(?!\\d)").matcher(breadcrumb).find(),
            "Le fil d'Ariane doit nommer la reponse " + id + " : " + breadcrumb);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) ctx.page
            .locator("#steps-content li:is([displaycontrol], [data-display-control]):has(> .row > .col-sm-3)").evaluateAll(ROWS_JS);
        Map<String, String> values = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String title = (String) row.get("title");
            Assertions.assertNull(values.put(title, (String) row.get("value")),
                "Question '" + title + "' affichee deux fois dans le detail de la reponse " + id + " : " + rows);
        }
        return values;
    }

    @Test
    @DisplayName("Soumission FO sur deux etapes, valeurs relues dans le detail BO")
    void standaloneVerifyResponseValues() {
        FormsContext ctx = newLoggedInContext();
        try {
            CreateFormMacroTest.run(ctx, FormDataSet.defaults());
            CreateStepMacroTest.run(ctx, StepDataSet.of("Etape A"));
            CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape B"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Nom"));
            AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Ville").onStep(1));
            VerifyTransitionTargetsMacroTest.run(ctx, TransitionTargetsDataSet.of(0, 1));
            PublishFormMacroTest.run(ctx, PublishDataSet.defaults());

            OpenFormFOMacroTest.run(ctx);
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Nom", "Dupont"));
            GoToNextStepFOMacroTest.run(ctx, StepTargetDataSet.of(1));
            FillFieldFOMacroTest.run(ctx, FieldValueDataSet.text("Ville", "Paris"));
            ViewSummaryFOMacroTest.run(ctx);
            ValidateSummaryFOMacroTest.run(ctx);
            VerifyResponseRecordedMacroTest.run(ctx, ResponseCheckDataSet.defaults());

            run(ctx, ResponseValuesDataSet.of("Nom", "Dupont", "Ville", "Paris"));
        } finally {
            FormsNonRegSupport.cleanup(ctx);
        }
    }
}
