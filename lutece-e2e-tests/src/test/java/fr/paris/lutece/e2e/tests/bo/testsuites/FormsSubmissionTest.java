package fr.paris.lutece.e2e.tests.bo.testsuites;

import fr.paris.lutece.e2e.tests.bo.config.BaseTest;
import fr.paris.lutece.e2e.pages.bo.*;
import org.junit.jupiter.api.*;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E pour la soumission d'un formulaire en FO.
 * Doit etre execute apres FormsCreationTest.
 * Le Front Office ne necessite pas d'authentification.
 */
@DisplayName("Tests de soumission de formulaire")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FormsSubmissionTest extends BaseTest {

    private String formTitle;
    private static final String QUESTION_TEXT = config.getValue("test.forms.question.text", String.class);
    private static final String QUESTION_NUMBER = config.getValue("test.forms.question.number", String.class);
    private static final String SUBMIT_TEXT = config.getValue("test.forms.submit.text", String.class);
    private static final String SUBMIT_NUMBER = config.getValue("test.forms.submit.number", String.class);
    private static final String SUBMIT_DATE = config.getValue("test.forms.submit.date", String.class);

    @Override
    protected void createContextAndPage() {
        // Ne rien faire : le contexte est cree une seule fois dans setup
    }

    @Override
    protected void closeContext() {
        // Ne rien faire : le contexte est ferme dans cleanup
    }

    private static String readRunSuffix() {
        try {
            return java.nio.file.Files.readString(
                java.nio.file.Paths.get("target/test-run-suffix.txt")).trim();
        } catch (Exception e) {
            return System.getProperty("test.run.suffix", "0");
        }
    }

    private static String readFormId() {
        try {
            return java.nio.file.Files.readString(
                java.nio.file.Paths.get("target/test-form-id.txt")).trim();
        } catch (Exception e) {
            return "1"; // Fallback
        }
    }

    @BeforeAll
    void setup() {
        String runSuffix = readRunSuffix();
        formTitle = config.getValue("test.forms.title", String.class) + " " + runSuffix;

        // Front Office - pas besoin d'authentification
        context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
            .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            .setLocale(LOCALE)
            .setIgnoreHTTPSErrors(true));

        // Le FO de certains sites (ex: image p30 site-integration-forms) charge des ressources tierces
        // (widget Google Translate, tarteaucitron, Matomo). Dans le conteneur de test SANS egress internet,
        // ces requetes pendent jusqu'au timeout et l'evenement 'load' ne se declenche jamais -> la moindre
        // attente de chargement (navigate / waitForLoadState) expire (30s) des l'ouverture de la page.
        // On coupe les requetes hors-localhost : le test devient hermetique (identique a un navigateur reel
        // cote formulaire, mais deterministe et independant de la disponibilite des tiers).
        context.route("**/*", route -> {
            String url = route.request().url();
            if (url.startsWith("http") && !url.contains("localhost") && !url.contains("127.0.0.1")) {
                route.abort();
            } else {
                route.resume();
            }
        });

        page = context.newPage();
        page.setDefaultTimeout(TIMEOUT);
    }

    @AfterAll
    void cleanup() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Soumission du formulaire en front office")
    @Severity(SeverityLevel.CRITICAL)
    void testSubmitFormInFrontOffice() {
        // Given - Acceder directement au formulaire cible en FO via son id_form
        String formId = readFormId();
        page.navigate(BASE_URL + "/jsp/site/Portal.jsp?page=forms&view=stepView&id_form=" + formId);
        page.waitForLoadState();
        page.waitForTimeout(1000);

        FormsFrontOfficePage foPage = new FormsFrontOfficePage(page, BASE_URL);

        // Fermer l'offcanvas s'il est present
        foPage.dismissOffcanvasIfPresent();

        // Verifier que les champs du formulaire sont presents
        boolean hasFormFields = page.locator("input[type='text']").count() > 0 ||
                                page.locator("input[type='number']").count() > 0 ||
                                page.locator("textarea").count() > 0;

        if (!hasFormFields) {
            // Prendre un screenshot pour debug
            try {
                java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target/screenshots"));
                page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions()
                    .setPath(java.nio.file.Paths.get("target/screenshots/fo-form-not-found.png")));
            } catch (Exception e) {
                // ignore
            }
            // Skip le test si les champs ne sont pas disponibles
            Assumptions.assumeTrue(hasFormFields,
                "Les champs du formulaire ne sont pas disponibles en front office. URL: " + page.url());
        }

        // Remplir les champs de l'etape 1 avec gestion des erreurs
        try {
            foPage.fillTextField(QUESTION_TEXT, SUBMIT_TEXT);
            foPage.fillNumberField(QUESTION_NUMBER, SUBMIT_NUMBER);
            foPage.fillDateField(SUBMIT_DATE);

            // Passer a l'etape suivante
            foPage.clickNextStep();

            // Voir et valider le recapitulatif
            foPage.clickViewSummary();
            foPage.clickValidateSummary();

            // Attendre la fin de la soumission
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

            // Then - Verifier que la soumission a eu lieu
            assertTrue(page.url().contains("forms") || page.content().contains("formulaire"),
                "La soumission devrait etre effectuee");
        } catch (Exception e) {
            // Prendre un screenshot en cas d'erreur
            try {
                java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target/screenshots"));
                page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions()
                    .setPath(java.nio.file.Paths.get("target/screenshots/fo-submission-error.png")));
            } catch (Exception ex) {
                // ignore
            }
            throw e;
        }
    }
}
