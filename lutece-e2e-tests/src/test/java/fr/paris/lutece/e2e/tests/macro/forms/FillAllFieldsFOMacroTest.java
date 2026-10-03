package fr.paris.lutece.e2e.tests.macro.forms;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import fr.paris.lutece.e2e.tests.macro.FormsContext;
import fr.paris.lutece.e2e.tests.macro.MacroTest;
import fr.paris.lutece.e2e.tests.macro.data.FormDataSet;
import fr.paris.lutece.e2e.tests.macro.data.PublishDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionDataSet;
import fr.paris.lutece.e2e.tests.macro.data.QuestionType;
import fr.paris.lutece.e2e.tests.macro.data.StepDataSet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Brique macro : renseigner toutes les reponses de l'etape front-office courante.
 *
 * <p>Lit : la page front-office courante. Ecrit : rien dans le contexte.</p>
 *
 * <p>Complete {@link FillFieldFOMacroTest}, qui vise une question par son libelle. Ici aucune
 * question n'est designee : tous les champs de l'etape sont renseignes selon leur nature, ce qui
 * couvre les types dont le libelle ne suffit pas a atteindre le controle — une liste triable, un
 * groupe de boutons radio, un creneau. Le formulaire est ainsi soumis complet, et non reduit aux
 * quelques champs qu'un scenario aurait nommes.</p>
 *
 * <p>Les champs de televersement sont laisses de cote : ils supposent un fichier sur le poste
 * d'execution, ce qui ferait dependre le scenario de son environnement.</p>
  *
 * <p><b>Completer, jamais ecraser.</b> Les champs deja renseignes sont laisses tels quels. Ce
 * n'est pas une commodite : une valeur posee avant cet appel l'a ete deliberement, et certaines
 * commandent la suite du parcours — reprendre le premier choix d'une question d'aiguillage
 * enverrait le formulaire sur une autre branche que celle que le scenario veut eprouver.</p>
 */
@Epic("Forms")
@Feature("Front office")
@Story("Renseigner toutes les reponses d'une etape")
@Tag("macro")
@Tag("forms")
@Tag("brick")
public class FillAllFieldsFOMacroTest extends MacroTest {

    private static final String TEXTE = "Reponse de test";
    private static final String NOMBRE = "42";
    private static final String DATE = "2026-02-04";
    private static final String TELEPHONE = "0123456789";

    /** Delai d'ouverture du calendrier d'un champ date, en millisecondes. */
    private static final double CALENDRIER_TIMEOUT_MS = 5000;

    /** Rang du jour choisi dans le mois affiche : un jour de plein milieu, jamais hors bornes. */
    private static final int JOUR_CIBLE = 14;

    @Step("Renseigner toutes les reponses de l'etape")
    public static int run(FormsContext ctx) {
        Page page = ctx.page;
        page.waitForLoadState();

        int renseignes = 0;
        renseignes += choisirDates(page);
        renseignes += remplirSaisies(page);
        renseignes += remplirListes(page);
        renseignes += cocherChoix(page);
        return renseignes;
    }

    /**
     * Renseigne les champs de date en passant par leur calendrier.
     *
     * <p>Une question de type date n'est pas une simple saisie : le champ visible ne porte aucun
     * attribut {@code name} et sert uniquement d'affichage, la valeur reellement soumise etant
     * portee par un champ cache que seul le calendrier alimente. Y ecrire directement laisse donc
     * la reponse vide, sans aucun signe visible — d'ou la selection d'un jour dans le calendrier.</p>
     *
     * @param page page front-office courante
     * @return le nombre de dates choisies
     */
    private static int choisirDates(Page page) {
        Locator champs = page.locator("form input.lutece-datepicker:visible");
        int total = champs.count();
        int faits = 0;
        for (int i = 0; i < total; i++) {
            Locator champ = champs.nth(i);
            if (!champ.inputValue().isBlank()) {
                continue;
            }
            champ.click();
            Locator jours = page.locator(
                ".datepicker-dropdown .datepicker-cell.day:not(.prev):not(.next):not(.disabled)");
            try {
                jours.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(CALENDRIER_TIMEOUT_MS));
            } catch (RuntimeException calendrierAbsent) {
                continue;
            }
            int rang = Math.min(JOUR_CIBLE, jours.count() - 1);
            jours.nth(rang).click();
            if (!champ.inputValue().isBlank()) {
                faits++;
            }
        }
        return faits;
    }

    /**
     * Renseigne les champs de saisie libre visibles de l'etape.
     *
     * @param page page front-office courante
     * @return le nombre de champs renseignes
     */
    private static int remplirSaisies(Page page) {
        Locator champs = page.locator(
            "form input[type='text']:visible, form input[type='number']:visible, "
            + "form input[type='tel']:visible, form input[type='date']:visible, "
            + "form input[type='email']:visible, form textarea:visible");
        int total = champs.count();
        int faits = 0;
        for (int i = 0; i < total; i++) {
            Locator champ = champs.nth(i);
            String nom = champ.getAttribute("name");
            if (nom == null || nom.isBlank()) {
                // Les composants riches (date, creneau) exposent un champ d'affichage sans name,
                // double d'un champ cache porteur de la valeur : y ecrire n'a aucun effet et fait
                // echouer la soumission.
                continue;
            }
            if (!champ.inputValue().isBlank()) {
                continue;
            }
            String type = champ.getAttribute("type");
            champ.fill(valeurPour(type));
            faits++;
        }
        return faits;
    }

    /**
     * Selectionne une option dans chaque liste deroulante visible.
     *
     * @param page page front-office courante
     * @return le nombre de listes renseignees
     */
    private static int remplirListes(Page page) {
        Locator listes = page.locator("form select:visible");
        int total = listes.count();
        int faits = 0;
        for (int i = 0; i < total; i++) {
            Locator liste = listes.nth(i);
            String nom = liste.getAttribute("name");
            if (nom == null || nom.isBlank()) {
                continue;
            }
            if (!liste.inputValue().isBlank()) {
                continue;
            }
            Locator options = liste.locator("option[value]:not([value=''])");
            if (options.count() == 0) {
                continue;
            }
            liste.selectOption(options.first().getAttribute("value"));
            faits++;
        }
        return faits;
    }

    /**
     * Coche le premier choix de chaque groupe de boutons radio et chaque case visible.
     *
     * @param page page front-office courante
     * @return le nombre de choix effectues
     */
    private static int cocherChoix(Page page) {
        int faits = 0;
        java.util.Set<String> groupesVus = new java.util.HashSet<>();
        Locator radios = page.locator("form input[type='radio']:visible");
        for (int i = 0; i < radios.count(); i++) {
            Locator radio = radios.nth(i);
            String nom = radio.getAttribute("name");
            if (nom == null || nom.isBlank() || !groupesVus.add(nom)) {
                continue;
            }
            // Un groupe deja renseigne l'a ete deliberement : le reprendre ecraserait un choix dont
            // depend parfois la suite du parcours, et le formulaire partirait sur une autre branche.
            if (page.locator("form input[type='radio'][name='" + nom + "']:checked").count() > 0) {
                continue;
            }
            radio.check();
            faits++;
        }
        Locator cases = page.locator("form input[type='checkbox']:visible");
        for (int i = 0; i < cases.count(); i++) {
            Locator caseACocher = cases.nth(i);
            String nom = caseACocher.getAttribute("name");
            if (nom == null || nom.isBlank() || caseACocher.isChecked()) {
                continue;
            }
            caseACocher.check();
            faits++;
        }
        return faits;
    }

    /**
     * Valeur a saisir pour un type de champ donne.
     *
     * @param type attribut {@code type} du champ
     * @return une valeur acceptable pour ce type
     */
    private static String valeurPour(String type) {
        if (type == null) {
            return TEXTE;
        }
        return switch (type) {
            case "number" -> NOMBRE;
            case "date" -> DATE;
            case "tel" -> TELEPHONE;
            case "email" -> "test@example.com";
            default -> TEXTE;
        };
    }

    @Test
    @DisplayName("Renseigner toutes les reponses d'une etape (auto-provisionnement + ouverture FO)")
    void standalone() {
        FormsContext ctx = newLoggedInContext();
        CreateFormMacroTest.run(ctx, FormDataSet.defaults());
        CreateStepMacroTest.run(ctx, StepDataSet.finalStep("Etape unique"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.TEXT, "Champ texte"));
        AddQuestionMacroTest.run(ctx, QuestionDataSet.of(QuestionType.NUMBER, "Champ nombre"));
        PublishFormMacroTest.run(ctx, PublishDataSet.defaults());
        OpenFormFOMacroTest.run(ctx);
        int renseignes = run(ctx);
        Assertions.assertTrue(renseignes > 0,
            "Au moins une reponse aurait du etre renseignee sur l'etape front-office");
    }
}
